package com.example.funkyeventapp.services;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.funkyeventapp.models.User;
import com.example.funkyeventapp.models.UserRole;

import org.junit.Test;

public class AuthorizationServiceTest {
    @Test public void adminAndManagerCanAccessEvents() {
        assertTrue(AuthorizationService.canAccessEvents(user(UserRole.ADMIN, true)));
        assertTrue(AuthorizationService.canAccessEvents(user(UserRole.MANAGER, true)));
    }

    @Test public void coordinatorAndInactiveUsersCannotAccessEvents() {
        assertFalse(AuthorizationService.canAccessEvents(user(UserRole.COORDINATOR, true)));
        assertFalse(AuthorizationService.canAccessEvents(user(UserRole.ADMIN, false)));
    }

    private User user(UserRole role, boolean active) {
        return new User("uid", "Test", "User", "test@example.com", role, active);
    }
}
