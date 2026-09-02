package com.example.funkyeventapp.fragments;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.funkyeventapp.R;
import com.example.funkyeventapp.models.CashboxTransaction;
import com.example.funkyeventapp.models.Currency;
import com.example.funkyeventapp.models.DocumentSource;
import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.models.ExpensePurpose;
import com.example.funkyeventapp.models.Receipt;
import com.example.funkyeventapp.models.TransactionType;
import com.example.funkyeventapp.models.User;
import com.example.funkyeventapp.models.UserRole;
import com.example.funkyeventapp.repositories.CashboxRepository;
import com.example.funkyeventapp.repositories.EventRepository;
import com.example.funkyeventapp.services.AuthService;
import com.example.funkyeventapp.services.ReceiptScanProcessor;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.FirebaseStorage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReceiptReviewFragment extends Fragment {
    private final CashboxRepository cashboxRepository = CashboxRepository.getInstance();
    private final EventRepository eventRepository = EventRepository.getInstance();
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT);
    private final List<Event> events = new ArrayList<>();

    private DocumentSource source;
    private Uri receiptUri;
    private String originalFileName;
    private View root;
    private TextInputEditText sellerInput;
    private TextInputEditText taxIdInput;
    private TextInputEditText receiptNumberInput;
    private TextInputEditText dateInput;
    private TextInputEditText amountInput;
    private AutoCompleteTextView currencyInput;
    private AutoCompleteTextView eventInput;
    private MaterialButton saveButton;
    private LocalDate selectedDate;
    private int selectedEventPosition;
    private boolean processing;
    private boolean saving;

    public ReceiptReviewFragment() { super(R.layout.fragment_receipt_review); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        root = view;
        try {
            source = DocumentSource.valueOf(requireArguments().getString("source"));
            receiptUri = Uri.parse(requireArguments().getString("receiptUri"));
        } catch (Exception error) {
            Toast.makeText(requireContext(), R.string.receipt_read_error, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack();
            return;
        }
        bindSource();
        bindForm();
        loadEvents();
        runOcr();
        view.findViewById(R.id.buttonReceiptBack).setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());
    }

    private void bindSource() {
        ((TextView) root.findViewById(R.id.textReceiptSource)).setText(source.name());
        originalFileName = requireArguments().getString("fileName");
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            originalFileName = receiptUri.getLastPathSegment();
        }
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            originalFileName = source == DocumentSource.PDF ? "receipt.pdf" : "receipt.jpg";
        }
        ((TextView) root.findViewById(R.id.textReceiptFile)).setText(
                originalFileName);
        ImageView preview = root.findViewById(R.id.imageReceiptPreview);
        if (source == DocumentSource.PDF) {
            preview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            preview.setImageResource(R.drawable.ic_pdf);
        } else {
            try { preview.setImageURI(receiptUri); }
            catch (Exception error) {
                preview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                preview.setImageResource(R.drawable.ic_cashbox);
            }
        }
    }

    private void bindForm() {
        sellerInput = root.findViewById(R.id.inputReceiptSeller);
        taxIdInput = root.findViewById(R.id.inputReceiptTaxId);
        receiptNumberInput = root.findViewById(R.id.inputReceiptNumber);
        dateInput = root.findViewById(R.id.inputReceiptDate);
        amountInput = root.findViewById(R.id.inputReceiptAmount);
        currencyInput = root.findViewById(R.id.inputReceiptCurrency);
        eventInput = root.findViewById(R.id.inputReceiptEvent);
        saveButton = root.findViewById(R.id.buttonSaveReceipt);

        currencyInput.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, Currency.values()));
        currencyInput.setText(Currency.EUR.name(), false);
        currencyInput.setOnClickListener(v -> currencyInput.showDropDown());
        dateInput.setText("");
        dateInput.setOnClickListener(v -> {
            LocalDate initial = selectedDate == null ? LocalDate.now() : selectedDate;
            new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
                selectedDate = LocalDate.of(year, month + 1, day);
                dateInput.setText(selectedDate.format(dateFormat));
            }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
        });
        saveButton.setOnClickListener(v -> saveExpense());
    }

    private void loadEvents() {
        loadAvailableCashboxEvents(new EventRepository.Callback<List<Event>>() {
            @Override public void onSuccess(List<Event> loadedEvents) {
                if (!isAdded() || getView() != root) return;
                events.clear();
                events.addAll(loadedEvents);
                List<String> labels = new ArrayList<>();
                labels.add(getString(R.string.general_expenses));
                for (Event event : events) labels.add(event.getName());
                eventInput.setAdapter(new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, labels));
                eventInput.setText(labels.get(0), false);
                eventInput.setOnClickListener(v -> eventInput.showDropDown());
                eventInput.setOnItemClickListener((parent, selectedView, position, id) ->
                        selectedEventPosition = position);
            }

            @Override public void onError(@NonNull Exception error) {
                if (!isAdded() || getView() != root) return;
                List<String> generalOnly = new ArrayList<>();
                generalOnly.add(getString(R.string.general_expenses));
                eventInput.setAdapter(new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, generalOnly));
                eventInput.setText(generalOnly.get(0), false);
                Toast.makeText(requireContext(), R.string.events_load_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAvailableCashboxEvents(@NonNull EventRepository.Callback<List<Event>> callback) {
        User currentUser = AuthService.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getRole() == UserRole.COORDINATOR) {
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser == null) {
                callback.onError(new IllegalStateException("Authenticated user is required"));
                return;
            }
            eventRepository.getEventsAssignedToUser(firebaseUser.getUid(), callback);
            return;
        }
        eventRepository.getAllEvents(callback);
    }

    private void runOcr() {
        processing = true;
        updateSaveEnabled();
        ((TextView) root.findViewById(R.id.textReceiptFile)).append(
                " · " + getString(R.string.ocr_processing));
        ReceiptScanProcessor.process(requireContext(), receiptUri, source == DocumentSource.PDF,
                new ReceiptScanProcessor.Callback() {
                    @Override public void onSuccess(@NonNull ReceiptScanProcessor.Result result) {
                        if (!isAdded() || getView() != root) return;
                        processing = false;
                        Receipt receipt = result.getReceipt();
                        if (!hasRecognizedData(receipt)) {
                            Toast.makeText(requireContext(), R.string.ocr_no_text, Toast.LENGTH_LONG).show();
                        } else {
                            if (receipt.getSeller() != null && !receipt.getSeller().isEmpty()) sellerInput.setText(receipt.getSeller());
                            if (receipt.getSellerTaxId() != null) taxIdInput.setText(receipt.getSellerTaxId());
                            if (receipt.getReceiptNumber() != null) receiptNumberInput.setText(receipt.getReceiptNumber());
                            if (receipt.getTotalAmount() != null) {
                                amountInput.setText(receipt.getTotalAmount().stripTrailingZeros().toPlainString());
                            } else {
                                Toast.makeText(requireContext(), R.string.ocr_amount_not_found, Toast.LENGTH_LONG).show();
                            }
                            if (receipt.getIssueDate() != null) {
                                selectedDate = receipt.getIssueDate();
                                dateInput.setText(selectedDate.format(dateFormat));
                            }
                            if (receipt.getCurrency() != null) currencyInput.setText(receipt.getCurrency().name(), false);
                        }
                        TextView status = root.findViewById(R.id.textReceiptFile);
                        if (result.getSource() == ReceiptScanProcessor.Source.SUF_QR) {
                            status.setText(R.string.receipt_source_suf);
                        } else {
                            status.setText(getString(R.string.receipt_source_ocr,
                                    fallbackReasonText(result.getFallbackReason())));
                        }
                        updateSaveEnabled();
                    }

                    @Override public void onError(@NonNull Exception error) {
                        if (!isAdded() || getView() != root) return;
                        processing = false;
                        ((TextView) root.findViewById(R.id.textReceiptFile)).setText(getString(R.string.ocr_failed_manual_entry));
                        Toast.makeText(requireContext(), R.string.receipt_read_error, Toast.LENGTH_LONG).show();
                        updateSaveEnabled();
                    }
                });
    }

    private String fallbackReasonText(ReceiptScanProcessor.FallbackReason reason) {
        if (reason == null) return getString(R.string.qr_not_found);
        switch (reason) {
            case QR_DETECTED_NOT_READABLE: return getString(R.string.qr_not_readable);
            case NON_SUF_QR: return getString(R.string.qr_not_suf);
            case QR_SCAN_FAILED: return getString(R.string.qr_scan_failed);
            case SUF_LOOKUP_FAILED: return getString(R.string.suf_lookup_failed);
            default: return getString(R.string.qr_not_found);
        }
    }

    private boolean hasRecognizedData(Receipt receipt) {
        return receipt.getSeller() != null
                || receipt.getSellerTaxId() != null
                || receipt.getReceiptNumber() != null
                || receipt.getTotalAmount() != null
                || receipt.getIssueDate() != null
                || (receipt.getRecognizedText() != null
                && !receipt.getRecognizedText().trim().isEmpty());
    }

    private void saveExpense() {
        if (processing || saving) return;
        try {
            String description = text(sellerInput);
            BigDecimal amount = new BigDecimal(text(amountInput).replace(',', '.'));
            Currency currency = Currency.valueOf(currencyInput.getText().toString());
            if (description.isEmpty() || amount.signum() <= 0 || selectedDate == null) throw new IllegalArgumentException();
            Event selectedEvent = selectedEventPosition > 0 && selectedEventPosition <= events.size()
                    ? events.get(selectedEventPosition - 1) : null;
            BigDecimal rate = exchangeRate(currency);
            String transactionId = cashboxRepository.newExpenseId();
            CashboxTransaction transaction = new CashboxTransaction(transactionId, null, description,
                    description, amount, currency, rate, amount.divide(rate, 2, RoundingMode.HALF_UP),
                    selectedDate, TransactionType.EXPENSE,
                    selectedEvent == null ? ExpensePurpose.GENERAL : ExpensePurpose.EVENT,
                    selectedEvent == null ? null : selectedEvent.getId(), null);
            saving = true;
            updateSaveEnabled();
            uploadAndSave(transaction);
        } catch (Exception error) {
            Toast.makeText(requireContext(), R.string.invalid_receipt, Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadAndSave(@NonNull CashboxTransaction transaction) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (receiptUri == null || receiptUri.toString().trim().isEmpty()) {
            createExpense(transaction, null);
            return;
        }
        if (user == null) {
            finishSaveWithError(R.string.receipt_upload_failed);
            return;
        }

        String safeFileName = originalFileName.replace('/', '_').replace('\\', '_');
        StorageReference uploadedFile = FirebaseStorage.getInstance().getReference()
                .child("receipts")
                .child(user.getUid())
                .child(transaction.getId())
                .child(safeFileName);
        uploadedFile.putFile(receiptUri)
                .continueWithTask(upload -> {
                    if (!upload.isSuccessful()) {
                        Exception error = upload.getException();
                        throw error == null ? new IllegalStateException("Receipt upload failed") : error;
                    }
                    return uploadedFile.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    transaction.setReceiptId(downloadUri.toString());
                    createExpense(transaction, uploadedFile);
                })
                .addOnFailureListener(error -> finishSaveWithError(R.string.receipt_upload_failed));
    }

    private void createExpense(@NonNull CashboxTransaction transaction,
                               @Nullable StorageReference uploadedFile) {
        cashboxRepository.createExpense(transaction, new CashboxRepository.Callback<Void>() {
                @Override public void onSuccess(Void ignored) {
                    if (!isAdded() || getView() != root) return;
                    Toast.makeText(requireContext(), R.string.receipt_saved, Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(root).popBackStack();
                }

                @Override public void onError(@NonNull Exception error) {
                    if (uploadedFile == null) {
                        showTransactionSaveError(error);
                    } else {
                        uploadedFile.delete().addOnCompleteListener(cleanup ->
                                showTransactionSaveError(error));
                    }
                }
            });
    }

    private void showTransactionSaveError(@NonNull Exception error) {
        if (!isAdded() || getView() != root) return;
        saving = false;
        updateSaveEnabled();
        boolean permissionDenied = error instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) error).getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED;
        Toast.makeText(requireContext(), permissionDenied
                ? R.string.cashbox_rules_blocked : R.string.cashbox_expense_save_failed,
                Toast.LENGTH_LONG).show();
    }

    private void finishSaveWithError(int message) {
        if (!isAdded() || getView() != root) return;
        saving = false;
        updateSaveEnabled();
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    private void updateSaveEnabled() {
        if (saveButton != null) {
            saveButton.setEnabled(!processing && !saving);
            saveButton.setText(saving ? R.string.receipt_saving : R.string.save_receipt);
        }
    }

    private BigDecimal exchangeRate(Currency currency) {
        switch (currency) {
            case RSD: return new BigDecimal("117.20");
            case AED: return new BigDecimal("3.97");
            case USD: return new BigDecimal("1.08");
            default: return BigDecimal.ONE;
        }
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}
