package com.example.funkyeventapp.services;

import androidx.annotation.NonNull;

import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.repositories.BudgetRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure aggregation of derived client finances; no values are persisted. */
public final class ClientFinanceCalculator {
    public static final class Totals {
        private final int eventCount;
        private final BigDecimal revenue;
        private final BigDecimal actualCost;

        public Totals(int eventCount, BigDecimal revenue, BigDecimal actualCost) {
            this.eventCount = eventCount;
            this.revenue = revenue == null ? BigDecimal.ZERO : revenue;
            this.actualCost = actualCost == null ? BigDecimal.ZERO : actualCost;
        }

        public int getEventCount() { return eventCount; }
        public BigDecimal getRevenue() { return revenue; }
        public BigDecimal getActualCost() { return actualCost; }
        public BigDecimal getProfit() { return revenue.subtract(actualCost); }
    }

    private ClientFinanceCalculator() { }

    @NonNull public static Map<String, Totals> byClient(
            @NonNull List<Event> events,
            @NonNull Map<String, BudgetRepository.EventFinancials> financialsByEventId) {
        Map<String, Totals> result = new HashMap<>();
        for (Event event : events) {
            if (event == null || event.getClientId() == null) continue;
            Totals current = result.getOrDefault(event.getClientId(), zero());
            result.put(event.getClientId(), addEvent(current,
                    financialsByEventId.get(event.getId())));
        }
        return result;
    }

    @NonNull public static Totals forClient(
            @NonNull String clientId,
            @NonNull List<Event> events,
            @NonNull Map<String, BudgetRepository.EventFinancials> financialsByEventId) {
        return byClient(events, financialsByEventId).getOrDefault(clientId, zero());
    }

    @NonNull public static Totals forYear(
            @NonNull List<Event> events,
            @NonNull Map<String, BudgetRepository.EventFinancials> financialsByEventId,
            int year) {
        Totals total = zero();
        for (Event event : events) {
            if (event == null || event.getStartDate() == null
                    || event.getStartDate().getYear() != year) continue;
            total = addEvent(total, financialsByEventId.get(event.getId()));
        }
        return total;
    }

    @NonNull public static Totals zero() {
        return new Totals(0, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private static Totals addEvent(Totals current,
                                   BudgetRepository.EventFinancials eventFinancials) {
        BigDecimal revenue = eventFinancials == null
                ? BigDecimal.ZERO : eventFinancials.getRevenue();
        BigDecimal actual = eventFinancials == null
                ? BigDecimal.ZERO : eventFinancials.getActualCost();
        return new Totals(current.eventCount + 1,
                current.revenue.add(revenue), current.actualCost.add(actual));
    }
}
