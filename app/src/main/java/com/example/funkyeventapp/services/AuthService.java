package com.example.funkyeventapp.services;

import com.example.funkyeventapp.models.User;
import com.example.funkyeventapp.repositories.MockDataRepository;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class AuthService {
    public enum Result { SUCCESS, INVALID_CREDENTIALS, INACTIVE_USER, EMAIL_EXISTS }
    private static final AuthService INSTANCE = new AuthService();
    private final MockDataRepository repository = MockDataRepository.getInstance();
    private final Map<String, String> mockPasswords = new HashMap<>();
    private User currentUser;

    private AuthService() {
        mockPasswords.put("teodora@funkybusiness.rs", "funky123");
        mockPasswords.put("bojana@funkybusiness.rs", "funky123");
        mockPasswords.put("nikola@funkybusiness.rs", "funky123");
        mockPasswords.put("valentina@funkybusiness.rs", "funky123");
        mockPasswords.put("vladica@funkybusiness.rs", "funky123");
    }

    public static AuthService getInstance() { return INSTANCE; }
    public User getCurrentUser() { return currentUser; }
    public boolean isAuthenticated() { return currentUser != null && currentUser.isActive(); }

    public Result login(String email, String password) {
        String key = normalize(email);
        User found = null;
        for (User user : repository.getUsers()) if (key.equals(normalize(user.getEmail()))) { found = user; break; }
        if (found == null || !password.equals(mockPasswords.get(key))) return Result.INVALID_CREDENTIALS;
        if (!found.isActive()) return Result.INACTIVE_USER;
        currentUser = found;
        return Result.SUCCESS;
    }

    public Result signUp(User user, String password) {
        String key = normalize(user.getEmail());
        if (repository.emailExists(key)) return Result.EMAIL_EXISTS;
        repository.addUser(user);
        mockPasswords.put(key, password);
        return Result.SUCCESS;
    }

    public void logout() { currentUser = null; }
    private String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
}
