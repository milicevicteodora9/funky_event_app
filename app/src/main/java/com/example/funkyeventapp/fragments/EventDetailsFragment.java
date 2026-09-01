package com.example.funkyeventapp.fragments;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.funkyeventapp.R;
import com.example.funkyeventapp.models.Budget;
import com.example.funkyeventapp.models.BudgetCategory;
import com.example.funkyeventapp.models.BudgetItem;
import com.example.funkyeventapp.models.BudgetType;
import com.example.funkyeventapp.models.Client;
import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.models.EventAssignment;
import com.example.funkyeventapp.models.EventStatus;
import com.example.funkyeventapp.models.User;
import com.example.funkyeventapp.repositories.BudgetRepository;
import com.example.funkyeventapp.repositories.ClientRepository;
import com.example.funkyeventapp.repositories.EventRepository;
import com.example.funkyeventapp.repositories.MockDataRepository;
import com.example.funkyeventapp.services.PdfService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.math.BigDecimal;
import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EventDetailsFragment extends Fragment {
    private final MockDataRepository repository = MockDataRepository.getInstance();
    private final BudgetRepository budgetRepository = BudgetRepository.getInstance();
    private final ClientRepository clientRepository = ClientRepository.getInstance();
    private final EventRepository eventRepository = EventRepository.getInstance();
    private final PdfService pdfService = new PdfService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.GERMANY));
    private Event event;
    private Budget budget;
    private final List<BudgetItem> budgetItems = new ArrayList<>();
    private final List<BudgetCategory> budgetCategories = new ArrayList<>();
    private BudgetType selectedBudgetType = BudgetType.EXTERNAL;
    private TextView teamTitle, statusText, externalTotal, internalTotal, actualTotal, estimatedProfit, actualProfit;
    private ChipGroup teamGroup;
    private LinearLayout budgetItemsContainer;
    private MaterialButton externalTab, internalTab, actualTab, copyAllButton;

    public EventDetailsFragment() { super(R.layout.fragment_event_details); }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_event_details, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        String eventId = getArguments() == null ? null : getArguments().getString("eventId");
        if (eventId == null) {
            showEventNotFound(view);
            return;
        }

        eventRepository.getEventById(eventId, new EventRepository.Callback<Event>() {
            @Override public void onSuccess(Event loadedEvent) {
                if (!isAdded() || getView() != view) return;
                if (loadedEvent == null) {
                    showEventNotFound(view);
                    return;
                }
                showEvent(view, loadedEvent);
            }

            @Override public void onError(@NonNull Exception error) {
                if (!isAdded() || getView() != view) return;
                Toast.makeText(requireContext(), R.string.events_load_error, Toast.LENGTH_SHORT).show();
                Navigation.findNavController(view).popBackStack();
            }
        });
    }

    private void showEvent(@NonNull View view, @NonNull Event loadedEvent) {
        event = loadedEvent;
        budget = new Budget(event.getId(), event.getId(), false, BigDecimal.ZERO);
        bindViews(view);
        bindEvent(view);
        bindActions(view);
        renderTeam();
        renderBudget();
        loadBudget(view);
    }

    private void loadBudget(@NonNull View view) {
        budgetRepository.getBudgetForEvent(event.getId(),
                new BudgetRepository.Callback<BudgetRepository.BudgetData>() {
                    @Override public void onSuccess(BudgetRepository.BudgetData data) {
                        if (!isAdded() || getView() != view) return;
                        budget = data.getBudget();
                        budgetItems.clear();
                        budgetItems.addAll(data.getItems());
                        budgetCategories.clear();
                        budgetCategories.addAll(data.getCategories());
                        ((MaterialCheckBox) view.findViewById(R.id.checkIncludeVat))
                                .setChecked(budget.isIncludeVat());
                        ((EditText) view.findViewById(R.id.inputDiscount))
                                .setText(stripZeros(budget.getDiscountPercentage()));
                        renderBudget();
                    }

                    @Override public void onError(@NonNull Exception error) {
                        if (!isAdded() || getView() != view) return;
                        Toast.makeText(requireContext(), R.string.budget_load_error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showEventNotFound(@NonNull View view) {
        Toast.makeText(requireContext(), R.string.event_not_found, Toast.LENGTH_SHORT).show();
        Navigation.findNavController(view).popBackStack();
    }

    private void bindViews(View view) {
        teamTitle = view.findViewById(R.id.textTeamTitle);
        statusText = view.findViewById(R.id.textDetailStatus);
        teamGroup = view.findViewById(R.id.chipGroupTeam);
        budgetItemsContainer = view.findViewById(R.id.containerBudgetItems);
        externalTotal = view.findViewById(R.id.textExternalTotal);
        internalTotal = view.findViewById(R.id.textInternalTotal);
        actualTotal = view.findViewById(R.id.textActualTotal);
        estimatedProfit = view.findViewById(R.id.textEstimatedProfit);
        actualProfit = view.findViewById(R.id.textActualProfit);
        externalTab = view.findViewById(R.id.buttonBudgetExternal);
        internalTab = view.findViewById(R.id.buttonBudgetInternal);
        actualTab = view.findViewById(R.id.buttonBudgetActual);
        copyAllButton = view.findViewById(R.id.buttonCopyAllInternal);
    }

    private void bindEvent(View view) {
        ((TextView) view.findViewById(R.id.textDetailName)).setText(event.getName());
        ((TextView) view.findViewById(R.id.textDetailType)).setText(event.getType().getLabel());
        ((TextView) view.findViewById(R.id.textDetailDate)).setText(formatDateRange());
        ((TextView) view.findViewById(R.id.textDetailLocation)).setText(event.getLocation());
        updateStatus();
        bindClient(view);
        ((TextView) view.findViewById(R.id.textBillingEntity)).setText(valueOrDash(event.getBillingEntity()));
        ((TextView) view.findViewById(R.id.textPoNumber)).setText(valueOrDash(event.getPoNumber()));
        ((TextView) view.findViewById(R.id.textPaymentTerms)).setText(valueOrDash(event.getPaymentTerms()));
        MaterialCheckBox vat = view.findViewById(R.id.checkIncludeVat);
        EditText discount = view.findViewById(R.id.inputDiscount);
        vat.setChecked(budget.isIncludeVat());
        discount.setText(stripZeros(budget.getDiscountPercentage()));
        vat.setEnabled(false);
        discount.setEnabled(false);
    }

    private void bindClient(@NonNull View view) {
        TextView clientName = view.findViewById(R.id.textClientNameDetail);
        TextView clientContact = view.findViewById(R.id.textClientContact);
        clientName.setText(R.string.unknown_client);
        clientContact.setText("—");
        clientRepository.getClientById(event.getClientId(),
                new ClientRepository.Callback<Client>() {
                    @Override public void onSuccess(Client client) {
                        if (!isAdded() || getView() != view || client == null) return;
                        clientName.setText(client.getName());
                        clientContact.setText(client.getContactPerson() + " · " + client.getEmail());
                    }

                    @Override public void onError(@NonNull Exception error) {
                        if (!isAdded() || getView() != view) return;
                        Toast.makeText(requireContext(), R.string.clients_load_error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void bindActions(View view) {
        view.findViewById(R.id.buttonBack).setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
        view.findViewById(R.id.buttonEditEvent).setOnClickListener(v -> {
            Bundle arguments = new Bundle();
            arguments.putString("eventId", event.getId());
            Navigation.findNavController(v).navigate(
                    R.id.action_eventDetailsFragment_to_addEventFragment, arguments);
        });
        view.findViewById(R.id.buttonEditBilling).setOnClickListener(v -> showToast(R.string.edit_billing_later));
        view.findViewById(R.id.buttonCompleteEvent).setOnClickListener(v -> toggleCompleted());
        view.findViewById(R.id.buttonDeleteEvent).setOnClickListener(v -> confirmDeleteEvent(view));
        view.findViewById(R.id.buttonPdfQuote).setOnClickListener(v -> generateAndShareQuote());
        view.findViewById(R.id.buttonAddTeamUser).setOnClickListener(v -> showAddTeamDialog());
        externalTab.setOnClickListener(v -> selectBudget(BudgetType.EXTERNAL));
        internalTab.setOnClickListener(v -> selectBudget(BudgetType.INTERNAL));
        actualTab.setOnClickListener(v -> selectBudget(BudgetType.ACTUAL));
        view.findViewById(R.id.buttonAddBudgetItem).setEnabled(false);
        copyAllButton.setVisibility(View.GONE);
    }

    private void confirmDeleteEvent(@NonNull View root) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.confirm_delete_event)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) ->
                        eventRepository.deleteEvent(event.getId())
                                .addOnSuccessListener(unused -> {
                                    if (!isAdded() || getView() != root) return;
                                    Toast.makeText(requireContext(), R.string.event_deleted,
                                            Toast.LENGTH_SHORT).show();
                                    Navigation.findNavController(root)
                                            .popBackStack(R.id.eventsFragment, false);
                                })
                                .addOnFailureListener(error -> {
                                    if (!isAdded() || getView() != root) return;
                                    Toast.makeText(requireContext(), R.string.event_delete_error,
                                            Toast.LENGTH_SHORT).show();
                                }))
                .show();
    }

    private void showAddTeamDialog() {
        List<User> available = repository.getAvailableUsersForEvent(event.getId());
        if (available.isEmpty()) { showToast(R.string.no_available_users); return; }
        String[] labels = new String[available.size()];
        for (int i = 0; i < available.size(); i++) labels[i] = available.get(i).getFullName() + " — " + available.get(i).getRole().name();
        final int[] selected = {-1};
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_team_member)
                .setSingleChoiceItems(labels, -1, (d, which) -> selected[0] = which)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.add, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (selected[0] < 0) { showToast(R.string.select_user_first); return; }
            repository.addEventAssignment(event.getId(), available.get(selected[0]).getId(), "Team member", false);
            dialog.dismiss();
            renderTeam();
        }));
        dialog.show();
    }

    private void renderTeam() {
        teamGroup.removeAllViews();
        List<EventAssignment> assignments = repository.getAssignmentsForEvent(event.getId());
        teamTitle.setText(getString(R.string.team_count, assignments.size()));
        for (EventAssignment assignment : assignments) {
            User user = repository.getUserById(assignment.getUserId());
            if (user == null) continue;
            Chip chip = new Chip(requireContext());
            chip.setEnsureMinTouchTargetSize(false);
            chip.setMinHeight(dp(27));
            chip.setText(user.getFullName() + (assignment.isOwner() ? "  (owner)" : ""));
            chip.setTextSize(11.5f);
            chip.setChipStartPadding(dp(4));
            chip.setChipEndPadding(dp(2));
            chip.setChipBackgroundColor(ColorStateList.valueOf(requireContext().getColor(R.color.funky_badge)));
            chip.setTextColor(requireContext().getColor(assignment.isOwner() ? R.color.funky_completed_text : R.color.funky_text));
            chip.setCloseIconVisible(!assignment.isOwner());
            chip.setOnCloseIconClickListener(v -> { repository.removeAssignment(assignment.getId()); renderTeam(); });
            teamGroup.addView(chip);
        }
    }

    private void selectBudget(BudgetType type) { selectedBudgetType = type; renderBudget(); }

    private void renderBudget() {
        renderTabs();
        renderSummary();
        budgetItemsContainer.removeAllViews();
        List<BudgetItem> items = new ArrayList<>();
        for (BudgetItem item : budgetItems) {
            if (item.getBudgetType() == selectedBudgetType) items.add(item);
        }
        for (BudgetCategory category : budgetCategories) {
            boolean headingAdded = false;
            for (BudgetItem item : items) if (category.getId().equals(item.getCategoryId())) {
                if (!headingAdded) { addCategoryHeading(category.getName()); headingAdded = true; }
                addBudgetItemCard(item);
            }
        }
        copyAllButton.setVisibility(View.GONE);
    }

    private void renderTabs() {
        styleTab(externalTab, selectedBudgetType == BudgetType.EXTERNAL);
        styleTab(internalTab, selectedBudgetType == BudgetType.INTERNAL);
        styleTab(actualTab, selectedBudgetType == BudgetType.ACTUAL);
    }

    private void styleTab(MaterialButton button, boolean selected) {
        button.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(selected ? R.color.white : R.color.funky_badge)));
        button.setTextColor(requireContext().getColor(selected ? R.color.funky_text : R.color.funky_text_secondary));
    }

    private void renderSummary() {
        BigDecimal external = adjustedExternal();
        BigDecimal internal = getBudgetTotal(BudgetType.INTERNAL);
        BigDecimal actual = getBudgetTotal(BudgetType.ACTUAL);
        externalTotal.setText(getString(R.string.total_external, money(external)));
        internalTotal.setText(getString(R.string.total_internal, money(internal)));
        actualTotal.setText(getString(R.string.total_actual, money(actual)));
        estimatedProfit.setText(getString(R.string.estimated_profit_value, money(external.subtract(internal))));
        actualProfit.setText(getString(R.string.actual_profit_value, money(external.subtract(actual))));
    }

    private BigDecimal adjustedExternal() {
        BigDecimal value = getBudgetTotal(BudgetType.EXTERNAL);
        BigDecimal discount = budget.getDiscountPercentage() == null ? BigDecimal.ZERO : budget.getDiscountPercentage();
        value = value.multiply(BigDecimal.ONE.subtract(discount.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)));
        if (budget.isIncludeVat()) value = value.multiply(new BigDecimal("1.20"));
        return value;
    }

    private BigDecimal getBudgetTotal(BudgetType type) {
        BigDecimal total = BigDecimal.ZERO;
        for (BudgetItem item : budgetItems) {
            if (item.getBudgetType() == type) total = total.add(item.getTotal());
        }
        return total;
    }

    private void addCategoryHeading(String name) {
        TextView heading = new TextView(requireContext());
        heading.setText(name.toUpperCase(Locale.ROOT));
        heading.setTextColor(requireContext().getColor(R.color.funky_text_secondary));
        heading.setTextSize(11);
        heading.setLetterSpacing(0.08f);
        heading.setPadding(dp(4), dp(8), 0, dp(5));
        budgetItemsContainer.addView(heading);
    }

    private void addBudgetItemCard(BudgetItem item) {
        MaterialCardView card = new MaterialCardView(requireContext());
        card.setCardBackgroundColor(requireContext().getColor(R.color.funky_surface));
        card.setRadius(dp(12));
        card.setCardElevation(0);
        card.setStrokeColor(requireContext().getColor(R.color.funky_border));
        card.setStrokeWidth(dp(1));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(7);
        card.setLayoutParams(cardParams);

        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(11), dp(9), dp(6), dp(9));
        LinearLayout textColumn = new LinearLayout(requireContext());
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView description = text(item.getDescription(), 13, R.color.funky_text, true);
        TextView formula = text(stripZeros(item.getQuantity()) + " × " + stripZeros(item.getDays()) + " × " + money(item.getDailyRate()), 11, R.color.funky_text_secondary, false);
        textColumn.addView(description); textColumn.addView(formula); row.addView(textColumn);
        TextView amount = text(money(item.getTotal()), 13, R.color.funky_text, true);
        amount.setPadding(dp(6), 0, dp(4), 0); row.addView(amount);
        card.addView(row);
        budgetItemsContainer.addView(card);
    }

    private void generateAndShareQuote() {
        try {
            File pdfFile = pdfService.generateQuote(requireContext(), event);
            Toast.makeText(requireContext(), R.string.pdf_quote_generated, Toast.LENGTH_SHORT).show();
            pdfService.shareQuote(requireContext(), pdfFile);
        } catch (IOException | RuntimeException error) {
            Toast.makeText(requireContext(), R.string.pdf_quote_error, Toast.LENGTH_LONG).show();
        }
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView text = new TextView(requireContext()); text.setText(value); text.setTextSize(size); text.setTextColor(requireContext().getColor(color));
        if (bold) text.setTypeface(text.getTypeface(), android.graphics.Typeface.BOLD); return text;
    }

    private void toggleCompleted() {
        event.setCompleted(!event.isCompleted()); event.setStatus(event.isCompleted() ? EventStatus.COMPLETED : EventStatus.CURRENT); updateStatus();
        showToast(event.isCompleted() ? R.string.event_completed : R.string.event_reopened);
    }
    private void updateStatus() {
        statusText.setText(event.isCompleted() ? R.string.status_completed : R.string.status_current);
        statusText.setTextColor(requireContext().getColor(event.isCompleted() ? R.color.funky_completed_text : R.color.funky_mint));
    }
    private String formatDateRange() {
        String result = event.getStartDate().format(dateFormatter);
        if (event.getEndDate() != null && !event.getEndDate().equals(event.getStartDate())) result += " – " + event.getEndDate().format(dateFormatter);
        return result;
    }
    private String money(BigDecimal value) { return moneyFormat.format(value.setScale(2, RoundingMode.HALF_UP)) + " €"; }
    private String stripZeros(BigDecimal value) { return value == null ? "" : value.stripTrailingZeros().toPlainString(); }
    private String valueOrDash(String value) { return value == null || value.trim().isEmpty() ? "—" : value; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void showToast(int message) { Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show(); }
}
