package com.example.funkyeventapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.funkyeventapp.models.BudgetCategory;
import com.example.funkyeventapp.models.BudgetItem;
import com.example.funkyeventapp.models.BudgetItemSource;
import com.example.funkyeventapp.models.BudgetType;
import com.example.funkyeventapp.models.EventAssignment;
import com.example.funkyeventapp.models.User;
import com.example.funkyeventapp.repositories.MockDataRepository;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

public class MockDataRepositoryTest {
    private final MockDataRepository repository = MockDataRepository.getInstance();

    @Test public void assignmentSelectionAddAndRemoveUsesConcreteUser() {
        List<User> available = repository.getAvailableUsersForEvent("5");
        assertFalse(available.isEmpty());
        User chosen = available.get(0);
        EventAssignment assignment = repository.addEventAssignment("5", chosen.getId(), "Team member", false);
        assertNotNull(assignment);
        assertFalse(repository.getAvailableUsersForEvent("5").contains(chosen));
        assertTrue(repository.removeAssignment(assignment.getId()));
        assertTrue(repository.getAvailableUsersForEvent("5").contains(chosen));
    }

    @Test public void actualManualItemSupportsAddEditDeleteAndTotals() {
        BudgetCategory category = repository.addBudgetCategory("Test QA category");
        BudgetItem item = new BudgetItem(null, "qa_event", BudgetType.ACTUAL, category.getId(),
                "Manual actual", new BigDecimal("2"), new BigDecimal("1"), new BigDecimal("25"),
                "", BudgetItemSource.MANUAL, null, null);
        repository.addBudgetItem(item);
        assertEquals(0, repository.getBudgetTotal("qa_event", BudgetType.ACTUAL).compareTo(new BigDecimal("50")));
        assertEquals(BudgetItemSource.MANUAL, item.getSourceType());
        assertEquals(null, item.getSourceTransactionId());
        item.setDailyRate(new BigDecimal("30"));
        repository.updateBudgetItem(item);
        assertEquals(0, repository.getBudgetTotal("qa_event", BudgetType.ACTUAL).compareTo(new BigDecimal("60")));
        assertTrue(repository.deleteBudgetItem(item.getId()));
        assertEquals(0, repository.getBudgetTotal("qa_event", BudgetType.ACTUAL).compareTo(BigDecimal.ZERO));
    }

    @Test public void externalCopyCreatesIndependentInternalItemWithoutDuplicates() {
        BudgetCategory category = repository.getBudgetCategories().get(0);
        BudgetItem source = new BudgetItem(null, "qa_copy_event", BudgetType.EXTERNAL, category.getId(),
                "Copy source", BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("80"), "",
                BudgetItemSource.MANUAL, null, null);
        repository.addBudgetItem(source);
        assertTrue(repository.copyBudgetItemToInternal(source.getId()));
        assertFalse(repository.copyBudgetItemToInternal(source.getId()));
        List<BudgetItem> internal = repository.getBudgetItems("qa_copy_event", BudgetType.INTERNAL);
        assertEquals(1, internal.size());
        assertFalse(source == internal.get(0));
        assertEquals(source.getId(), internal.get(0).getSourceBudgetItemId());
    }
}
