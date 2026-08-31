package com.example.funkyeventapp.repositories;

import androidx.annotation.NonNull;

import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.models.EventStatus;
import com.example.funkyeventapp.models.EventType;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Firestore access for events. */
public final class EventRepository {
    public interface Callback<T> {
        void onSuccess(T value);
        void onError(@NonNull Exception error);
    }

    private static final EventRepository INSTANCE = new EventRepository();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    private EventRepository() { }

    public static EventRepository getInstance() { return INSTANCE; }

    public void getAllEvents(@NonNull Callback<List<Event>> callback) {
        firestore.collection("events").get()
                .addOnSuccessListener(snapshot -> {
                    try {
                        List<Event> events = new ArrayList<>();
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            events.add(mapEvent(document));
                        }
                        callback.onSuccess(events);
                    } catch (IllegalArgumentException | IllegalStateException error) {
                        callback.onError(error);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    private Event mapEvent(DocumentSnapshot document) {
        EventType type = EventType.valueOf(requiredString(document, "type")
                .toUpperCase(Locale.ROOT));
        Boolean completedField = document.getBoolean("completed");
        String statusField = document.getString("status");
        EventStatus status;
        if (statusField != null && !statusField.trim().isEmpty()) {
            status = EventStatus.valueOf(statusField.toUpperCase(Locale.ROOT));
        } else if (completedField != null) {
            status = completedField ? EventStatus.COMPLETED : EventStatus.CURRENT;
        } else {
            throw new IllegalStateException("Missing status field");
        }
        boolean completed = completedField != null
                ? completedField
                : status == EventStatus.COMPLETED;

        return new Event(document.getId(), requiredString(document, "name"), type,
                requiredDate(document, "startDate"), optionalDate(document, "endDate"),
                requiredString(document, "location"), status,
                requiredString(document, "clientId"), optionalString(document, "billingEntity"),
                optionalString(document, "poNumber"), optionalString(document, "paymentTerms"),
                optionalString(document, "notes"), completed);
    }

    private LocalDate requiredDate(DocumentSnapshot document, String field) {
        LocalDate value = optionalDate(document, field);
        if (value == null) throw new IllegalStateException("Missing or invalid " + field + " field");
        return value;
    }

    private LocalDate optionalDate(DocumentSnapshot document, String field) {
        Object value = document.get(field);
        if (value == null) return null;
        if (value instanceof Timestamp) {
            return toLocalDate(((Timestamp) value).toDate());
        }
        if (value instanceof Date) return toLocalDate((Date) value);
        if (value instanceof String) {
            try {
                return LocalDate.parse((String) value);
            } catch (DateTimeParseException error) {
                throw new IllegalStateException("Invalid " + field + " field", error);
            }
        }
        throw new IllegalStateException("Invalid " + field + " field");
    }

    private LocalDate toLocalDate(Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneOffset.UTC).toLocalDate();
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
}
