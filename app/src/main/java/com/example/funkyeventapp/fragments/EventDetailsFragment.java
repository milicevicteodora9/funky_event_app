package com.example.funkyeventapp.fragments;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.example.funkyeventapp.models.BudgetItemSource;
import com.example.funkyeventapp.models.BudgetType;
import com.example.funkyeventapp.models.Client;
import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.models.EventAssignment;
import com.example.funkyeventapp.models.EventStatus;
import com.example.funkyeventapp.models.User;
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
import java.util.List;
import java.util.Locale;

public class EventDetailsFragment extends Fragment {
    private final MockDataRepository repository = MockDataRepository.getInstance();
    private final ClientRepository clientRepository = ClientRepository.getInstance();
    private final EventRepository eventRepository = EventRepository.getInstance();
    private final PdfService pdfService = new PdfService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.GERMANY));
    private Event event;
    private Budget budget;
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
        budget = repository.getBudgetForEvent(event.getId());
        bindViews(view);
        bindEvent(view);
        bindActions(view);
        renderTeam();
        renderBudget();
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
        vat.setOnCheckedChangeListener((button, checked) -> { budget.setIncludeVat(checked); renderSummary(); });
        discount.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable editable) {
                budget.setDiscountPercentage(parseDecimal(editable.toString(), BigDecimal.ZERO));
                renderSummary();
            }
        });
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
        view.findViewById(R.id.buttonAddBudgetItem).setOnClickListener(v -> showBudgetItemDialog(null));
        copyAllButton.setOnClickListener(v -> confirmCopyAll());
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
        List<BudgetItem> items = repository.getBudgetItems(event.getId(), selectedBudgetType);
        for (BudgetCategory category : repository.getBudgetCategories()) {
            boolean headingAdded = false;
            for (BudgetItem item : items) if (category.getId().equals(item.getCategoryId())) {
                if (!headingAdded) { addCategoryHeading(category.getName()); headingAdded = true; }
                addBudgetItemCard(item);
            }
        }
        copyAllButton.setVisibility(selectedBudgetType == BudgetType.EXTERNAL ? View.VISIBLE : View.GONE);
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
        BigDecimal internal = repository.getBudgetTotal(event.getId(), BudgetType.INTERNAL);
        BigDecimal actual = repository.getBudgetTotal(event.getId(), BudgetType.ACTUAL);
        externalTotal.setText(getString(R.string.total_external, money(external)));
        internalTotal.setText(getString(R.string.total_internal, money(internal)));
        actualTotal.setText(getString(R.string.total_actual, money(actual)));
        estimatedProfit.setText(getString(R.string.estimated_profit_value, money(external.subtract(internal))));
        actualProfit.setText(getString(R.string.actual_profit_value, money(external.subtract(actual))));
    }

    private BigDecimal adjustedExternal() {
        BigDecimal value = repository.getBudgetTotal(event.getId(), BudgetType.EXTERNAL);
        BigDecimal discount = budget.getDiscountPercentage() == null ? BigDecimal.ZERO : budget.getDiscountPercentage();
        value = value.multiply(BigDecimal.ONE.subtract(discount.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)));
        if (budget.isIncludeVat()) value = value.multiply(new BigDecimal("1.20"));
        return value;
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
        if (selectedBudgetType == BudgetType.EXTERNAL) row.addView(actionButton(R.drawable.ic_copy, v -> copyItem(item)));
        row.addView(actionButton(R.drawable.ic_edit, v -> showBudgetItemDialog(item)));
        row.addView(actionButton(R.drawable.ic_delete, v -> confirmDelete(item)));
        card.addView(row);
        budgetItemsContainer.addView(card);
    }

    private ImageButton actionButton(int icon, View.OnClickListener listener) {
        ImageButton button = new ImageButton(requireContext());
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(34), dp(34)));
        button.setPadding(dp(7), dp(7), dp(7), dp(7));
        button.setImageResource(icon);
        button.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        button.setOnClickListener(listener);
        return button;
    }

    private void showBudgetItemDialog(@Nullable BudgetItem existing) {
        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), 0, dp(20), 0);
        Spinner categorySpinner = new Spinner(requireContext());
        EditText newCategory = field(R.string.new_category, false);
        Button createCategory = new Button(requireContext()); createCategory.setText(R.string.create_category);
        EditText description = field(R.string.description, false);
        EditText quantity = field(R.string.quantity, true);
        EditText days = field(R.string.days, true);
        EditText rate = field(R.string.daily_rate, true);
        EditText notes = field(R.string.notes, false);
        form.addView(categorySpinner); form.addView(newCategory); form.addView(createCategory);
        form.addView(description); form.addView(quantity); form.addView(days); form.addView(rate); form.addView(notes);
        final List<BudgetCategory> categories = new java.util.ArrayList<>(repository.getBudgetCategories());
        ArrayAdapter<BudgetCategory> categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories);
        categorySpinner.setAdapter(categoryAdapter);
        createCategory.setOnClickListener(v -> {
            String name = newCategory.getText().toString().trim();
            if (name.isEmpty()) return;
            BudgetCategory created = repository.addBudgetCategory(name);
            categories.clear(); categories.addAll(repository.getBudgetCategories()); categoryAdapter.notifyDataSetChanged();
            categorySpinner.setSelection(categories.indexOf(created)); newCategory.setText(""); showToast(R.string.category_created);
        });
        if (existing != null) {
            description.setText(existing.getDescription()); quantity.setText(stripZeros(existing.getQuantity()));
            days.setText(stripZeros(existing.getDays())); rate.setText(stripZeros(existing.getDailyRate())); notes.setText(existing.getNotes());
            for (int i = 0; i < categories.size(); i++) if (categories.get(i).getId().equals(existing.getCategoryId())) categorySpinner.setSelection(i);
        }
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(existing == null ? R.string.add_item : R.string.edit)
                .setView(form).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.save, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            BigDecimal q = parseDecimal(quantity.getText().toString(), null);
            BigDecimal d = parseDecimal(days.getText().toString(), null);
            BigDecimal r = parseDecimal(rate.getText().toString(), null);
            if (description.getText().toString().trim().isEmpty() || q == null || d == null || r == null || q.signum() < 0 || d.signum() < 0 || r.signum() < 0) {
                showToast(R.string.invalid_budget_item); return;
            }
            BudgetItem target = existing == null ? new BudgetItem() : existing;
            target.setEventId(event.getId()); target.setBudgetType(existing == null ? selectedBudgetType : existing.getBudgetType());
            target.setCategoryId(((BudgetCategory) categorySpinner.getSelectedItem()).getId());
            target.setDescription(description.getText().toString().trim()); target.setQuantity(q); target.setDays(d); target.setDailyRate(r);
            target.setNotes(notes.getText().toString().trim()); target.setSourceType(BudgetItemSource.MANUAL); target.setSourceTransactionId(null);
            if (existing == null) repository.addBudgetItem(target); else repository.updateBudgetItem(target);
            dialog.dismiss(); renderBudget();
        }));
        dialog.show();
    }

    private void copyItem(BudgetItem item) {
        showToast(repository.copyBudgetItemToInternal(item.getId()) ? R.string.copied_internal : R.string.already_copied);
        renderSummary();
    }

    private void confirmCopyAll() {
        new MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.confirm_copy_all)
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.add, (d, w) -> {
                    int count = repository.copyAllExternalToInternal(event.getId());
                    Toast.makeText(requireContext(), count == 0 ? getString(R.string.nothing_to_copy) : getString(R.string.items_copied, count), Toast.LENGTH_SHORT).show();
                    renderBudget();
                }).show();
    }

    private void confirmDelete(BudgetItem item) {
        new MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.confirm_delete_item)
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.delete, (d, w) -> {
                    repository.deleteBudgetItem(item.getId()); renderBudget();
                }).show();
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

    private EditText field(int hint, boolean numeric) {
        EditText field = new EditText(requireContext()); field.setHint(hint); field.setTextSize(14);
        field.setSingleLine(true); field.setInputType(numeric ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL : InputType.TYPE_CLASS_TEXT);
        return field;
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
    private BigDecimal parseDecimal(String value, BigDecimal fallback) { try { return new BigDecimal(value.trim().replace(',', '.')); } catch (Exception e) { return fallback; } }
    private String valueOrDash(String value) { return value == null || value.trim().isEmpty() ? "—" : value; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void showToast(int message) { Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show(); }
    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
    }
}
