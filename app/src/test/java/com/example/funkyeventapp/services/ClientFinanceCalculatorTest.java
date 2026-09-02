package com.example.funkyeventapp.services;

import static org.junit.Assert.assertEquals;

import com.example.funkyeventapp.models.Budget;
import com.example.funkyeventapp.models.BudgetItem;
import com.example.funkyeventapp.models.BudgetItemSource;
import com.example.funkyeventapp.models.BudgetType;
import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.models.EventStatus;
import com.example.funkyeventapp.models.EventType;
import com.example.funkyeventapp.repositories.BudgetRepository;

import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientFinanceCalculatorTest {
    @Test public void clientWithoutEventsHasZeroTotals() {
        ClientFinanceCalculator.Totals totals = ClientFinanceCalculator.forClient(
                "client-1", Collections.emptyList(), Collections.emptyMap());

        assertTotals(totals, 0, "0", "0", "0");
    }

    @Test public void externalBudgetUsesQuantityDaysDiscountAndVat() {
        Budget budget = new Budget("event-1", "event-1", true, new BigDecimal("10"));
        List<BudgetItem> items = Arrays.asList(
                item("event-1", BudgetType.EXTERNAL, "2", "3", "100"),
                item("event-1", BudgetType.ACTUAL, "2", "1", "200"),
                item("event-1", BudgetType.INTERNAL, "1", "1", "9999"));

        BigDecimal revenue = BudgetCalculator.calculateTotal(
                budget, items, BudgetType.EXTERNAL);
        BigDecimal actual = BudgetCalculator.calculateTotal(
                budget, items, BudgetType.ACTUAL);

        assertDecimal("648", revenue);
        assertDecimal("400", actual);
        assertDecimal("248", revenue.subtract(actual));
    }

    @Test public void clientWithMultipleEventsIncludesMissingBudgetAsZero() {
        List<Event> events = Arrays.asList(
                event("event-1", "client-1", 2026),
                event("event-2", "client-1", 2026),
                event("event-3", "client-2", 2026));
        Map<String, BudgetRepository.EventFinancials> financials = new HashMap<>();
        financials.put("event-1", new BudgetRepository.EventFinancials(
                new BigDecimal("1000"), new BigDecimal("400")));

        ClientFinanceCalculator.Totals totals = ClientFinanceCalculator.forClient(
                "client-1", events, financials);

        assertTotals(totals, 2, "1000", "400", "600");
    }

    @Test public void profitCanBeNegative() {
        Event event = event("event-1", "client-1", 2026);
        Map<String, BudgetRepository.EventFinancials> financials = Collections.singletonMap(
                "event-1", new BudgetRepository.EventFinancials(
                        new BigDecimal("100"), new BigDecimal("175")));

        ClientFinanceCalculator.Totals totals = ClientFinanceCalculator.forClient(
                "client-1", Collections.singletonList(event), financials);

        assertTotals(totals, 1, "100", "175", "-75");
    }

    @Test public void yearlyTotalsUseEventStartYearOnly() {
        List<Event> events = Arrays.asList(
                event("event-2026-a", "client-1", 2026),
                event("event-2026-b", "client-2", 2026),
                event("event-2025", "client-1", 2025));
        Map<String, BudgetRepository.EventFinancials> financials = new HashMap<>();
        financials.put("event-2026-a", finance("500", "200"));
        financials.put("event-2026-b", finance("300", "350"));
        financials.put("event-2025", finance("900", "100"));

        ClientFinanceCalculator.Totals totals = ClientFinanceCalculator.forYear(
                events, financials, 2026);

        assertTotals(totals, 2, "800", "550", "250");
    }

    private BudgetRepository.EventFinancials finance(String revenue, String actual) {
        return new BudgetRepository.EventFinancials(
                new BigDecimal(revenue), new BigDecimal(actual));
    }

    private Event event(String id, String clientId, int year) {
        return new Event(id, "Event", EventType.EVENT, LocalDate.of(year, 5, 10),
                LocalDate.of(year, 5, 11), "Belgrade", EventStatus.CURRENT, clientId,
                "", "", "", "", false);
    }

    private BudgetItem item(String eventId, BudgetType type, String quantity,
                            String days, String dailyRate) {
        return new BudgetItem(null, eventId, type, "category", "Item",
                new BigDecimal(quantity), new BigDecimal(days), new BigDecimal(dailyRate),
                "", BudgetItemSource.MANUAL, null, null);
    }

    private void assertTotals(ClientFinanceCalculator.Totals totals, int eventCount,
                              String revenue, String actual, String profit) {
        assertEquals(eventCount, totals.getEventCount());
        assertDecimal(revenue, totals.getRevenue());
        assertDecimal(actual, totals.getActualCost());
        assertDecimal(profit, totals.getProfit());
    }

    private void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
