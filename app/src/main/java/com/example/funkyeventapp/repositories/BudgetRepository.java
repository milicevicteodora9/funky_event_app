package com.example.funkyeventapp.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.funkyeventapp.models.Budget;
import com.example.funkyeventapp.models.BudgetCategory;
import com.example.funkyeventapp.models.BudgetItem;
import com.example.funkyeventapp.models.BudgetItemSource;
import com.example.funkyeventapp.models.BudgetType;
import com.example.funkyeventapp.models.CashboxTransaction;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Read access for an event budget and its items/categories. */
public final class BudgetRepository {
    public interface Callback<T> {
        void onSuccess(T value);
        void onError(@NonNull Exception error);
    }

    public static final class BudgetData {
        private final Budget budget;
        private final List<BudgetItem> items;
        private final List<BudgetCategory> categories;

        BudgetData(Budget budget, List<BudgetItem> items, List<BudgetCategory> categories) {
            this.budget = budget;
            this.items = items;
            this.categories = categories;
        }

        public Budget getBudget() { return budget; }
        public List<BudgetItem> getItems() { return items; }
        public List<BudgetCategory> getCategories() { return categories; }
    }

    private static final BudgetRepository INSTANCE = new BudgetRepository();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    private BudgetRepository() { }

    public static BudgetRepository getInstance() { return INSTANCE; }

    public Task<Void> createBudgetCategory(@NonNull BudgetCategory category) {
        DocumentReference document = firestore.collection("budgetCategories").document();
        Map<String, Object> data = new HashMap<>();
        data.put("name", category.getName());
        return document.set(data)
                .addOnSuccessListener(unused -> category.setId(document.getId()));
    }

