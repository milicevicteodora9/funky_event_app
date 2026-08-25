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
import com.example.funkyeventapp.models.CashboxTransaction;
import com.example.funkyeventapp.models.Currency;
import com.example.funkyeventapp.models.ExpensePurpose;
import com.example.funkyeventapp.models.TransactionType;
import com.example.funkyeventapp.models.DocumentSource;
import com.example.funkyeventapp.models.Receipt;
import com.example.funkyeventapp.models.ReceiptProcessingStatus;
import com.example.funkyeventapp.models.ScannedDocument;
import com.example.funkyeventapp.models.TeamMember;
import com.example.funkyeventapp.models.TeamFee;
import com.example.funkyeventapp.models.TeamPayment;
import com.example.funkyeventapp.models.UserRole;
import com.example.funkyeventapp.repositories.MockDataRepository;

import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class MockDataRepositoryTest {
    private final MockDataRepository repository = MockDataRepository.getInstance();

    @Test public void teamDebtIsComputedFromFeesAndPaymentsAndIgnoresCredit() {
        TeamMember aleksandar = repository.getTeamMemberById("tm_1");
        assertNotNull(aleksandar);
        assertEquals(0, repository.getTotalFeesForMember(aleksandar.getId()).compareTo(new BigDecimal("3200")));
        assertEquals(0, repository.getTotalPaidForMember(aleksandar.getId()).compareTo(new BigDecimal("1600")));
        assertEquals(0, repository.getDebtForMember(aleksandar.getId()).compareTo(new BigDecimal("1600")));
        assertTrue(repository.getDebtForMember("tm_7").signum() < 0);
        assertEquals(0, repository.getTotalTeamDebt().compareTo(new BigDecimal("1970")));
    }

    @Test public void appUsersRemainLimitedToInternalRoles() {
        assertEquals(3, UserRole.values().length);
        assertEquals(UserRole.ADMIN, UserRole.valueOf("ADMIN"));
        assertEquals(UserRole.MANAGER, UserRole.valueOf("MANAGER"));
        assertEquals(UserRole.COORDINATOR, UserRole.valueOf("COORDINATOR"));
    }

    @Test public void teamFeeAndPaymentCrudAlwaysRecalculatesMemberDebt() {
        String memberId = "tm_2";
        BigDecimal feesBefore = repository.getTotalFeesForMember(memberId);
        BigDecimal paidBefore = repository.getTotalPaidForMember(memberId);
        TeamFee fee = repository.addTeamFee(new TeamFee(null, memberId, "1", "QA fee",
                new BigDecimal("400"), Currency.EUR, LocalDate.of(2026, 8, 24), ""));
        assertEquals(0, repository.getTotalFeesForMember(memberId).compareTo(feesBefore.add(new BigDecimal("400"))));
        fee.setAmount(new BigDecimal("500"));
        assertTrue(repository.updateTeamFee(fee));
        assertEquals(0, repository.getDebtForMember(memberId).compareTo(feesBefore.add(new BigDecimal("500")).subtract(paidBefore)));

        TeamPayment payment = repository.addTeamPayment(new TeamPayment(null, memberId, new BigDecimal("200"),
                Currency.EUR, LocalDate.of(2026, 8, 24), "CASH", "QA payment"));
        assertEquals(0, repository.getTotalPaidForMember(memberId).compareTo(paidBefore.add(new BigDecimal("200"))));
        payment.setAmount(new BigDecimal("300"));
        assertTrue(repository.updateTeamPayment(payment));
        assertEquals(0, repository.getDebtForMember(memberId).compareTo(feesBefore.add(new BigDecimal("500")).subtract(paidBefore.add(new BigDecimal("300")))));
        assertTrue(repository.deleteTeamFee(fee.getId()));
        assertTrue(repository.deleteTeamPayment(payment.getId()));
        assertEquals(0, repository.getTotalFeesForMember(memberId).compareTo(feesBefore));
        assertEquals(0, repository.getTotalPaidForMember(memberId).compareTo(paidBefore));
    }

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

    @Test public void cashboxEditDeleteKeepsOnlyLinkedCashboxActualInSync() {
        String cashboxId = repository.getCashboxForUser("user_teodora").getId();
        BudgetCategory category = repository.getBudgetCategories().get(0);
        BudgetItem manual = new BudgetItem(null, "1", BudgetType.ACTUAL, category.getId(), "Protected manual",
                BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("9"), "", BudgetItemSource.MANUAL, null, null);
        repository.addBudgetItem(manual);

        BigDecimal spentBefore = repository.getCashboxTotal(cashboxId, TransactionType.EXPENSE);
        CashboxTransaction transaction = transaction(cashboxId, "100", "100", TransactionType.EXPENSE,
                ExpensePurpose.GENERAL, null, category.getId());
        repository.addCashboxTransaction(transaction);
        assertEquals(0, repository.getCashboxTotal(cashboxId, TransactionType.EXPENSE)
                .compareTo(spentBefore.add(new BigDecimal("100"))));
        assertEquals(null, repository.getActualBudgetItemForTransaction(transaction.getId()));

        transaction.setAmount(new BigDecimal("120"));
        transaction.setAmountInEur(new BigDecimal("120"));
        repository.updateCashboxTransaction(transaction);
        assertEquals(0, repository.getCashboxTotal(cashboxId, TransactionType.EXPENSE)
                .compareTo(spentBefore.add(new BigDecimal("120"))));
        assertEquals(null, repository.getActualBudgetItemForTransaction(transaction.getId()));

        transaction.setExpensePurpose(ExpensePurpose.EVENT);
        transaction.setEventId("1");
        repository.updateCashboxTransaction(transaction);
        BudgetItem linked = repository.getActualBudgetItemForTransaction(transaction.getId());
        assertNotNull(linked);
        assertEquals("1", linked.getEventId());
        assertEquals(0, linked.getTotal().compareTo(new BigDecimal("120")));

        transaction.setAmount(new BigDecimal("140"));
        transaction.setAmountInEur(new BigDecimal("140"));
        repository.updateCashboxTransaction(transaction);
        assertEquals(0, repository.getActualBudgetItemForTransaction(transaction.getId()).getTotal().compareTo(new BigDecimal("140")));

        transaction.setEventId("2");
        repository.updateCashboxTransaction(transaction);
        linked = repository.getActualBudgetItemForTransaction(transaction.getId());
        assertEquals("2", linked.getEventId());
        assertFalse(hasCashboxActual("1", transaction.getId()));

        transaction.setExpensePurpose(ExpensePurpose.GENERAL);
        transaction.setEventId(null);
        repository.updateCashboxTransaction(transaction);
        assertEquals(null, repository.getActualBudgetItemForTransaction(transaction.getId()));

        transaction.setExpensePurpose(ExpensePurpose.EVENT);
        transaction.setEventId("1");
        repository.updateCashboxTransaction(transaction);
        assertNotNull(repository.getActualBudgetItemForTransaction(transaction.getId()));

        transaction.setTransactionType(TransactionType.INCOME);
        transaction.setExpensePurpose(ExpensePurpose.GENERAL);
        transaction.setEventId(null);
        repository.updateCashboxTransaction(transaction);
        assertEquals(null, repository.getActualBudgetItemForTransaction(transaction.getId()));
        assertTrue(repository.deleteCashboxTransaction(transaction.getId()));

        CashboxTransaction eventExpense = transaction(cashboxId, "33", "33", TransactionType.EXPENSE,
                ExpensePurpose.EVENT, "1", category.getId());
        repository.addCashboxTransaction(eventExpense);
        assertNotNull(repository.getActualBudgetItemForTransaction(eventExpense.getId()));
        assertTrue(repository.deleteCashboxTransaction(eventExpense.getId()));
        assertEquals(null, repository.getActualBudgetItemForTransaction(eventExpense.getId()));

        CashboxTransaction general = transaction(cashboxId, "20", "20", TransactionType.EXPENSE,
                ExpensePurpose.GENERAL, null, category.getId());
        repository.addCashboxTransaction(general);
        assertTrue(repository.deleteCashboxTransaction(general.getId()));
        assertNotNull(findBudgetItem(manual.getId()));
        assertEquals(BudgetItemSource.MANUAL, findBudgetItem(manual.getId()).getSourceType());
        repository.deleteBudgetItem(manual.getId());
    }

    private CashboxTransaction transaction(String cashboxId, String amount, String eur, TransactionType type,
            ExpensePurpose purpose, String eventId, String categoryId) {
        CashboxTransaction transaction = new CashboxTransaction(null, cashboxId, "QA transaction", "",
                new BigDecimal(amount), Currency.EUR, BigDecimal.ONE, new BigDecimal(eur), LocalDate.of(2026, 8, 24),
                type, purpose, eventId, null);
        transaction.setCategoryId(categoryId);
        return transaction;
    }

    private boolean hasCashboxActual(String eventId, String transactionId) {
        for (BudgetItem item : repository.getBudgetItems(eventId, BudgetType.ACTUAL))
            if (item.getSourceType() == BudgetItemSource.CASHBOX && transactionId.equals(item.getSourceTransactionId())) return true;
        return false;
    }

    private BudgetItem findBudgetItem(String id) {
        for (BudgetItem item : repository.getBudgetItems("1", BudgetType.ACTUAL)) if (id.equals(item.getId())) return item;
        return null;
    }

    @Test public void receiptSourcesReuseCashboxActualFlowAndManualEntriesStayIndependent() {
        String cashboxId = repository.getCashboxForUser("user_teodora").getId();
        BudgetCategory category = repository.getBudgetCategories().get(0);
        BudgetItem manual = new BudgetItem(null, "1", BudgetType.ACTUAL, category.getId(), "Receipt flow manual guard",
                BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("7"), "", BudgetItemSource.MANUAL, null, null);
        repository.addBudgetItem(manual);

        CashboxTransaction cameraGeneral = receiptTransaction(cashboxId, ExpensePurpose.GENERAL, null, category.getId());
        Receipt cameraReceipt = repository.createMockReceiptDraft();
        ScannedDocument cameraDocument = repository.createMockScannedDocument(DocumentSource.CAMERA);
        repository.saveConfirmedReceiptExpense(cameraDocument, cameraReceipt, cameraGeneral);
        assertNotNull(cameraGeneral.getReceiptId());
        assertNotNull(repository.getReceiptById(cameraGeneral.getReceiptId()));
        assertEquals(ReceiptProcessingStatus.CONFIRMED, cameraReceipt.getProcessingStatus());
        assertEquals(null, repository.getActualBudgetItemForTransaction(cameraGeneral.getId()));

        CashboxTransaction galleryEvent = receiptTransaction(cashboxId, ExpensePurpose.EVENT, "1", category.getId());
        Receipt galleryReceipt = repository.createMockReceiptDraft();
        repository.saveConfirmedReceiptExpense(repository.createMockScannedDocument(DocumentSource.GALLERY), galleryReceipt, galleryEvent);
        assertNotNull(galleryEvent.getReceiptId());
        assertNotNull(repository.getActualBudgetItemForTransaction(galleryEvent.getId()));
        assertEquals("1", repository.getActualBudgetItemForTransaction(galleryEvent.getId()).getEventId());

        CashboxTransaction pdfGeneral = receiptTransaction(cashboxId, ExpensePurpose.GENERAL, null, category.getId());
        ScannedDocument pdfDocument = repository.createMockScannedDocument(DocumentSource.PDF);
        repository.saveConfirmedReceiptExpense(pdfDocument, repository.createMockReceiptDraft(), pdfGeneral);
        assertTrue(pdfDocument.getFileName().endsWith(".pdf"));
        assertNotNull(pdfGeneral.getReceiptId());

        CashboxTransaction manualCashbox = transaction(cashboxId, "10", "10", TransactionType.EXPENSE,
                ExpensePurpose.GENERAL, null, category.getId());
        repository.addCashboxTransaction(manualCashbox);
        assertEquals(null, manualCashbox.getReceiptId());

        repository.deleteCashboxTransaction(galleryEvent.getId());
        assertEquals(null, repository.getActualBudgetItemForTransaction(galleryEvent.getId()));
        assertNotNull(findBudgetItem(manual.getId()));
        assertEquals(BudgetItemSource.MANUAL, findBudgetItem(manual.getId()).getSourceType());
        repository.deleteCashboxTransaction(cameraGeneral.getId());
        repository.deleteCashboxTransaction(pdfGeneral.getId());
        repository.deleteCashboxTransaction(manualCashbox.getId());
        repository.deleteBudgetItem(manual.getId());
    }

    private CashboxTransaction receiptTransaction(String cashboxId, ExpensePurpose purpose, String eventId, String categoryId) {
        CashboxTransaction transaction = new CashboxTransaction(null, cashboxId, "NIS Petrol", "Receipt 001234",
                new BigDecimal("5000"), Currency.RSD, new BigDecimal("117.20"), new BigDecimal("42.66"),
                LocalDate.of(2026, 8, 24), TransactionType.EXPENSE, purpose, eventId, null);
        transaction.setCategoryId(categoryId);
        return transaction;
    }
}
