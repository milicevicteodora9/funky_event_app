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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.example.funkyeventapp.models.Currency;
import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.models.ExpensePurpose;
import com.example.funkyeventapp.models.DocumentSource;
import com.example.funkyeventapp.models.Receipt;
import com.example.funkyeventapp.models.ScannedDocument;
import com.example.funkyeventapp.models.TransactionType;
import com.example.funkyeventapp.repositories.MockDataRepository;
import com.example.funkyeventapp.models.User;
import com.example.funkyeventapp.services.AuthService;
import com.example.funkyeventapp.services.AuthorizationService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
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
    private final MockDataRepository repository = MockDataRepository.getInstance();
    private final DecimalFormat money = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.GERMANY));
    private final DateTimeFormatter inputDate = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private Cashbox cashbox;
    private View root;
    private CashboxTransactionAdapter adapter;
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
                saveSelectedDocumentAndOpenReview(pendingCameraUri, pendingCameraFileName, "image/jpeg", DocumentSource.CAMERA);
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
        User current = AuthService.getInstance().getCurrentUser();
        if (current == null) { com.example.funkyeventapp.ui.AuthenticatedHeader.openLogin(view); return; }
        String requestedCashboxId = getArguments() == null ? null : getArguments().getString("cashboxId");
        cashbox = requestedCashboxId == null ? repository.getCashboxForUser(current.getId()) : repository.getCashboxById(requestedCashboxId);
        boolean ownCashbox = cashbox != null && current.getId().equals(cashbox.getUserId());
        if (cashbox != null && !ownCashbox && !AuthorizationService.canAccessUserManagement(current)) {
            Toast.makeText(requireContext(), R.string.access_denied, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack(); return;
        }
        if (cashbox == null) {
            Toast.makeText(requireContext(), R.string.cashbox_not_found, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack();
            return;
        }
        User owner = cashbox.getUserId() == null ? null : repository.getUserById(cashbox.getUserId());
        String ownerName = owner == null ? getString(R.string.general_expenses) : owner.getFullName();
        ((TextView) view.findViewById(R.id.textCashboxOwner)).setText(getString(R.string.cashbox_owner,
                ownerName, cashbox.getDisplayCurrency().name()));
        view.findViewById(R.id.buttonLogout).setOnClickListener(v -> {
            AuthService.getInstance().logout();
            com.example.funkyeventapp.ui.AuthenticatedHeader.openLogin(v);
        });
        adapter = new CashboxTransactionAdapter(new CashboxTransactionAdapter.Listener() {
            @Override public void onReceipt(CashboxTransaction item) { showReceiptDetails(item); }
            @Override public void onEdit(CashboxTransaction item) { showEntryDialog(item); }
            @Override public void onDelete(CashboxTransaction item) { confirmDelete(item); }
        });
        RecyclerView list = view.findViewById(R.id.recyclerCashbox);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
        view.findViewById(R.id.buttonCashboxBack).setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
        view.findViewById(R.id.buttonCashboxAdd).setOnClickListener(v -> showEntryDialog(null));
        view.findViewById(R.id.buttonCashboxCamera).setOnClickListener(v -> launchCamera());
        view.findViewById(R.id.buttonCashboxGallery).setOnClickListener(v -> galleryLauncher.launch(new String[]{"image/*"}));
        view.findViewById(R.id.buttonCashboxPdf).setOnClickListener(v -> pdfLauncher.launch(new String[]{"application/pdf"}));
        refreshCashbox();
    }

    @Override public void onResume() { super.onResume(); if (cashbox != null && adapter != null) refreshCashbox(); }

    private void openReceiptReview(DocumentSource source, String documentId) {
        Bundle args = new Bundle(); args.putString("source", source.name()); args.putString("documentId", documentId);
        args.putString("cashboxId", cashbox.getId());
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
        saveSelectedDocumentAndOpenReview(uri, fileName, mimeType, source);
    }

    private void saveSelectedDocumentAndOpenReview(Uri uri, String fileName, String mimeType, DocumentSource source) {
        ScannedDocument document = new ScannedDocument(null, fileName, uri.toString(), mimeType, source, LocalDateTime.now());
        repository.saveScannedDocument(document);
        openReceiptReview(source, document.getId());
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
        Receipt receipt = repository.getReceiptById(transaction.getReceiptId());
        if (receipt == null) return;
        String details = getString(R.string.receipt_details, receipt.getSeller(), receipt.getSellerTaxId(),
                receipt.getReceiptNumber(), receipt.getIssueDate().format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)),
                money.format(receipt.getTotalAmount()), receipt.getCurrency().name(), receipt.getProcessingStatus().name());
        new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.receipt_review).setMessage(details)
                .setPositiveButton(android.R.string.ok, null).show();
    }

    private void refreshCashbox() {
        List<CashboxTransaction> transactions = repository.getCashboxTransactions(cashbox.getId());
        ((TextView) root.findViewById(R.id.textCashboxReceived)).setText(getString(R.string.received_value, money.format(repository.getCashboxTotal(cashbox.getId(), TransactionType.INCOME))));
        ((TextView) root.findViewById(R.id.textCashboxSpent)).setText(getString(R.string.spent_value, money.format(repository.getCashboxTotal(cashbox.getId(), TransactionType.EXPENSE))));
        BigDecimal balance = repository.getCashboxBalance(cashbox.getId());
        TextView balanceView = root.findViewById(R.id.textCashboxBalance);
        balanceView.setText(getString(R.string.balance_value, money.format(balance)));
        balanceView.setTextColor(requireContext().getColor(balance.signum() < 0 ? R.color.funky_expense : R.color.funky_completed_text));
        ((TextView) root.findViewById(R.id.textCashboxEntriesTitle)).setText(getString(R.string.all_entries, transactions.size()));
        adapter.submitList(transactions);
    }

    private void showEntryDialog(@Nullable CashboxTransaction existing) {
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_cashbox_entry, null, false);
        ((TextView) form.findViewById(R.id.textCashboxEntryDialogTitle)).setText(
                existing == null ? R.string.add_cashbox_entry : R.string.edit_cashbox_entry);
        MaterialButton received = form.findViewById(R.id.buttonEntryReceived);
        MaterialButton spent = form.findViewById(R.id.buttonEntrySpent);
        TextInputEditText amount = form.findViewById(R.id.inputEntryAmount);
        TextInputEditText rateInput = form.findViewById(R.id.inputEntryExchangeRate);
        TextInputEditText eurInput = form.findViewById(R.id.inputEntryEurAmount);
        TextInputEditText date = form.findViewById(R.id.inputEntryDate);
        TextInputEditText description = form.findViewById(R.id.inputEntryDescription);
        AutoCompleteTextView currency = form.findViewById(R.id.inputEntryCurrency);
        AutoCompleteTextView eventInput = form.findViewById(R.id.inputEntryEvent);
        TransactionType[] selectedType = {existing == null ? TransactionType.INCOME : existing.getTransactionType()};
        LocalDate[] selectedDate = {existing == null ? LocalDate.now() : existing.getDate()};
        Currency[] currencies = Currency.values();
        currency.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, currencies));
        currency.setOnClickListener(v -> currency.showDropDown());
        Currency initialCurrency = existing == null ? Currency.RSD : existing.getCurrency();
        currency.setText(initialCurrency.name(), false);
        List<Event> assignedEvents = repository.getAllEvents();
        List<String> eventLabels = new ArrayList<>();
        eventLabels.add(getString(R.string.general_expenses));
        for (Event event : assignedEvents) eventLabels.add(event.getName());
        eventInput.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, eventLabels));
        eventInput.setOnClickListener(v -> eventInput.showDropDown());
        int selectedEventIndex = 0;
        if (existing != null && existing.getEventId() != null)
            for (int i = 0; i < assignedEvents.size(); i++) if (existing.getEventId().equals(assignedEvents.get(i).getId())) selectedEventIndex = i + 1;
        eventInput.setText(eventLabels.get(selectedEventIndex), false);
        if (existing != null) {
            amount.setText(existing.getAmount().toPlainString());
            rateInput.setText(existing.getExchangeRate().toPlainString());
            eurInput.setText(existing.getAmountInEur().toPlainString());
            description.setText(existing.getDescription() == null || existing.getDescription().trim().isEmpty()
                    ? existing.getName() : existing.getDescription());
        } else {
            rateInput.setText(exchangeRate(initialCurrency).toPlainString());
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
        received.setOnClickListener(v -> { selectedType[0] = TransactionType.INCOME; styleTypeButtons(received, spent, true); });
        spent.setOnClickListener(v -> { selectedType[0] = TransactionType.EXPENSE; styleTypeButtons(received, spent, false); });
        styleTypeButtons(received, spent, selectedType[0] == TransactionType.INCOME);
        currency.setOnItemClickListener((parent, view, position, id) -> {
            BigDecimal newRate = exchangeRate(currencies[position]);
            rateInput.setText(newRate.toPlainString());
            try { eurInput.setText(new BigDecimal(text(amount).replace(',', '.')).divide(newRate, 2, RoundingMode.HALF_UP).toPlainString()); }
            catch (Exception ignored) { }
        });
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext()).setView(form).create();
        form.findViewById(R.id.buttonSaveCashboxEntry).setOnClickListener(v -> {
            try {
                String entryDescription = text(description);
                BigDecimal originalAmount = new BigDecimal(text(amount).replace(',', '.'));
                if (entryDescription.isEmpty() || originalAmount.signum() <= 0) throw new NumberFormatException();
                Currency selectedCurrency = Currency.valueOf(currency.getText().toString());
                BigDecimal rate = new BigDecimal(text(rateInput).replace(',', '.'));
                if (rate.signum() <= 0) throw new NumberFormatException();
                BigDecimal eur = text(eurInput).isEmpty() ? originalAmount.divide(rate, 2, RoundingMode.HALF_UP)
                        : new BigDecimal(text(eurInput).replace(',', '.'));
                if (eur.signum() <= 0) throw new NumberFormatException();
                int eventPosition = eventLabels.indexOf(eventInput.getText().toString());
                Event selectedEvent = eventPosition > 0 ? assignedEvents.get(eventPosition - 1) : null;
                if (selectedType[0] == TransactionType.EXPENSE && eventPosition < 0) throw new IllegalArgumentException();
                CashboxTransaction saved = new CashboxTransaction(existing == null ? null : existing.getId(), cashbox.getId(), entryDescription, entryDescription, originalAmount,
                        selectedCurrency, rate, eur, selectedDate[0], selectedType[0], selectedEvent == null ? ExpensePurpose.GENERAL : ExpensePurpose.EVENT,
                        selectedEvent == null ? null : selectedEvent.getId(), existing == null ? null : existing.getReceiptId());
                if (existing != null) saved.setCategoryId(existing.getCategoryId());
                if (existing == null) repository.addCashboxTransaction(saved); else repository.updateCashboxTransaction(saved);
                refreshCashbox();
                dialog.dismiss();
                Toast.makeText(requireContext(), existing == null ? R.string.cashbox_entry_saved : R.string.cashbox_entry_updated, Toast.LENGTH_SHORT).show();
            } catch (Exception exception) { Toast.makeText(requireContext(), R.string.invalid_cashbox_entry, Toast.LENGTH_SHORT).show(); }
        });
        dialog.show();
    }

    private void confirmDelete(CashboxTransaction transaction) {
        new MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.delete_transaction_question)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    repository.deleteCashboxTransaction(transaction.getId());
                    refreshCashbox();
                }).show();
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

    private String text(TextInputEditText input) { return input.getText() == null ? "" : input.getText().toString().trim(); }
    private void coming(View view) {
        String label = view.getContentDescription() != null ? view.getContentDescription().toString()
                : view instanceof TextView ? ((TextView) view).getText().toString() : getString(R.string.cashbox);
        Toast.makeText(requireContext(), getString(R.string.coming_later, label), Toast.LENGTH_SHORT).show();
    }
}
