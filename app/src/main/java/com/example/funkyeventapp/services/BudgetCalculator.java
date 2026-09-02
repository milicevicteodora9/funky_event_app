package com.example.funkyeventapp.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.funkyeventapp.models.Budget;
import com.example.funkyeventapp.models.BudgetItem;
import com.example.funkyeventapp.models.BudgetType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Central calculation used by every final budget and financial summary. */
public final class BudgetCalculator {
    private BudgetCalculator() { }

    public static BigDecimal calculateTotal(@Nullable Budget budget,
                                            @Nullable List<BudgetItem> items,
                                            @NonNull BudgetType type) {
        BigDecimal total = BigDecimal.ZERO;
        if (items != null) {
            for (BudgetItem item : items) {
                if (item != null && item.getBudgetType() == type) {
                    total = total.add(item.getTotal());
                }
            }
        }
        if (type != BudgetType.EXTERNAL || budget == null) return total;
        BigDecimal discount = budget.getDiscountPercentage() == null
                ? BigDecimal.ZERO : budget.getDiscountPercentage();
        total = total.multiply(BigDecimal.ONE.subtract(
                discount.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)));
        if (budget.isIncludeVat()) total = total.multiply(new BigDecimal("1.20"));
        return total;
    }
}