    public Task<Void> updateBudgetCategory(@NonNull BudgetCategory category) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", category.getName());
        return firestore.collection("budgetCategories").document(category.getId()).update(data);
    }

    public Task<Void> deleteBudgetCategory(@NonNull String categoryId) {
        return firestore.collection("budgetCategories").document(categoryId).delete();
    }

    public Task<Void> createBudgetItem(@NonNull BudgetItem item) {
        if (item.getEventId() == null || item.getEventId().trim().isEmpty()) {
            throw new IllegalArgumentException("Event ID is required");
        }
        DocumentReference budgetDocument = firestore.collection("budgets")
                .document(item.getEventId());
        DocumentReference itemDocument = budgetDocument.collection("items").document();
        return firestore.<Void>runTransaction(transaction -> {
                    DocumentSnapshot existingBudget = transaction.get(budgetDocument);
                    if (!existingBudget.exists()) {
                        Map<String, Object> budgetData = new HashMap<>();
                        budgetData.put("eventId", item.getEventId());
                        budgetData.put("includeVat", false);
                        budgetData.put("discountPercentage", 0.0);
                        transaction.set(budgetDocument, budgetData);
                    }
                    transaction.set(itemDocument, itemData(item));
                    return null;
                })
                .addOnSuccessListener(unused -> item.setId(itemDocument.getId()));
    }

    Task<Void> createCashboxExpenseWithActual(@NonNull String userId,
                                               @NonNull CashboxTransaction expense,
                                               @NonNull Map<String, Object> cashboxTransactionData) {
        if (expense.getId() == null || expense.getEventId() == null
                || expense.getAmountInEur() == null) {
            throw new IllegalArgumentException("Cashbox transaction ID, event ID and amount are required");
        }

        String eventId = expense.getEventId();
        String transactionId = expense.getId();
        DocumentReference cashboxDocument = firestore.collection("cashboxes").document(userId);
        DocumentReference cashboxTransactionDocument = cashboxDocument
                .collection("transactions").document(transactionId);
        DocumentReference budgetDocument = firestore.collection("budgets").document(eventId);
        DocumentReference actualItemDocument = budgetDocument.collection("items")
                .document("cashbox_" + transactionId);
        BudgetItem actualItem = new BudgetItem(actualItemDocument.getId(), eventId,
                BudgetType.ACTUAL, expense.getCategoryId(), expense.getDescription(),
                BigDecimal.ONE, BigDecimal.ONE, expense.getAmountInEur(), "",
                BudgetItemSource.CASHBOX, transactionId, null);
        Map<String, Object> actualItemData = itemData(actualItem);
        actualItemData.put("sourceTransactionId", transactionId);
        actualItemData.put("sourceBudgetItemId", null);

        return firestore.runTransaction(transaction -> {
            DocumentSnapshot cashboxSnapshot = transaction.get(cashboxDocument);
            DocumentSnapshot budgetSnapshot = transaction.get(budgetDocument);
            DocumentSnapshot actualItemSnapshot = transaction.get(actualItemDocument);
            if (!cashboxSnapshot.exists() || !userId.equals(cashboxSnapshot.getString("userId"))) {
                throw new IllegalStateException("Cashbox must belong to the authenticated user");
            }
            if (actualItemSnapshot.exists()) {
                String sourceType = actualItemSnapshot.getString("sourceType");
                String existingSourceId = actualItemSnapshot.getString("sourceTransactionId");
                if (!BudgetItemSource.CASHBOX.name().equals(sourceType)
                        || !transactionId.equals(existingSourceId)) {
                    throw new IllegalStateException("Budget item ID collision");
                }
            }
            if (!budgetSnapshot.exists()) {
                Map<String, Object> budgetData = new HashMap<>();
                budgetData.put("eventId", eventId);
                budgetData.put("includeVat", false);
                budgetData.put("discountPercentage", 0.0);
                transaction.set(budgetDocument, budgetData);
            }
            transaction.set(cashboxTransactionDocument, cashboxTransactionData);
            if (!actualItemSnapshot.exists()) {
                transaction.set(actualItemDocument, actualItemData);
            }
            return null;
        });
    }

    Task<Void> updateCashboxExpenseWithActual(@NonNull String userId,
                                               @Nullable String previousEventId,
                                               @NonNull CashboxTransaction expense,
                                               @NonNull Map<String, Object> cashboxTransactionData) {
        if (expense.getId() == null || expense.getAmountInEur() == null) {
            throw new IllegalArgumentException("Cashbox transaction ID and amount are required");
        }

        String transactionId = expense.getId();
        String nextEventId = emptyToNull(expense.getEventId());
        String oldEventId = emptyToNull(previousEventId);
        DocumentReference cashboxDocument = firestore.collection("cashboxes").document(userId);
        DocumentReference cashboxTransactionDocument = cashboxDocument
                .collection("transactions").document(transactionId);
        DocumentReference nextBudgetDocument = nextEventId == null ? null
                : firestore.collection("budgets").document(nextEventId);
        DocumentReference nextActualDocument = cashboxActualDocument(nextEventId, transactionId);
        Map<String, Object> nextActualData = nextEventId == null ? null
                : cashboxActualData(expense, nextEventId);
        Task<List<DocumentReference>> oldMatchesTask =
                findCashboxActualDocuments(oldEventId, transactionId);
        Task<List<DocumentReference>> nextMatchesTask = oldEventId != null
                && oldEventId.equals(nextEventId) ? oldMatchesTask
                : findCashboxActualDocuments(nextEventId, transactionId);

        return Tasks.whenAllSuccess(oldMatchesTask, nextMatchesTask)
                .continueWithTask(result -> {
                    @SuppressWarnings("unchecked")
                    List<DocumentReference> oldActualDocuments = withCanonical(
                            (List<DocumentReference>) result.getResult().get(0),
                            cashboxActualDocument(oldEventId, transactionId));
                    @SuppressWarnings("unchecked")
                    List<DocumentReference> nextActualDocuments = withCanonical(
                            (List<DocumentReference>) result.getResult().get(1), nextActualDocument);
                    Map<String, DocumentReference> candidates = new HashMap<>();
                    for (DocumentReference document : oldActualDocuments) {
                        candidates.put(document.getPath(), document);
                    }
                    for (DocumentReference document : nextActualDocuments) {
                        candidates.put(document.getPath(), document);
                    }
                    boolean eventChanged = oldEventId == null
                            ? nextEventId != null : !oldEventId.equals(nextEventId);

                    return firestore.runTransaction(transaction -> {
                        DocumentSnapshot cashboxSnapshot = transaction.get(cashboxDocument);
                        DocumentSnapshot expenseSnapshot = transaction.get(cashboxTransactionDocument);
                        DocumentSnapshot nextBudgetSnapshot = nextBudgetDocument == null ? null
                                : transaction.get(nextBudgetDocument);
                        Map<String, DocumentSnapshot> actualSnapshots = new HashMap<>();
                        for (DocumentReference document : candidates.values()) {
                            actualSnapshots.put(document.getPath(), transaction.get(document));
                        }

                        validateCashboxOwner(cashboxSnapshot, userId);
                        if (!expenseSnapshot.exists()) {
                            throw new IllegalStateException("Cashbox expense does not exist");
                        }
                        for (DocumentSnapshot actualSnapshot : actualSnapshots.values()) {
                            validateCashboxActual(actualSnapshot, transactionId);
                        }

                        transaction.set(cashboxTransactionDocument, cashboxTransactionData);
                        if (eventChanged) {
                            for (DocumentReference document : oldActualDocuments) {
                                if (actualSnapshots.get(document.getPath()).exists()) {
                                    transaction.delete(document);
                                }
                            }
                        }
                        if (nextActualDocument != null) {
                            if (!nextBudgetSnapshot.exists()) {
                                Map<String, Object> budgetData = new HashMap<>();
                                budgetData.put("eventId", nextEventId);
                                budgetData.put("includeVat", false);
                                budgetData.put("discountPercentage", 0.0);
                                transaction.set(nextBudgetDocument, budgetData);
                            }
                            for (DocumentReference document : nextActualDocuments) {
                                if (!document.getPath().equals(nextActualDocument.getPath())
                                        && actualSnapshots.get(document.getPath()).exists()) {
                                    transaction.delete(document);
                                }
                            }
                            transaction.set(nextActualDocument, nextActualData);
                        }
                        return null;
                    });
                });
    }

    Task<Void> deleteCashboxExpenseWithActual(@NonNull String userId,
                                               @NonNull CashboxTransaction expense) {
        if (expense.getId() == null || expense.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Cashbox transaction ID is required");
        }
        String transactionId = expense.getId();
        String eventId = emptyToNull(expense.getEventId());
        DocumentReference cashboxDocument = firestore.collection("cashboxes").document(userId);
        DocumentReference cashboxTransactionDocument = cashboxDocument
                .collection("transactions").document(transactionId);

        return findCashboxActualDocuments(eventId, transactionId).continueWithTask(result -> {
            List<DocumentReference> actualDocuments = withCanonical(result.getResult(),
                    cashboxActualDocument(eventId, transactionId));
            return firestore.runTransaction(transaction -> {
                DocumentSnapshot cashboxSnapshot = transaction.get(cashboxDocument);
                DocumentSnapshot expenseSnapshot = transaction.get(cashboxTransactionDocument);
                List<DocumentSnapshot> actualSnapshots = new ArrayList<>();
                for (DocumentReference document : actualDocuments) {
                    actualSnapshots.add(transaction.get(document));
                }
                validateCashboxOwner(cashboxSnapshot, userId);
                if (!expenseSnapshot.exists()) {
                    throw new IllegalStateException("Cashbox expense does not exist");
                }
                for (DocumentSnapshot actualSnapshot : actualSnapshots) {
                    validateCashboxActual(actualSnapshot, transactionId);
                }
                transaction.delete(cashboxTransactionDocument);
                for (int index = 0; index < actualDocuments.size(); index++) {
                    if (actualSnapshots.get(index).exists()) {
                        transaction.delete(actualDocuments.get(index));
                    }
                }
                return null;
            });
        });
    }

    public void getAllBudgetCategories(@NonNull Callback<List<BudgetCategory>> callback) {
        firestore.collection("budgetCategories").get()
                .addOnSuccessListener(snapshot -> {
                    try {
                        List<BudgetCategory> categories = new ArrayList<>();
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            categories.add(mapCategory(document));
                        }
                        callback.onSuccess(categories);
                    } catch (IllegalArgumentException | IllegalStateException error) {
                        callback.onError(error);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    private Task<List<DocumentReference>> findCashboxActualDocuments(
            @Nullable String eventId, @NonNull String transactionId) {
        if (eventId == null) return Tasks.forResult(new ArrayList<>());
        return firestore.collection("budgets").document(eventId).collection("items")
                .whereEqualTo("sourceTransactionId", transactionId).get()
                .continueWith(result -> {
                    List<DocumentReference> matches = new ArrayList<>();
                    for (DocumentSnapshot document : result.getResult().getDocuments()) {
                        if (BudgetItemSource.CASHBOX.name().equals(document.getString("sourceType"))) {
                            matches.add(document.getReference());
                        }
                    }
                    return matches;
                });
    }

    private List<DocumentReference> withCanonical(List<DocumentReference> matches,
                                                   @Nullable DocumentReference canonical) {
        List<DocumentReference> result = new ArrayList<>(matches);
        if (canonical == null) return result;
        for (DocumentReference document : result) {
            if (document.getPath().equals(canonical.getPath())) return result;
        }
        result.add(canonical);
        return result;
    }

    private DocumentReference cashboxActualDocument(@Nullable String eventId,
                                                     @NonNull String transactionId) {
        if (eventId == null) return null;
        return firestore.collection("budgets").document(eventId).collection("items")
                .document("cashbox_" + transactionId);
    }

    private Map<String, Object> cashboxActualData(@NonNull CashboxTransaction expense,
                                                   @NonNull String eventId) {
        String transactionId = expense.getId();
        BudgetItem actualItem = new BudgetItem("cashbox_" + transactionId, eventId,
                BudgetType.ACTUAL, expense.getCategoryId(), expense.getDescription(),
                BigDecimal.ONE, BigDecimal.ONE, expense.getAmountInEur(), "",
                BudgetItemSource.CASHBOX, transactionId, null);
        Map<String, Object> data = itemData(actualItem);
        data.put("sourceTransactionId", transactionId);
        data.put("sourceBudgetItemId", null);
        return data;
    }

    private void validateCashboxOwner(DocumentSnapshot cashboxSnapshot, String userId) {
        if (!cashboxSnapshot.exists() || !userId.equals(cashboxSnapshot.getString("userId"))) {
            throw new IllegalStateException("Cashbox must belong to the authenticated user");
        }
    }

    private void validateCashboxActual(@Nullable DocumentSnapshot actualSnapshot,
                                       @NonNull String transactionId) {
        if (actualSnapshot == null || !actualSnapshot.exists()) return;
        if (!BudgetItemSource.CASHBOX.name().equals(actualSnapshot.getString("sourceType"))
                || !transactionId.equals(actualSnapshot.getString("sourceTransactionId"))) {
            throw new IllegalStateException("Budget item ID collision");
        }
    }

    private String emptyToNull(@Nullable String value) {
        return value == null || value.trim().isEmpty() ? null : value;
    }

    public Task<Void> updateBudgetItem(@NonNull String eventId, @NonNull BudgetItem item) {
        if (item.getId() == null || item.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Budget item ID is required");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("categoryId", item.getCategoryId());
        data.put("description", item.getDescription());
        data.put("quantity", item.getQuantity().doubleValue());
        data.put("days", item.getDays().doubleValue());
        data.put("dailyRate", item.getDailyRate().doubleValue());
        data.put("notes", item.getNotes());
        return firestore.collection("budgets").document(eventId)
                .collection("items").document(item.getId()).update(data);
    }

    public Task<Void> deleteBudgetItem(@NonNull String eventId, @NonNull String itemId) {
        return firestore.collection("budgets").document(eventId)
                .collection("items").document(itemId).delete();
    }

    public Task<Integer> copyExternalItemsToInternal(@NonNull String eventId) {
        return copyItems(eventId, BudgetType.EXTERNAL, BudgetType.INTERNAL, null);
    }

    public Task<Integer> copyInternalItemsToActual(@NonNull String eventId) {
        return copyItems(eventId, BudgetType.INTERNAL, BudgetType.ACTUAL, null);
    }

    public Task<Integer> copyBudgetItem(@NonNull String eventId, @NonNull String itemId,
                                        @NonNull BudgetType sourceType,
                                        @NonNull BudgetType targetType) {
        return copyItems(eventId, sourceType, targetType, itemId);
    }

    private Task<Integer> copyItems(@NonNull String eventId, @NonNull BudgetType sourceType,
                                    @NonNull BudgetType targetType,
                                    @Nullable String onlySourceItemId) {
        CollectionReference itemsCollection = firestore.collection("budgets")
                .document(eventId).collection("items");
        return itemsCollection.get().continueWithTask(readTask -> {
            QuerySnapshot snapshot = readTask.getResult();
            List<BudgetItem> sourceItems = new ArrayList<>();
            Set<String> copiedSourceIds = new HashSet<>();
            for (DocumentSnapshot document : snapshot.getDocuments()) {
                BudgetItem item = mapItem(document, eventId);
                if (item.getBudgetType() == sourceType
                        && (onlySourceItemId == null || onlySourceItemId.equals(item.getId()))) {
                    sourceItems.add(item);
                } else if (item.getBudgetType() == targetType
                        && item.getSourceBudgetItemId() != null) {
                    copiedSourceIds.add(item.getSourceBudgetItemId());
                }
            }

            WriteBatch batch = firestore.batch();
            int copyCount = 0;
            for (BudgetItem source : sourceItems) {
                if (copiedSourceIds.contains(source.getId())) continue;
                BudgetItem copy = new BudgetItem(null, eventId, targetType,
                        source.getCategoryId(), source.getDescription(), source.getQuantity(),
                        source.getDays(), source.getDailyRate(), source.getNotes(),
                        BudgetItemSource.MANUAL, null, source.getId());
                Map<String, Object> data = itemData(copy);
                data.put("sourceBudgetItemId", source.getId());
                batch.set(itemsCollection.document(targetType.name().toLowerCase(Locale.ROOT)
                        + "_" + source.getId()), data);
                copyCount++;
            }
            if (copyCount == 0) return Tasks.forResult(0);
            int finalCopyCount = copyCount;
            return batch.commit().continueWith(commitTask -> {
                if (!commitTask.isSuccessful()) {
                    throw commitTask.getException();
                }
                return finalCopyCount;
            });
        });
    }

    public void getBudgetForEvent(@NonNull String eventId,
                                  @NonNull Callback<BudgetData> callback) {
        Task<DocumentSnapshot> budgetTask = firestore.collection("budgets")
                .document(eventId).get();
        Task<QuerySnapshot> itemsTask = firestore.collection("budgets")
                .document(eventId).collection("items").get();
        Task<QuerySnapshot> categoriesTask = firestore.collection("budgetCategories").get();

        Tasks.whenAllSuccess(budgetTask, itemsTask, categoriesTask)
                .addOnSuccessListener(results -> {
                    try {
                        DocumentSnapshot budgetDocument = (DocumentSnapshot) results.get(0);
                        QuerySnapshot itemDocuments = (QuerySnapshot) results.get(1);
                        QuerySnapshot categoryDocuments = (QuerySnapshot) results.get(2);

                        Budget budget = mapBudget(budgetDocument, eventId);
                        List<BudgetItem> items = new ArrayList<>();
                        for (DocumentSnapshot document : itemDocuments.getDocuments()) {
                            items.add(mapItem(document, eventId));
                        }
                        List<BudgetCategory> categories = new ArrayList<>();
                        for (DocumentSnapshot document : categoryDocuments.getDocuments()) {
                            categories.add(mapCategory(document));
                        }
                        callback.onSuccess(new BudgetData(budget, items, categories));
                    } catch (IllegalArgumentException | IllegalStateException error) {
                        callback.onError(error);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    private Budget mapBudget(DocumentSnapshot document, String eventId) {
        if (!document.exists()) {
            return new Budget(eventId, eventId, false, BigDecimal.ZERO);
        }
        Boolean includeVat = document.getBoolean("includeVat");
        return new Budget(document.getId(), eventId, includeVat != null && includeVat,
                decimal(document.get("discountPercentage"), BigDecimal.ZERO));
    }

    private Map<String, Object> itemData(BudgetItem item) {
        Map<String, Object> data = new HashMap<>();
        data.put("budgetType", item.getBudgetType().name());
        data.put("categoryId", item.getCategoryId());
        data.put("description", item.getDescription());
        data.put("quantity", item.getQuantity().doubleValue());
        data.put("days", item.getDays().doubleValue());
        data.put("dailyRate", item.getDailyRate().doubleValue());
        data.put("notes", item.getNotes());
        data.put("sourceType", item.getSourceType().name());
        return data;
    }

    private BudgetItem mapItem(DocumentSnapshot document, String eventId) {
        String typeValue = requiredString(document, "budgetType");
        String sourceValue = optionalString(document, "sourceType");
        BudgetItemSource sourceType = sourceValue.isEmpty()
                ? BudgetItemSource.MANUAL
                : BudgetItemSource.valueOf(sourceValue.toUpperCase(Locale.ROOT));
        return new BudgetItem(document.getId(), eventId,
                BudgetType.valueOf(typeValue.toUpperCase(Locale.ROOT)),
                optionalString(document, "categoryId"),
                optionalString(document, "description"),
                decimal(document.get("quantity"), BigDecimal.ZERO),
                decimal(document.get("days"), BigDecimal.ZERO),
                decimal(document.get("dailyRate"), BigDecimal.ZERO),
                optionalString(document, "notes"), sourceType,
                nullableString(document, "sourceTransactionId"),
                nullableString(document, "sourceBudgetItemId"));
    }

    private BudgetCategory mapCategory(DocumentSnapshot document) {
        return new BudgetCategory(document.getId(), requiredString(document, "name"));
    }

    private BigDecimal decimal(Object value, BigDecimal fallback) {
        if (value == null) return fallback;
        if (value instanceof Number || value instanceof String) {
            try {
                return new BigDecimal(value.toString());
            } catch (NumberFormatException error) {
                throw new IllegalStateException("Invalid decimal value", error);
            }
        }
        throw new IllegalStateException("Invalid decimal value");
    }

    private String requiredString(DocumentSnapshot document, String field) {
        String value = document.getString(field);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing or invalid " + field + " field");
        }
        return value;
    }

    private String optionalString(DocumentSnapshot document, String field) {
        String value = document.getString(field);
        return value == null ? "" : value;
    }

    private String nullableString(DocumentSnapshot document, String field) {
        return document.getString(field);
    }
}
