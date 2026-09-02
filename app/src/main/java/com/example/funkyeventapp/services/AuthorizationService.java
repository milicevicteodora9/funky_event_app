package com.example.funkyeventapp.services;

import com.example.funkyeventapp.models.User;
import com.example.funkyeventapp.models.UserRole;

public final class AuthorizationService {
    private AuthorizationService() { }

    private static boolean active(User user) { return user != null && user.isActive(); }
    public static boolean canAccessEvents(User user) {
        return active(user) && (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.MANAGER);
    }
    public static boolean canAccessClients(User user) { return active(user); }
    public static boolean canAccessCashbox(User user) { return active(user); }
    public static boolean canAccessTeam(User user) {
        return active(user) && (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.MANAGER);
    }
    public static boolean canAccessUserManagement(User user) {
        return active(user) && user.getRole() == UserRole.ADMIN;
    }
}
