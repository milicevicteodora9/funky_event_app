package com.example.funkyeventapp.repositories;

import androidx.annotation.NonNull;

import com.example.funkyeventapp.models.Cashbox;
import com.example.funkyeventapp.models.CashboxTransaction;
import com.example.funkyeventapp.models.Currency;
import com.example.funkyeventapp.models.ExpensePurpose;
import com.example.funkyeventapp.models.TransactionType;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Read access to the authenticated user's Firestore cashbox. */
public final class CashboxRepository {
    public interface Callback<T> {
        void onSuccess(T value);
        void onError(@NonNull Exception error);
    }

    public static final class CashboxData {
        private final Cashbox cashbox;
        private final List<CashboxTransaction> transactions;

        private CashboxData(Cashbox cashbox, List<CashboxTransaction> transactions) {
            this.cashbox = cashbox;
            this.transactions = Collections.unmodifiableList(new ArrayList<>(transactions));
        }

        public Cashbox getCashbox() { return cashbox; }
        public List<CashboxTransaction> getTransactions() { return transactions; }
    }

    private static final CashboxRepository INSTANCE = new CashboxRepository();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    private CashboxRepository() { }

    public static CashboxRepository getInstance() { return INSTANCE; }

    public void getCashboxForCurrentUser(@NonNull Callback<CashboxData> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError(new IllegalStateException("Authenticated user is required"));
            return;
        }

        String userId = user.getUid();
        firestore.collection("cashboxes").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onSuccess(new CashboxData(emptyCashbox(userId), Collections.emptyList()));
                        return;
                    }
                    try {
                        Cashbox cashbox = mapCashbox(document, userId);
                        loadTransactions(userId, cashbox, callback);
                    } catch (IllegalArgumentException | IllegalStateException error) {
                        callback.onError(error);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    private void loadTransactions(String userId, Cashbox cashbox, Callback<CashboxData> callback) {
        firestore.collection("cashboxes").document(userId).collection("transactions").get()
                .addOnSuccessListener(snapshot -> {
                    try {
                        List<CashboxTransaction> transactions = new ArrayList<>();
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            transactions.add(mapTransaction(document, userId));
                        }
                        transactions.sort((first, second) -> second.getDate().compareTo(first.getDate()));
                        callback.onSuccess(new CashboxData(cashbox, transactions));
                    } catch (IllegalArgumentException | IllegalStateException error) {
                        callback.onError(error);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    private Cashbox emptyCashbox(String userId) {
        Cashbox cashbox = new Cashbox(userId, userId, Currency.EUR);
        cashbox.setReceivedAmount(BigDecimal.ZERO);
        return cashbox;
    }

    private Cashbox mapCashbox(DocumentSnapshot document, String authenticatedUserId) {
        String storedUserId = optionalString(document, "userId");
        if (!storedUserId.isEmpty() && !authenticatedUserId.equals(storedUserId)) {
            throw new IllegalStateException("Cashbox owner does not match authenticated user");
        }
        Cashbox cashbox = new Cashbox(document.getId(), authenticatedUserId,
                optionalCurrency(document, "displayCurrency", Currency.EUR));
        cashbox.setReceivedAmount(optionalDecimal(document, "receivedAmount", BigDecimal.ZERO));
        cashbox.setCreatedAt(optionalDateTime(document, "createdAt"));
        return cashbox;
    }

    private CashboxTransaction mapTransaction(DocumentSnapshot document, String cashboxId) {
        String description = optionalString(document, "description");
        String name = optionalString(document, "name");
        if (name.isEmpty()) name = description;
        BigDecimal amount = requiredDecimal(document, "amount");
        Currency currency = optionalCurrency(document, "currency", Currency.EUR);
        BigDecimal exchangeRate = optionalDecimal(document, "exchangeRate", BigDecimal.ONE);
        BigDecimal amountInEur = optionalDecimal(document, "amountInEur",
                currency == Currency.EUR ? amount : amount.divide(exchangeRate, 2, java.math.RoundingMode.HALF_UP));
        String eventId = nullableString(document, "eventId");
        TransactionType type = transactionType(document);
        LocalDate date = transactionDate(document);

        CashboxTransaction transaction = new CashboxTransaction(document.getId(), cashboxId, name,
                description, amount, currency, exchangeRate, amountInEur, date, type,
                eventId == null ? ExpensePurpose.GENERAL : ExpensePurpose.EVENT, eventId,
                nullableString(document, document.contains("receiptUri") ? "receiptUri" : "receiptId"));
        transaction.setCategoryId(nullableString(document, "categoryId"));
        return transaction;
    }

    private TransactionType transactionType(DocumentSnapshot document) {
        String value = optionalString(document, "type");
        if (value.isEmpty()) value = optionalString(document, "transactionType");
        if (value.isEmpty()) throw new IllegalStateException("Missing transaction type field");
        return TransactionType.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private LocalDate transactionDate(DocumentSnapshot document) {
        Object value = document.contains("createdAt") ? document.get("createdAt") : document.get("date");
        if (value instanceof Timestamp) return toLocalDate(((Timestamp) value).toDate());
        if (value instanceof Date) return toLocalDate((Date) value);
        if (value instanceof String) {
            try { return LocalDate.parse((String) value); }
            catch (DateTimeParseException error) { throw new IllegalStateException("Invalid transaction date", error); }
        }
        throw new IllegalStateException("Missing transaction createdAt field");
    }

    private LocalDateTime optionalDateTime(DocumentSnapshot document, String field) {
        Object value = document.get(field);
        if (value == null) return null;
        if (value instanceof Timestamp) return toLocalDateTime(((Timestamp) value).toDate());
        if (value instanceof Date) return toLocalDateTime((Date) value);
        if (value instanceof String) {
            try { return LocalDateTime.parse((String) value); }
            catch (DateTimeParseException error) { throw new IllegalStateException("Invalid " + field + " field", error); }
        }
        throw new IllegalStateException("Invalid " + field + " field");
    }

    private BigDecimal requiredDecimal(DocumentSnapshot document, String field) {
        Object value = document.get(field);
        if (value == null) throw new IllegalStateException("Missing " + field + " field");
        return decimal(value, field);
    }

    private BigDecimal optionalDecimal(DocumentSnapshot document, String field, BigDecimal fallback) {
        Object value = document.get(field);
        return value == null ? fallback : decimal(value, field);
    }

    private BigDecimal decimal(Object value, String field) {
        if (value instanceof Number || value instanceof String) {
            try { return new BigDecimal(String.valueOf(value)); }
            catch (NumberFormatException error) { throw new IllegalStateException("Invalid " + field + " field", error); }
        }
        throw new IllegalStateException("Invalid " + field + " field");
    }

    private Currency optionalCurrency(DocumentSnapshot document, String field, Currency fallback) {
        String value = optionalString(document, field);
        return value.isEmpty() ? fallback : Currency.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private String optionalString(DocumentSnapshot document, String field) {
        String value = document.getString(field);
        return value == null ? "" : value.trim();
    }

    private String nullableString(DocumentSnapshot document, String field) {
        String value = optionalString(document, field);
        return value.isEmpty() ? null : value;
    }

    private LocalDate toLocalDate(Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneOffset.UTC).toLocalDate();
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneOffset.UTC).toLocalDateTime();
    }
}
