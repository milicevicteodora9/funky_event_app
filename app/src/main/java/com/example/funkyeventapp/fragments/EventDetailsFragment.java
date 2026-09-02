package com.example.funkyeventapp.fragments;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
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
import com.example.funkyeventapp.models.EventStatus;
import com.example.funkyeventapp.models.User;
import com.example.funkyeventapp.repositories.BudgetRepository;
import com.example.funkyeventapp.repositories.ClientRepository;
import com.example.funkyeventapp.repositories.EventRepository;
import com.example.funkyeventapp.repositories.UserRepository;
import com.example.funkyeventapp.services.AuthService;
import com.example.funkyeventapp.services.AuthorizationService;
import com.example.funkyeventapp.services.BudgetCalculator;
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
    private final UserRepository userRepository = UserRepository.getInstance();
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
        if (!AuthorizationService.canAccessEvents(AuthService.getInstance().getCurrentUser())) {
            Toast.makeText(requireContext(), R.string.module_access_denied, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack();
            return;
        }
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
        renderAssignedUsers(view);
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
        view.findViewById(R.id.buttonAddTeamUser).setVisibility(View.GONE);
        externalTab.setOnClickListener(v -> selectBudget(BudgetType.EXTERNAL));
        internalTab.setOnClickListener(v -> selectBudget(BudgetType.INTERNAL));
        actualTab.setOnClickListener(v -> selectBudget(BudgetType.ACTUAL));
        view.findViewById(R.id.buttonAddBudgetItem).setOnClickListener(v ->
                showBudgetItemDialog(view, null));
        copyAllButton.setOnClickListener(v -> confirmCopyAll(view));
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

    private void renderAssignedUsers(@NonNull View view) {
        teamGroup.removeAllViews();
        List<String> assignedIds = event.getAssignedUserIds();
        teamTitle.setText(getString(R.string.assigned_users_count, assignedIds.size()));
        TextView emptyState = view.findViewById(R.id.textNoAssignedUsers);
        emptyState.setVisibility(assignedIds.isEmpty() ? View.VISIBLE : View.GONE);
        if (assignedIds.isEmpty()) return;
        userRepository.getAllUsers(new UserRepository.Callback<List<User>>() {
            @Override public void onSuccess(List<User> users) {
                if (!isAdded() || getView() != view) return;
                for (String userId : assignedIds) {
                    User assigned = null;
                    for (User user : users) {
                        if (userId.equals(user.getId())) {
                            assigned = user;
                            break;
                        }
                    }
                    Chip chip = new Chip(requireContext());
                    chip.setEnsureMinTouchTargetSize(false);
                    chip.setMinHeight(dp(27));
                    chip.setText(assigned == null
                            ? getString(R.string.unknown_user) : assigned.getFullName());
                    chip.setTextSize(11.5f);
                    chip.setChipStartPadding(dp(4));
                    chip.setChipEndPadding(dp(4));
                    chip.setChipBackgroundColor(ColorStateList.valueOf(
                            requireContext().getColor(R.color.funky_badge)));
                    chip.setTextColor(requireContext().getColor(R.color.funky_text));
                    teamGroup.addView(chip);
                }
            }

            @Override public void onError(@NonNull Exception error) {
                if (!isAdded() || getView() != view) return;
                emptyState.setVisibility(View.VISIBLE);
                emptyState.setText(R.string.assigned_users_load_error);
            }
        });
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
        boolean uncategorizedHeadingAdded = false;
        for (BudgetItem item : items) {
            boolean categoryExists = false;
            for (BudgetCategory category : budgetCategories) {
                if (category.getId().equals(item.getCategoryId())) {
                    categoryExists = true;
                    break;
                }
            }
            if (!categoryExists) {
                if (!uncategorizedHeadingAdded) {
                    addCategoryHeading(getString(R.string.uncategorized));
                    uncategorizedHeadingAdded = true;
                }
                addBudgetItemCard(item);
            }
        }
        boolean copySupported = selectedBudgetType == BudgetType.EXTERNAL
                || selectedBudgetType == BudgetType.INTERNAL;
        copyAllButton.setVisibility(copySupported ? View.VISIBLE : View.GONE);
        if (copySupported) {
            copyAllButton.setText(selectedBudgetType == BudgetType.EXTERNAL
                    ? R.string.copy_all_internal : R.string.copy_all_actual);
        }
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
        BigDecimal external = BudgetCalculator.calculateTotal(
                budget, budgetItems, BudgetType.EXTERNAL);
        BigDecimal internal = BudgetCalculator.calculateTotal(
                budget, budgetItems, BudgetType.INTERNAL);
        BigDecimal actual = BudgetCalculator.calculateTotal(
                budget, budgetItems, BudgetType.ACTUAL);
        externalTotal.setText(getString(R.string.total_external, money(external)));
        internalTotal.setText(getString(R.string.total_internal, money(internal)));
        actualTotal.setText(getString(R.string.total_actual, money(actual)));
        estimatedProfit.setText(getString(R.string.estimated_profit_value, money(external.subtract(internal))));
        actualProfit.setText(getString(R.string.actual_profit_value, money(external.subtract(actual))));
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
        if (item.getBudgetType() == BudgetType.EXTERNAL
                || item.getBudgetType() == BudgetType.INTERNAL) {
            row.addView(actionButton(R.drawable.ic_copy,
                    v -> copySingleBudgetItem(requireView(), item)));
        }
        row.addView(actionButton(R.drawable.ic_edit, v -> showBudgetItemDialog(requireView(), item)));
        row.addView(actionButton(R.drawable.ic_delete,
                v -> confirmDeleteBudgetItem(requireView(), item)));
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

    private void confirmDeleteBudgetItem(@NonNull View root, @NonNull BudgetItem item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.confirm_delete_item)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) ->
                        budgetRepository.deleteBudgetItem(event.getId(), item.getId())
                                .addOnSuccessListener(unused -> {
                                    if (!isAdded() || getView() != root) return;
                                    Toast.makeText(requireContext(),
                                            R.string.budget_item_deleted,
                                            Toast.LENGTH_SHORT).show();
                                    loadBudget(root);
                                })
                                .addOnFailureListener(error -> {
                                    if (!isAdded() || getView() != root) return;
                                    Toast.makeText(requireContext(),
                                            R.string.budget_item_delete_error,
                                            Toast.LENGTH_SHORT).show();
                                }))
                .show();
    }

    private void confirmCopyAll(@NonNull View root) {
        BudgetType sourceType = selectedBudgetType;
        BudgetType targetType = sourceType == BudgetType.EXTERNAL
                ? BudgetType.INTERNAL : BudgetType.ACTUAL;
        boolean hasSourceItems = false;
        for (BudgetItem item : budgetItems) {
            if (item.getBudgetType() == sourceType) {
                hasSourceItems = true;
                break;
            }
        }
        if (!hasSourceItems) {
            Toast.makeText(requireContext(), sourceType == BudgetType.EXTERNAL
                            ? R.string.no_external_budget_items
                            : R.string.no_internal_budget_items,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(sourceType == BudgetType.EXTERNAL
                        ? R.string.confirm_copy_all : R.string.confirm_copy_internal_actual)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    com.google.android.gms.tasks.Task<Integer> copyTask = sourceType
                            == BudgetType.EXTERNAL
                            ? budgetRepository.copyExternalItemsToInternal(event.getId())
                            : budgetRepository.copyInternalItemsToActual(event.getId());
                    copyTask
                                .addOnSuccessListener(copiedCount -> {
                                    if (!isAdded() || getView() != root) return;
                                    Toast.makeText(requireContext(), copiedCount == 0
                                                    ? getString(R.string.nothing_to_copy)
                                                    : getString(R.string.items_copied, copiedCount),
                                            Toast.LENGTH_SHORT).show();
                                    if (copiedCount > 0 && targetType == BudgetType.ACTUAL) {
                                        selectedBudgetType = BudgetType.ACTUAL;
                                    }
                                    loadBudget(root);
                                })
                                .addOnFailureListener(error -> {
                                    if (!isAdded() || getView() != root) return;
                                    Toast.makeText(requireContext(), R.string.budget_copy_error,
                                            Toast.LENGTH_SHORT).show();
                                });
                })
                .show();
    }

    private void copySingleBudgetItem(@NonNull View root, @NonNull BudgetItem item) {
        BudgetType targetType = item.getBudgetType() == BudgetType.EXTERNAL
                ? BudgetType.INTERNAL : BudgetType.ACTUAL;
        budgetRepository.copyBudgetItem(event.getId(), item.getId(),
                        item.getBudgetType(), targetType)
                .addOnSuccessListener(copiedCount -> {
                    if (!isAdded() || getView() != root) return;
                    if (copiedCount == 0) {
                        Toast.makeText(requireContext(), R.string.already_copied,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(requireContext(), targetType == BudgetType.INTERNAL
                                    ? R.string.copied_internal : R.string.copied_actual,
                            Toast.LENGTH_SHORT).show();
                    if (targetType == BudgetType.ACTUAL) {
                        selectedBudgetType = BudgetType.ACTUAL;
                    }
                    loadBudget(root);
                })
                .addOnFailureListener(error -> {
                    if (!isAdded() || getView() != root) return;
                    Toast.makeText(requireContext(), R.string.budget_copy_error,
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showBudgetItemDialog(@NonNull View root, @Nullable BudgetItem existing) {
        if (budgetCategories.isEmpty()) {
            Toast.makeText(requireContext(), R.string.no_budget_categories,
                    Toast.LENGTH_SHORT).show();
        }

        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), 0, dp(20), 0);
        TextView emptyCategories = text(getString(R.string.no_budget_categories), 12,
                R.color.funky_text_secondary, false);
        emptyCategories.setVisibility(budgetCategories.isEmpty() ? View.VISIBLE : View.GONE);
        Spinner categorySpinner = new Spinner(requireContext());
        ArrayAdapter<BudgetCategory> categoryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, budgetCategories);
        categorySpinner.setAdapter(categoryAdapter);
        EditText newCategory = field(R.string.new_category, false);
        Button createCategory = new Button(requireContext());
        createCategory.setText(R.string.create_category);
        LinearLayout categoryActions = new LinearLayout(requireContext());
        categoryActions.setOrientation(LinearLayout.HORIZONTAL);
        Button renameCategory = new Button(requireContext());
        renameCategory.setText(R.string.rename_category);
        Button deleteCategory = new Button(requireContext());
        deleteCategory.setText(R.string.delete_category);
        categoryActions.addView(renameCategory,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        categoryActions.addView(deleteCategory,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        EditText description = field(R.string.description, false);
        EditText quantity = field(R.string.quantity, true);
        EditText days = field(R.string.days, true);
        EditText rate = field(R.string.daily_rate, true);
        EditText notes = field(R.string.notes, false);
        form.addView(emptyCategories);
        form.addView(categorySpinner);
        form.addView(newCategory);
        form.addView(createCategory);
        form.addView(categoryActions);
        form.addView(description);
        form.addView(quantity);
        form.addView(days);
        form.addView(rate);
        form.addView(notes);

        if (existing != null) {
            description.setText(existing.getDescription());
            quantity.setText(stripZeros(existing.getQuantity()));
            days.setText(stripZeros(existing.getDays()));
            rate.setText(stripZeros(existing.getDailyRate()));
            notes.setText(existing.getNotes());
            for (int i = 0; i < budgetCategories.size(); i++) {
                if (budgetCategories.get(i).getId().equals(existing.getCategoryId())) {
                    categorySpinner.setSelection(i);
                    break;
                }
            }
        }

        createCategory.setOnClickListener(v -> createBudgetCategory(root, newCategory,
                categorySpinner, categoryAdapter, emptyCategories, createCategory));
        renameCategory.setOnClickListener(v -> {
            BudgetCategory selected = (BudgetCategory) categorySpinner.getSelectedItem();
            if (selected == null) {
                Toast.makeText(requireContext(), R.string.no_budget_categories,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            showRenameBudgetCategoryDialog(root, selected, categoryAdapter);
        });
        deleteCategory.setOnClickListener(v -> {
            BudgetCategory selected = (BudgetCategory) categorySpinner.getSelectedItem();
            if (selected == null) {
                Toast.makeText(requireContext(), R.string.no_budget_categories,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            confirmDeleteBudgetCategory(root, selected, categoryAdapter, emptyCategories);
        });

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(existing == null ? R.string.add_item : R.string.edit_budget_item)
                .setView(form)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(
                androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            BigDecimal parsedQuantity = parseDecimal(quantity.getText().toString(), null);
            BigDecimal parsedDays = parseDecimal(days.getText().toString(), null);
            BigDecimal parsedRate = parseDecimal(rate.getText().toString(), null);
            if (description.getText().toString().trim().isEmpty()
                    || parsedQuantity == null || parsedDays == null || parsedRate == null
                    || parsedQuantity.signum() < 0 || parsedDays.signum() < 0
                    || parsedRate.signum() < 0) {
                Toast.makeText(requireContext(), R.string.invalid_budget_item,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            BudgetCategory category = (BudgetCategory) categorySpinner.getSelectedItem();
            if (category == null) {
                Toast.makeText(requireContext(), R.string.no_budget_categories,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            BudgetItem item = new BudgetItem(existing == null ? null : existing.getId(),
                    event.getId(), existing == null ? selectedBudgetType : existing.getBudgetType(),
                    category.getId(), description.getText().toString().trim(),
                    parsedQuantity, parsedDays, parsedRate,
                    notes.getText().toString().trim(),
                    existing == null ? BudgetItemSource.MANUAL : existing.getSourceType(),
                    existing == null ? null : existing.getSourceTransactionId(),
                    existing == null ? null : existing.getSourceBudgetItemId());
            View saveButton = dialog.getButton(
                    androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            saveButton.setEnabled(false);
            com.google.android.gms.tasks.Task<Void> saveTask = existing == null
                    ? budgetRepository.createBudgetItem(item)
                    : budgetRepository.updateBudgetItem(event.getId(), item);
            saveTask
                    .addOnSuccessListener(unused -> {
                        if (!isAdded() || getView() != root) return;
                        dialog.dismiss();
                        Toast.makeText(requireContext(), existing == null
                                        ? R.string.budget_item_saved
                                        : R.string.budget_item_updated,
                                Toast.LENGTH_SHORT).show();
                        loadBudget(root);
                    })
                    .addOnFailureListener(error -> {
                        if (!isAdded() || getView() != root) return;
                        saveButton.setEnabled(true);
                        Toast.makeText(requireContext(), existing == null
                                        ? R.string.budget_item_save_error
                                        : R.string.budget_item_update_error,
                                Toast.LENGTH_SHORT).show();
                    });
        }));
        dialog.show();
    }

    private void createBudgetCategory(@NonNull View root, @NonNull EditText input,
                                      @NonNull Spinner spinner,
                                      @NonNull ArrayAdapter<BudgetCategory> adapter,
                                      @NonNull TextView emptyCategories,
                                      @NonNull Button createButton) {
        String name = input.getText().toString().trim();
        if (name.isEmpty()) {
            input.setError(getString(R.string.category_name_required));
            return;
        }
        BudgetCategory category = new BudgetCategory(null, name);
        createButton.setEnabled(false);
        budgetRepository.createBudgetCategory(category)
                .addOnSuccessListener(unused -> {
                    if (!isAdded() || getView() != root) return;
                    budgetCategories.add(category);
                    adapter.notifyDataSetChanged();
                    spinner.setSelection(budgetCategories.size() - 1);
                    emptyCategories.setVisibility(View.GONE);
                    input.setText("");
                    createButton.setEnabled(true);
                    renderBudget();
                    Toast.makeText(requireContext(), R.string.category_created,
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(error -> {
                    if (!isAdded() || getView() != root) return;
                    createButton.setEnabled(true);
                    Toast.makeText(requireContext(), R.string.category_save_error,
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showRenameBudgetCategoryDialog(@NonNull View root,
                                                @NonNull BudgetCategory category,
                                                @NonNull ArrayAdapter<BudgetCategory> adapter) {
        EditText input = field(R.string.category_name, false);
        input.setText(category.getName());
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.rename_category)
                .setView(input)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(
                androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                input.setError(getString(R.string.category_name_required));
                return;
            }
            BudgetCategory updated = new BudgetCategory(category.getId(), name);
            View saveButton = dialog.getButton(
                    androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            saveButton.setEnabled(false);
            budgetRepository.updateBudgetCategory(updated)
                    .addOnSuccessListener(unused -> {
                        if (!isAdded() || getView() != root) return;
                        category.setName(name);
                        adapter.notifyDataSetChanged();
                        renderBudget();
                        dialog.dismiss();
                        Toast.makeText(requireContext(), R.string.category_updated,
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(error -> {
                        if (!isAdded() || getView() != root) return;
                        saveButton.setEnabled(true);
                        Toast.makeText(requireContext(), R.string.category_update_error,
                                Toast.LENGTH_SHORT).show();
                    });
        }));
        dialog.show();
    }

    private void confirmDeleteBudgetCategory(@NonNull View root,
                                             @NonNull BudgetCategory category,
                                             @NonNull ArrayAdapter<BudgetCategory> adapter,
                                             @NonNull TextView emptyCategories) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(getString(R.string.delete_category_question, category.getName()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) ->
                        budgetRepository.deleteBudgetCategory(category.getId())
                                .addOnSuccessListener(unused -> {
                                    if (!isAdded() || getView() != root) return;
                                    budgetCategories.remove(category);
                                    adapter.notifyDataSetChanged();
                                    emptyCategories.setVisibility(budgetCategories.isEmpty()
                                            ? View.VISIBLE : View.GONE);
                                    renderBudget();
                                    Toast.makeText(requireContext(), R.string.category_deleted,
                                            Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(error -> {
                                    if (!isAdded() || getView() != root) return;
                                    Toast.makeText(requireContext(), R.string.category_delete_error,
                                            Toast.LENGTH_SHORT).show();
                                }))
                .show();
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

    private EditText field(int hint, boolean numeric) {
        EditText field = new EditText(requireContext());
        field.setHint(hint);
        field.setTextSize(14);
        field.setSingleLine(true);
        field.setInputType(numeric
                ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                : InputType.TYPE_CLASS_TEXT);
        return field;
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
}
