package com.example.funkyeventapp.repositories;

import androidx.annotation.NonNull;

import com.example.funkyeventapp.models.User;
import com.example.funkyeventapp.models.UserRole;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/** Firestore access for the authenticated user's profile only. */
public final class UserRepository {
    public interface Callback<T> {
        void onSuccess(T value);
        void onError(@NonNull Exception error);
    }

    public static final class UserNotFoundException extends Exception {
        public UserNotFoundException(String uid) {
            super("Firestore user document does not exist for uid: " + uid);
        }
    }

    private static final UserRepository INSTANCE = new UserRepository();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    private UserRepository() { }

    public static UserRepository getInstance() { return INSTANCE; }

    public void createUser(@NonNull User user, @NonNull Callback<Void> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("firstName", user.getFirstName());
        data.put("lastName", user.getLastName());
        data.put("email", user.getEmail());
        data.put("role", user.getRole().name());
        data.put("active", user.isActive());

        firestore.collection("users").document(user.getId()).set(data)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void getUserById(@NonNull String uid, @NonNull Callback<User> callback) {
        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(document -> mapUser(uid, document, callback))
                .addOnFailureListener(callback::onError);
    }

    private void mapUser(String uid, DocumentSnapshot document, Callback<User> callback) {
        if (!document.exists()) {
            callback.onError(new UserNotFoundException(uid));
            return;
        }
        try {
            String firstName = requiredString(document, "firstName");
            String lastName = requiredString(document, "lastName");
            String email = requiredString(document, "email");
            String roleValue = requiredString(document, "role");
            Boolean active = document.getBoolean("active");
            if (active == null) throw new IllegalStateException("Missing active field");
            callback.onSuccess(new User(uid, firstName, lastName, email,
                    UserRole.valueOf(roleValue), active));
        } catch (IllegalArgumentException | IllegalStateException error) {
            callback.onError(error);
        }
    }

    private String requiredString(DocumentSnapshot document, String field) {
        String value = document.getString(field);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing or invalid " + field + " field");
        }
        return value;
    }
}
