package com.example.funkyeventapp.services;

import androidx.annotation.NonNull;

import com.example.funkyeventapp.models.User;
import com.example.funkyeventapp.models.UserRole;
import com.example.funkyeventapp.repositories.UserRepository;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.Locale;

public final class AuthService {
    public enum Result {
        SUCCESS,
        INVALID_CREDENTIALS,
        INVALID_EMAIL,
        INACTIVE_USER,
        EMAIL_EXISTS,
        WEAK_PASSWORD,
        NETWORK_ERROR,
        MISSING_USER_DOCUMENT,
        UNKNOWN_ERROR
    }

    public interface Callback {
        void onComplete(Result result, User user);
    }

    private static final AuthService INSTANCE = new AuthService();
    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();
    private User currentUser;

    private AuthService() { }

    public static AuthService getInstance() { return INSTANCE; }
    public User getCurrentUser() { return currentUser; }
    public boolean isAuthenticated() { return currentUser != null && currentUser.isActive(); }
    public boolean hasFirebaseSession() { return firebaseAuth.getCurrentUser() != null; }

    public void login(String email, String password, @NonNull Callback callback) {
        firebaseAuth.signInWithEmailAndPassword(normalize(email), password)
                .addOnSuccessListener(result -> loadAuthenticatedUser(callback))
                .addOnFailureListener(error -> callback.onComplete(mapAuthError(error, true), null));
    }

    public void restoreSession(@NonNull Callback callback) {
        if (firebaseAuth.getCurrentUser() == null) {
            callback.onComplete(Result.INVALID_CREDENTIALS, null);
            return;
        }
        if (isAuthenticated()) {
            callback.onComplete(Result.SUCCESS, currentUser);
            return;
        }
        loadAuthenticatedUser(callback);
    }

    public void signUp(User user, String password, @NonNull Callback callback) {
        firebaseAuth.createUserWithEmailAndPassword(normalize(user.getEmail()), password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        logout();
                        callback.onComplete(Result.UNKNOWN_ERROR, null);
                        return;
                    }
                    User profile = new User(firebaseUser.getUid(), user.getFirstName(), user.getLastName(),
                            normalize(user.getEmail()), UserRole.COORDINATOR, true);
                    userRepository.createUser(profile, new UserRepository.Callback<Void>() {
                        @Override public void onSuccess(Void unused) {
                            logout();
                            callback.onComplete(Result.SUCCESS, profile);
                        }

                        @Override public void onError(@NonNull Exception error) {
                            // Avoid an Auth-only account that can never load a Firestore profile.
                            firebaseUser.delete().addOnCompleteListener(task -> {
                                logout();
                                callback.onComplete(mapDataError(error), null);
                            });
                        }
                    });
                })
                .addOnFailureListener(error -> callback.onComplete(mapAuthError(error, false), null));
    }

    public void logout() {
        firebaseAuth.signOut();
        currentUser = null;
    }

    private void loadAuthenticatedUser(@NonNull Callback callback) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            currentUser = null;
            callback.onComplete(Result.INVALID_CREDENTIALS, null);
            return;
        }
        userRepository.getUserById(firebaseUser.getUid(), new UserRepository.Callback<User>() {
            @Override public void onSuccess(User user) {
                if (!user.isActive()) {
                    logout();
                    callback.onComplete(Result.INACTIVE_USER, null);
                    return;
                }
                currentUser = user;
                callback.onComplete(Result.SUCCESS, user);
            }

            @Override public void onError(@NonNull Exception error) {
                logout();
                callback.onComplete(mapDataError(error), null);
            }
        });
    }

    private Result mapAuthError(Exception error, boolean signingIn) {
        if (error instanceof FirebaseNetworkException) return Result.NETWORK_ERROR;
        if (error instanceof FirebaseAuthUserCollisionException) return Result.EMAIL_EXISTS;
        if (error instanceof FirebaseAuthWeakPasswordException) return Result.WEAK_PASSWORD;
        if (error instanceof FirebaseAuthInvalidCredentialsException) {
            return signingIn ? Result.INVALID_CREDENTIALS : Result.INVALID_EMAIL;
        }
        if (error instanceof FirebaseAuthInvalidUserException) return Result.INVALID_CREDENTIALS;
        return Result.UNKNOWN_ERROR;
    }

    private Result mapDataError(Exception error) {
        if (error instanceof UserRepository.UserNotFoundException) return Result.MISSING_USER_DOCUMENT;
        if (error instanceof FirebaseNetworkException) return Result.NETWORK_ERROR;
        if (error instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) error).getCode() == FirebaseFirestoreException.Code.UNAVAILABLE) {
            return Result.NETWORK_ERROR;
        }
        return Result.UNKNOWN_ERROR;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
