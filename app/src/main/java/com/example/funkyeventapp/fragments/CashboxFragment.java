package com.example.funkyeventapp.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Environment;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.funkyeventapp.R;
import com.example.funkyeventapp.adapters.CashboxTransactionAdapter;
import com.example.funkyeventapp.models.Cashbox;
import com.example.funkyeventapp.models.CashboxTransaction;
import com.example.funkyeventapp.models.BudgetCategory;
import com.example.funkyeventapp.models.Currency;
import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.models.ExpensePurpose;
import com.example.funkyeventapp.models.DocumentSource;
import com.example.funkyeventapp.models.TransactionType;
import com.example.funkyeventapp.repositories.CashboxRepository;
import com.example.funkyeventapp.repositories.BudgetRepository;
import com.example.funkyeventapp.repositories.EventRepository;
import com.example.funkyeventapp.models.User;
import com.example.funkyeventapp.services.AuthService;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.io.File;
import java.io.IOException;

public class CashboxFragment extends Fragment {
    private static final String TAG = "CashboxFragment";
    private final CashboxRepository cashboxRepository = CashboxRepository.getInstance();
    private final EventRepository eventRepository = EventRepository.getInstance();
    private final BudgetRepository budgetRepository = BudgetRepository.getInstance();
    private final DecimalFormat money = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.GERMANY));
    private final DateTimeFormatter inputDate = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private Cashbox cashbox;
    private View root;
    private CashboxTransactionAdapter adapter;
    private final List<CashboxTransaction> cashboxEntries = new ArrayList<>();
    private TransactionType entryFilter;
    private Uri pendingCameraUri;
    private String pendingCameraFileName;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String[]> galleryLauncher;
    private ActivityResultLauncher<String[]> pdfLauncher;

    public CashboxFragment() { super(R.layout.fragment_cashbox); }

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (state != null) {
            String uri = state.getString("pendingCameraUri");
            pendingCameraUri = uri == null ? null : Uri.parse(uri);
            pendingCameraFileName = state.getString("pendingCameraFileName");
        }
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && pendingCameraUri != null) {
                openReceiptReview(DocumentSource.CAMERA, pendingCameraUri,
                        pendingCameraFileName, "image/jpeg");
            }
            pendingCameraUri = null;
            pendingCameraFileName = null;
        });
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                uri -> { if (uri != null) handleSelectedDocument(uri, DocumentSource.GALLERY); });
        pdfLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                uri -> { if (uri != null) handleSelectedDocument(uri, DocumentSource.PDF); });
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pendingCameraUri != null) outState.putString("pendingCameraUri", pendingCameraUri.toString());
        outState.putString("pendingCameraFileName", pendingCameraFileName);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        root = view;
        view.findViewById(R.id.buttonLogout).setOnClickListener(v -> {
            AuthService.getInstance().logout();
            com.example.funkyeventapp.ui.AuthenticatedHeader.openLogin(v);
        });
        User current = AuthService.getInstance().getCurrentUser();
        if (current == null) { com.example.funkyeventapp.ui.AuthenticatedHeader.openLogin(view); return; }
        String requestedCashboxId = getArguments() == null ? null : getArguments().getString("cashboxId");
        if (requestedCashboxId != null && !requestedCashboxId.equals(current.getId())) {
            Toast.makeText(requireContext(), R.string.access_denied, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack(); return;
        }
        ((TextView) view.findViewById(R.id.textCashboxOwner)).setText(getString(R.string.cashbox_owner,
                current.getFullName(), Currency.EUR.name()));
        adapter = new CashboxTransactionAdapter(new CashboxTransactionAdapter.Listener() {
            @Override public void onReceipt(CashboxTransaction item) { openReceipt(item); }
            @Override public void onEdit(CashboxTransaction item) { loadEditData(item); }
            @Override public void onDelete(CashboxTransaction item) { confirmDelete(item); }
        });
        RecyclerView list = view.findViewById(R.id.recyclerCashbox);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
        view.findViewById(R.id.buttonCashboxBack).setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
        view.findViewById(R.id.buttonCashboxAdd).setOnClickListener(v -> loadEventsAndShowExpenseDialog());
        view.findViewById(R.id.textCashboxReceived).setOnClickListener(v -> setEntryFilter(TransactionType.INCOME));
        view.findViewById(R.id.textCashboxSpent).setOnClickListener(v -> setEntryFilter(TransactionType.EXPENSE));
        view.findViewById(R.id.textCashboxBalance).setOnClickListener(v -> setEntryFilter(null));
        view.findViewById(R.id.textCashboxEntriesTitle).setOnClickListener(v -> setEntryFilter(null));
        view.findViewById(R.id.buttonCashboxCamera).setOnClickListener(v -> launchCamera());
        view.findViewById(R.id.buttonCashboxGallery).setOnClickListener(v ->
                galleryLauncher.launch(new String[]{"image/*"}));
        view.findViewById(R.id.buttonCashboxPdf).setOnClickListener(v ->
                pdfLauncher.launch(new String[]{"application/pdf"}));
        refreshCashbox();
    }

    @Override public void onResume() { super.onResume(); if (cashbox != null && adapter != null) refreshCashbox(); }

    private void openReceiptReview(DocumentSource source, Uri uri, String fileName, String mimeType) {
        Bundle args = new Bundle();
        args.putString("source", source.name());
        args.putString("receiptUri", uri.toString());
        args.putString("fileName", fileName);
        args.putString("mimeType", mimeType);
        Navigation.findNavController(root).navigate(R.id.action_cashboxFragment_to_receiptReviewFragment, args);
    }

    private void launchCamera() {
        try {
            File directory = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Receipts");
            if (!directory.exists() && !directory.mkdirs()) throw new IOException("Cannot create receipt directory");
            pendingCameraFileName = "receipt_" + System.currentTimeMillis() + ".jpg";
            File photo = new File(directory, pendingCameraFileName);
            pendingCameraUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", photo);
            cameraLauncher.launch(pendingCameraUri);
        } catch (Exception exception) {
            pendingCameraUri = null; pendingCameraFileName = null;
            Toast.makeText(requireContext(), R.string.document_picker_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSelectedDocument(Uri uri, DocumentSource source) {
        try {
            requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) { }
        String fileName = queryDisplayName(uri);
        String mimeType = source == DocumentSource.PDF ? "application/pdf" : requireContext().getContentResolver().getType(uri);
        if (mimeType == null) mimeType = "image/*";
        openReceiptReview(source, uri, fileName, mimeType);
    }

    private String queryDisplayName(Uri uri) {
        try (Cursor cursor = requireContext().getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) return cursor.getString(column);
            }
        } catch (Exception ignored) { }
        String last = uri.getLastPathSegment();
        return last == null ? "receipt_document" : last;
    }

    private void showReceiptDetails(CashboxTransaction transaction) {
        showReadOnlyMessage();
    }

    private void openReceipt(CashboxTransaction transaction) {
        if (transaction.getReceiptId() == null || transaction.getReceiptId().trim().isEmpty()) return;
        try {
            Uri uri = Uri.parse(transaction.getReceiptId());
            String mimeType = requireContext().getContentResolver().getType(uri);
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, mimeType == null ? "*/*" : mimeType)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception error) {
            Toast.makeText(requireContext(), R.string.receipt_read_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshCashbox() {
        cashboxRepository.getCashboxForCurrentUser(new CashboxRepository.Callback<CashboxRepository.CashboxData>() {
            @Override public void onSuccess(CashboxRepository.CashboxData data) {
                if (!isAdded() || getView() != root) return;
                cashbox = data.getCashbox();
                List<CashboxTransaction> transactions = data.getTransactions();
                BigDecimal received = cashbox.getReceivedAmount();
                BigDecimal spent = BigDecimal.ZERO;
                for (CashboxTransaction transaction : transactions) {
                    if (transaction.getTransactionType() == TransactionType.EXPENSE) {
                        spent = spent.add(transaction.getAmountInEur());
                    }
                }
                BigDecimal balance = received.subtract(spent);
                Currency displayCurrency = cashbox.getDisplayCurrency() == null ? Currency.EUR : cashbox.getDisplayCurrency();
                User current = AuthService.getInstance().getCurrentUser();
                String ownerName = current == null ? "" : current.getFullName();
                ((TextView) root.findViewById(R.id.textCashboxOwner)).setText(getString(
                        R.string.cashbox_owner, ownerName, displayCurrency.name()));
                ((TextView) root.findViewById(R.id.textCashboxReceived)).setText(
                        getString(R.string.received_value, money.format(received)));
                ((TextView) root.findViewById(R.id.textCashboxSpent)).setText(
                        getString(R.string.spent_value, money.format(spent)));
                TextView balanceView = root.findViewById(R.id.textCashboxBalance);
                balanceView.setText(getString(R.string.balance_value, money.format(balance)));
                balanceView.setTextColor(requireContext().getColor(balance.signum() < 0
                        ? R.color.funky_expense : R.color.funky_completed_text));
                cashboxEntries.clear();
                if (received.signum() > 0) {
                    LocalDate receivedDate = cashbox.getCreatedAt() == null
                            ? LocalDate.now() : cashbox.getCreatedAt().toLocalDate();
                    cashboxEntries.add(new CashboxTransaction("received_total", cashbox.getId(),
                            getString(R.string.received_total_entry), "", received, Currency.EUR,
                            BigDecimal.ONE, received, receivedDate, TransactionType.INCOME,
                            ExpensePurpose.GENERAL, null, null));
                }
                cashboxEntries.addAll(transactions);
                cashboxEntries.sort((first, second) -> {
                    LocalDate firstDate = first.getDate() == null ? LocalDate.MIN : first.getDate();
                    LocalDate secondDate = second.getDate() == null ? LocalDate.MIN : second.getDate();
                    return secondDate.compareTo(firstDate);
                });
                applyEntryFilter();
                loadEventNames();
            }

            @Override public void onError(@NonNull Exception error) {
                if (!isAdded() || getView() != root) return;
                Toast.makeText(requireContext(), R.string.cashbox_load_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadEventNames() {
        eventRepository.getAllEvents(new EventRepository.Callback<List<Event>>() {
            @Override public void onSuccess(List<Event> events) {
                if (!isAdded() || getView() != root) return;
                adapter.submitEventNames(events);
            }

            @Override public void onError(@NonNull Exception error) {
                // Cashbox data remains usable; unresolved event references show the existing fallback.
            }
        });
    }

    private void setEntryFilter(@Nullable TransactionType filter) {
        entryFilter = filter;
        applyEntryFilter();
    }

    private void applyEntryFilter() {
        if (adapter == null || root == null) return;
        List<CashboxTransaction> visibleEntries = new ArrayList<>();
        for (CashboxTransaction transaction : cashboxEntries) {
            if (entryFilter == null || transaction.getTransactionType() == entryFilter) {
                visibleEntries.add(transaction);
            }
        }
        int title = entryFilter == TransactionType.INCOME
                ? R.string.received_entries
                : entryFilter == TransactionType.EXPENSE
                ? R.string.spent_entries : R.string.all_entries;
        ((TextView) root.findViewById(R.id.textCashboxEntriesTitle)).setText(
                getString(title, visibleEntries.size()));
        adapter.submitList(visibleEntries);
    }

    private void showReceivedAmountDialog() {
        if (cashbox == null) return;
        EditText amountInput = new EditText(requireContext());
        amountInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amountInput.setHint(R.string.received_amount_hint);
        amountInput.setText(cashbox.getReceivedAmount().stripTrailingZeros().toPlainString());
        amountInput.setSelectAllOnFocus(true);
        int horizontalPadding = (int) (24 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(requireContext());
        container.setPadding(horizontalPadding, 0, horizontalPadding, 0);
        container.addView(amountInput, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.received_amount_title)
                .setView(container)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    try {
                        String value = amountInput.getText() == null ? "" : amountInput.getText().toString().trim();
                        BigDecimal receivedAmount = new BigDecimal(value.replace(',', '.'));
                        if (receivedAmount.signum() < 0) throw new NumberFormatException();
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        cashboxRepository.saveReceivedAmount(receivedAmount, new CashboxRepository.Callback<Void>() {
                            @Override public void onSuccess(Void ignored) {
                                if (!isAdded() || getView() != root) return;
                                dialog.dismiss();
                                Toast.makeText(requireContext(), R.string.received_amount_saved, Toast.LENGTH_SHORT).show();
                                refreshCashbox();
                            }

                            @Override public void onError(@NonNull Exception error) {
                                if (!isAdded() || getView() != root) return;
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                showSaveError(error, R.string.received_amount_save_failed);
                            }
                        });
                    } catch (NumberFormatException error) {
                        amountInput.setError(getString(R.string.invalid_received_amount));
                    }
                }));
        dialog.show();
    }

    private void showReadOnlyMessage() {
        Toast.makeText(requireContext(), R.string.cashbox_changes_not_available, Toast.LENGTH_SHORT).show();
    }

    private void loadEventsAndShowExpenseDialog() {
        eventRepository.getAllEvents(new EventRepository.Callback<List<Event>>() {
            @Override public void onSuccess(List<Event> events) {
                if (!isAdded() || getView() != root) return;
                showExpenseDialog(events, null, new ArrayList<>());
            }

            @Override public void onError(@NonNull Exception error) {
                if (!isAdded() || getView() != root) return;
                Toast.makeText(requireContext(), R.string.events_load_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadEditData(@NonNull CashboxTransaction expense) {
        if (expense.getTransactionType() != TransactionType.EXPENSE) return;
        eventRepository.getAllEvents(new EventRepository.Callback<List<Event>>() {
            @Override public void onSuccess(List<Event> events) {
                if (!isAdded() || getView() != root) return;
                budgetRepository.getAllBudgetCategories(new BudgetRepository.Callback<List<BudgetCategory>>() {
                    @Override public void onSuccess(List<BudgetCategory> categories) {
                        if (!isAdded() || getView() != root) return;
                        showExpenseDialog(events, expense, categories);
                    }

                    @Override public void onError(@NonNull Exception error) {
                        if (!isAdded() || getView() != root) return;
                        Toast.makeText(requireContext(), R.string.budget_load_error, Toast.LENGTH_SHORT).show();
                        showExpenseDialog(events, expense, new ArrayList<>());
                    }
                });
            }

            @Override public void onError(@NonNull Exception error) {
                if (!isAdded() || getView() != root) return;
                Toast.makeText(requireContext(), R.string.events_load_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showExpenseDialog(List<Event> assignedEvents,
                                   @Nullable CashboxTransaction editingExpense,
                                   List<BudgetCategory> categories) {
        boolean editing = editingExpense != null;
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_cashbox_entry, null, false);
        ((TextView) form.findViewById(R.id.textCashboxEntryDialogTitle)).setText(
                editing ? R.string.edit_cashbox_entry : R.string.add_cashbox_entry);
        form.findViewById(R.id.layoutEntryType).setVisibility(editing ? View.GONE : View.VISIBLE);
        MaterialButton received = form.findViewById(R.id.buttonEntryReceived);
        MaterialButton spent = form.findViewById(R.id.buttonEntrySpent);
        TextInputEditText amount = form.findViewById(R.id.inputEntryAmount);
        TextInputEditText rateInput = form.findViewById(R.id.inputEntryExchangeRate);
        TextInputEditText eurInput = form.findViewById(R.id.inputEntryEurAmount);
        TextInputEditText date = form.findViewById(R.id.inputEntryDate);
        TextInputEditText description = form.findViewById(R.id.inputEntryDescription);
        AutoCompleteTextView currency = form.findViewById(R.id.inputEntryCurrency);
        AutoCompleteTextView eventInput = form.findViewById(R.id.inputEntryEvent);
        AutoCompleteTextView categoryInput = form.findViewById(R.id.inputEntryCategory);
        TextInputLayout categoryLayout = form.findViewById(R.id.layoutEntryCategory);
        categoryLayout.setVisibility(editing ? View.VISIBLE : View.GONE);
        TransactionType[] selectedType = {TransactionType.EXPENSE};
        LocalDate[] selectedDate = {editing ? editingExpense.getDate() : LocalDate.now()};
        Currency[] currencies = Currency.values();
        currency.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, currencies));
        currency.setOnClickListener(v -> currency.showDropDown());
        Currency initialCurrency = editing ? editingExpense.getCurrency() : Currency.RSD;
        currency.setText(initialCurrency.name(), false);
        List<String> eventLabels = new ArrayList<>();
        List<String> eventIds = new ArrayList<>();
        eventLabels.add(getString(R.string.general_expenses));
        eventIds.add(null);
        for (Event event : assignedEvents) {
            eventLabels.add(event.getName());
            eventIds.add(event.getId());
        }
        int initialEventPosition = 0;
        if (editing && editingExpense.getEventId() != null) {
            for (int index = 1; index < eventIds.size(); index++) {
                if (editingExpense.getEventId().equals(eventIds.get(index))) {
                    initialEventPosition = index;
                    break;
                }
            }
            if (initialEventPosition == 0) {
                eventLabels.add(editingExpense.getEventId());
                eventIds.add(editingExpense.getEventId());
                initialEventPosition = eventIds.size() - 1;
            }
        }
        eventInput.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, eventLabels));
        eventInput.setOnClickListener(v -> eventInput.showDropDown());
        String[] selectedEventId = {eventIds.get(initialEventPosition)};
        eventInput.setOnItemClickListener((parent, selectedView, position, id) ->
                selectedEventId[0] = eventIds.get(position));
        eventInput.setText(eventLabels.get(initialEventPosition), false);

        List<String> categoryLabels = new ArrayList<>();
        List<String> categoryIds = new ArrayList<>();
        categoryLabels.add(getString(R.string.uncategorized));
        categoryIds.add(null);
        int initialCategoryPosition = 0;
        for (BudgetCategory category : categories) {
            categoryLabels.add(category.getName());
            categoryIds.add(category.getId());
            if (editing && category.getId().equals(editingExpense.getCategoryId())) {
                initialCategoryPosition = categoryLabels.size() - 1;
            }
        }
        if (editing && editingExpense.getCategoryId() != null && initialCategoryPosition == 0) {
            categoryLabels.add(editingExpense.getCategoryId());
            categoryIds.add(editingExpense.getCategoryId());
            initialCategoryPosition = categoryLabels.size() - 1;
        }
        String[] selectedCategoryId = {categoryIds.get(initialCategoryPosition)};
        categoryInput.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, categoryLabels));
        categoryInput.setText(categoryLabels.get(initialCategoryPosition), false);
        categoryInput.setOnClickListener(v -> categoryInput.showDropDown());
        categoryInput.setOnItemClickListener((parent, selectedView, position, id) ->
                selectedCategoryId[0] = categoryIds.get(position));

        rateInput.setText((editing ? editingExpense.getExchangeRate()
                : exchangeRate(initialCurrency)).toPlainString());
        if (editing) {
            amount.setText(editingExpense.getAmount().stripTrailingZeros().toPlainString());
            eurInput.setText(editingExpense.getAmountInEur().stripTrailingZeros().toPlainString());
            description.setText(editingExpense.getDescription());
        }
        boolean[] syncingEur = {false};
        boolean[] eurManuallyEdited = {false};
        TextWatcher calculationWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable editable) {
                if (eurManuallyEdited[0]) return;
                try {
                    BigDecimal value = new BigDecimal(text(amount).replace(',', '.'));
                    BigDecimal rate = new BigDecimal(text(rateInput).replace(',', '.'));
                    if (rate.signum() <= 0) return;
                    syncingEur[0] = true;
                    eurInput.setText(value.divide(rate, 2, RoundingMode.HALF_UP).toPlainString());
                    eurInput.setSelection(eurInput.length());
                } catch (Exception ignored) { } finally { syncingEur[0] = false; }
            }
        };
        amount.addTextChangedListener(calculationWatcher);
        rateInput.addTextChangedListener(calculationWatcher);
        eurInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable editable) { if (!syncingEur[0]) eurManuallyEdited[0] = true; }
        });
        date.setText(selectedDate[0].format(inputDate));
        date.setOnClickListener(v -> new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
            selectedDate[0] = LocalDate.of(year, month + 1, day);
            date.setText(selectedDate[0].format(inputDate));
        }, selectedDate[0].getYear(), selectedDate[0].getMonthValue() - 1, selectedDate[0].getDayOfMonth()).show());
        received.setOnClickListener(v -> {
            selectedType[0] = TransactionType.INCOME;
            eventInput.setEnabled(false);
            styleTypeButtons(received, spent, true);
        });
        spent.setOnClickListener(v -> {
            selectedType[0] = TransactionType.EXPENSE;
            eventInput.setEnabled(true);
            styleTypeButtons(received, spent, false);
        });
        styleTypeButtons(received, spent, false);
        currency.setOnItemClickListener((parent, view, position, id) -> {
            BigDecimal newRate = exchangeRate(currencies[position]);
            rateInput.setText(newRate.toPlainString());
            try { eurInput.setText(new BigDecimal(text(amount).replace(',', '.')).divide(newRate, 2, RoundingMode.HALF_UP).toPlainString()); }
            catch (Exception ignored) { }
        });
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext()).setView(form).create();
        MaterialButton saveButton = form.findViewById(R.id.buttonSaveCashboxEntry);
        saveButton.setOnClickListener(v -> {
            try {
                String entryDescription = text(description);
                BigDecimal originalAmount = new BigDecimal(text(amount).replace(',', '.'));
                if (originalAmount.signum() <= 0
                        || (selectedType[0] == TransactionType.EXPENSE && entryDescription.isEmpty())) {
                    throw new NumberFormatException();
                }
                Currency selectedCurrency = Currency.valueOf(currency.getText().toString());
                BigDecimal rate = new BigDecimal(text(rateInput).replace(',', '.'));
                if (rate.signum() <= 0) throw new NumberFormatException();
                BigDecimal eur = text(eurInput).isEmpty() ? originalAmount.divide(rate, 2, RoundingMode.HALF_UP)
                        : new BigDecimal(text(eurInput).replace(',', '.'));
                if (eur.signum() <= 0) throw new NumberFormatException();
                if (selectedType[0] == TransactionType.INCOME) {
                    saveButton.setEnabled(false);
                    saveButton.setText(R.string.cashbox_saving);
                    BigDecimal newReceivedAmount = cashbox.getReceivedAmount().add(eur);
                    cashboxRepository.saveReceivedAmount(newReceivedAmount, new CashboxRepository.Callback<Void>() {
                        @Override public void onSuccess(Void ignored) {
                            if (!isAdded() || getView() != root) return;
                            dialog.dismiss();
                            Toast.makeText(requireContext(), R.string.received_amount_saved, Toast.LENGTH_SHORT).show();
                            refreshCashbox();
                        }

                            @Override public void onError(@NonNull Exception error) {
                                if (!isAdded() || getView() != root) return;
                                saveButton.setEnabled(true);
                                saveButton.setText(R.string.save);
                                showSaveError(error, R.string.received_amount_save_failed);
                        }
                    });
                    return;
                }
                String eventId = selectedEventId[0];
                CashboxTransaction saved = new CashboxTransaction(
                        editing ? editingExpense.getId() : null,
                        cashbox.getId(), entryDescription, entryDescription, originalAmount,
                        selectedCurrency, rate, eur, selectedDate[0], selectedType[0],
                        eventId == null ? ExpensePurpose.GENERAL : ExpensePurpose.EVENT,
                        eventId, editing ? editingExpense.getReceiptId() : null);
                saved.setCategoryId(editing ? selectedCategoryId[0] : null);
                saveButton.setEnabled(false);
                saveButton.setText(R.string.cashbox_saving);
                CashboxRepository.Callback<Void> saveCallback = new CashboxRepository.Callback<Void>() {
                    @Override public void onSuccess(Void ignored) {
                        if (!isAdded() || getView() != root) return;
                        dialog.dismiss();
                        Toast.makeText(requireContext(), editing
                                ? R.string.cashbox_entry_updated : R.string.cashbox_expense_saved,
                                Toast.LENGTH_SHORT).show();
                        refreshCashbox();
                    }

                    @Override public void onError(@NonNull Exception error) {
                        if (!isAdded() || getView() != root) return;
                        saveButton.setEnabled(true);
                        saveButton.setText(R.string.save);
                        showSaveError(error, R.string.cashbox_expense_save_failed);
                    }
                };
                if (editing) {
                    cashboxRepository.updateExpense(editingExpense.getEventId(), saved, saveCallback);
                } else {
                    cashboxRepository.createExpense(saved, saveCallback);
                }
            } catch (Exception exception) { Toast.makeText(requireContext(), R.string.invalid_cashbox_entry, Toast.LENGTH_SHORT).show(); }
        });
        dialog.show();
    }

    private void confirmDelete(CashboxTransaction transaction) {
        if (transaction.getTransactionType() != TransactionType.EXPENSE) return;
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete)
                .setMessage(R.string.delete_transaction_question)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    android.widget.Button deleteButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    deleteButton.setEnabled(false);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                    deleteButton.setText(R.string.cashbox_deleting);
                    cashboxRepository.deleteExpense(transaction, new CashboxRepository.Callback<Void>() {
                        @Override public void onSuccess(Void ignored) {
                            deleteReceiptFromStorage(transaction.getReceiptId());
                            if (!isAdded() || getView() != root) return;
                            dialog.dismiss();
                            Toast.makeText(requireContext(), R.string.cashbox_entry_deleted,
                                    Toast.LENGTH_SHORT).show();
                            refreshCashbox();
                        }

                        @Override public void onError(@NonNull Exception error) {
                            if (!isAdded() || getView() != root) return;
                            deleteButton.setEnabled(true);
                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                            deleteButton.setText(R.string.delete);
                            showSaveError(error, R.string.cashbox_expense_delete_failed);
                        }
                    });
                }));
        dialog.show();
    }

    private void deleteReceiptFromStorage(@Nullable String receiptUrl) {
        if (receiptUrl == null || receiptUrl.trim().isEmpty()) return;
        String normalized = receiptUrl.trim().toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("gs://")
                && !normalized.contains("firebasestorage.googleapis.com")
                && !normalized.contains("storage.googleapis.com")) {
            return;
        }
        try {
            FirebaseStorage.getInstance().getReferenceFromUrl(receiptUrl).delete()
                    .addOnFailureListener(error -> {
                        Log.w(TAG, "Cashbox expense deleted, but receipt cleanup failed", error);
                        if (isAdded() && getView() == root) {
                            Toast.makeText(requireContext(), R.string.receipt_delete_failed,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        } catch (IllegalArgumentException error) {
            Log.w(TAG, "Invalid Firebase Storage receipt URL", error);
        }
    }

    private void styleTypeButtons(MaterialButton received, MaterialButton spent, boolean income) {
        received.setBackgroundTintList(requireContext().getColorStateList(income ? R.color.funky_completed : R.color.funky_surface));
        received.setStrokeColorResource(income ? R.color.funky_mint : R.color.funky_border);
        received.setTextColor(requireContext().getColor(income ? R.color.funky_completed_text : R.color.funky_text_secondary));
        spent.setBackgroundTintList(requireContext().getColorStateList(income ? R.color.funky_surface : R.color.funky_expense_soft));
        spent.setStrokeColorResource(income ? R.color.funky_border : R.color.funky_expense);
        spent.setTextColor(requireContext().getColor(income ? R.color.funky_text_secondary : R.color.funky_expense));
    }

    private BigDecimal exchangeRate(Currency currency) {
        switch (currency) {
            case RSD: return new BigDecimal("117.20");
            case AED: return new BigDecimal("3.97");
            case USD: return new BigDecimal("1.08");
            default: return BigDecimal.ONE;
        }
    }

    private void showSaveError(Exception error, int fallbackMessage) {
        boolean permissionDenied = error instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) error).getCode()
                == FirebaseFirestoreException.Code.PERMISSION_DENIED;
        Toast.makeText(requireContext(), permissionDenied
                ? R.string.cashbox_rules_blocked : fallbackMessage, Toast.LENGTH_LONG).show();
    }

    private String text(TextInputEditText input) { return input.getText() == null ? "" : input.getText().toString().trim(); }
    private void coming(View view) {
        String label = view.getContentDescription() != null ? view.getContentDescription().toString()
                : view instanceof TextView ? ((TextView) view).getText().toString() : getString(R.string.cashbox);
        Toast.makeText(requireContext(), getString(R.string.coming_later, label), Toast.LENGTH_SHORT).show();
    }
}
