package com.example.funkyeventapp.models;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventAssignmentTest {
    @Test public void missingAssignedUsersDefaultsToEmptyList() {
        Event event = new Event();

        assertTrue(event.getAssignedUserIds().isEmpty());
        assertFalse(event.isAssignedToUser("coordinator-1"));
    }

    @Test public void assignedUsersAreDefensivelyStoredAndReturned() {
        Event event = new Event();
        List<String> source = new ArrayList<>(Arrays.asList("manager-1", "coordinator-1"));

        event.setAssignedUserIds(source);
        source.clear();
        event.getAssignedUserIds().clear();

        assertTrue(event.isAssignedToUser("manager-1"));
        assertTrue(event.isAssignedToUser("coordinator-1"));
    }

    @Test public void nullAssignedUsersIsHandledAsEmptyList() {
        Event event = new Event();

        event.setAssignedUserIds(null);

        assertTrue(event.getAssignedUserIds().isEmpty());
    }
}
