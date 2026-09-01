package com.example.funkyeventapp.repositories;

import androidx.annotation.NonNull;

import com.example.funkyeventapp.models.Budget;
import com.example.funkyeventapp.models.BudgetCategory;
import com.example.funkyeventapp.models.BudgetItem;
import com.example.funkyeventapp.models.BudgetItemSource;
import com.example.funkyeventapp.models.BudgetType;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

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
                requiredString(document, "categoryId"),
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
