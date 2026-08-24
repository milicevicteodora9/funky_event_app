package com.example.funkyeventapp.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.funkyeventapp.R;
import com.example.funkyeventapp.models.BudgetCategory;
import com.example.funkyeventapp.models.Cashbox;
import com.example.funkyeventapp.models.CashboxTransaction;
import com.example.funkyeventapp.models.Currency;
import com.example.funkyeventapp.models.DocumentSource;
import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.models.ExpensePurpose;
import com.example.funkyeventapp.models.Receipt;
import com.example.funkyeventapp.models.ScannedDocument;
import com.example.funkyeventapp.models.TransactionType;
import com.example.funkyeventapp.repositories.MockDataRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReceiptReviewFragment extends Fragment {
    private static final String USER_ID = "user_teodora";
    private final MockDataRepository repository = MockDataRepository.getInstance();
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private DocumentSource source;
    private ScannedDocument document;
    private Receipt draft;

    public ReceiptReviewFragment() { super(R.layout.fragment_receipt_review); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        try { source = DocumentSource.valueOf(requireArguments().getString("source")); }
        catch (Exception ignored) { source = DocumentSource.CAMERA; }
        document = repository.getScannedDocumentById(requireArguments().getString("documentId"));
        if (document == null) document = repository.createMockScannedDocument(source);
        else source = document.getSource();
        draft = repository.createMockReceiptDraft();
        bindSource(view);
        bindForm(view);
        view.findViewById(R.id.buttonReceiptBack).setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
    }

    private void bindSource(View view) {
        ((TextView) view.findViewById(R.id.textReceiptSource)).setText(source.name());
        ((TextView) view.findViewById(R.id.textReceiptFile)).setText(source.name() + " · " + document.getFileName());
        ImageView preview = view.findViewById(R.id.imageReceiptPreview);
        if (source == DocumentSource.PDF) {
            preview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            preview.setImageResource(R.drawable.ic_pdf);
        } else {
            try { preview.setImageURI(Uri.parse(document.getFileUri())); }
            catch (Exception ignored) { preview.setImageResource(R.drawable.ic_cashbox); preview.setScaleType(ImageView.ScaleType.CENTER_INSIDE); }
        }
    }

    private void bindForm(View view) {
        TextInputEditText seller = view.findViewById(R.id.inputReceiptSeller);
        TextInputEditText taxId = view.findViewById(R.id.inputReceiptTaxId);
        TextInputEditText number = view.findViewById(R.id.inputReceiptNumber);
        TextInputEditText date = view.findViewById(R.id.inputReceiptDate);
        TextInputEditText amount = view.findViewById(R.id.inputReceiptAmount);
        AutoCompleteTextView currency = view.findViewById(R.id.inputReceiptCurrency);
        AutoCompleteTextView eventInput = view.findViewById(R.id.inputReceiptEvent);
        AutoCompleteTextView categoryInput = view.findViewById(R.id.inputReceiptCategory);
        TextInputLayout eventLayout = view.findViewById(R.id.layoutReceiptEvent);
        MaterialButton general = view.findViewById(R.id.buttonReceiptGeneral);
        MaterialButton eventButton = view.findViewById(R.id.buttonReceiptEvent);
        ExpensePurpose[] purpose = {ExpensePurpose.GENERAL};
        LocalDate[] selectedDate = {draft.getIssueDate()};

        seller.setText(draft.getSeller()); taxId.setText(draft.getSellerTaxId()); number.setText(draft.getReceiptNumber());
        date.setText(selectedDate[0].format(dateFormat)); amount.setText(draft.getTotalAmount().toPlainString());
        currency.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, Currency.values()));
        currency.setText(draft.getCurrency().name(), false);
        List<Event> events = repository.getAssignedEventsForUser(USER_ID);
        List<String> eventNames = new ArrayList<>(); for (Event event : events) eventNames.add(event.getName());
        eventInput.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, eventNames));
        List<BudgetCategory> categories = repository.getBudgetCategories();
        List<String> categoryNames = new ArrayList<>(); for (BudgetCategory category : categories) categoryNames.add(category.getName());
        categoryInput.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames));
        if (!categoryNames.isEmpty()) categoryInput.setText(categoryNames.get(0), false);
        setPurposeStyle(general, eventButton, true);
        general.setOnClickListener(v -> { purpose[0] = ExpensePurpose.GENERAL; eventLayout.setVisibility(View.GONE); setPurposeStyle(general, eventButton, true); });
        eventButton.setOnClickListener(v -> { purpose[0] = ExpensePurpose.EVENT; eventLayout.setVisibility(View.VISIBLE); setPurposeStyle(general, eventButton, false); });
        date.setOnClickListener(v -> new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
            selectedDate[0] = LocalDate.of(year, month + 1, day); date.setText(selectedDate[0].format(dateFormat));
        }, selectedDate[0].getYear(), selectedDate[0].getMonthValue() - 1, selectedDate[0].getDayOfMonth()).show());
        view.findViewById(R.id.buttonSaveReceipt).setOnClickListener(v -> {
            try {
                BigDecimal originalAmount = new BigDecimal(text(amount).replace(',', '.'));
                Currency selectedCurrency = Currency.valueOf(currency.getText().toString());
                int eventIndex = eventNames.indexOf(eventInput.getText().toString());
                if (text(seller).isEmpty() || text(number).isEmpty() || originalAmount.signum() <= 0
                        || (purpose[0] == ExpensePurpose.EVENT && eventIndex < 0)) throw new IllegalArgumentException();
                Event selectedEvent = purpose[0] == ExpensePurpose.EVENT ? events.get(eventIndex) : null;
                draft.setSeller(text(seller)); draft.setSellerTaxId(text(taxId)); draft.setReceiptNumber(text(number));
                draft.setIssueDate(selectedDate[0]); draft.setTotalAmount(originalAmount); draft.setCurrency(selectedCurrency);
                BigDecimal rate = exchangeRate(selectedCurrency);
                Cashbox cashbox = repository.getCashboxForUser(USER_ID);
                CashboxTransaction transaction = new CashboxTransaction(null, cashbox.getId(), text(seller),
                        "Receipt " + text(number), originalAmount, selectedCurrency, rate,
                        originalAmount.divide(rate, 2, RoundingMode.HALF_UP), selectedDate[0], TransactionType.EXPENSE,
                        purpose[0], selectedEvent == null ? null : selectedEvent.getId(), null);
                int categoryIndex = categoryNames.indexOf(categoryInput.getText().toString());
                if (categoryIndex >= 0) transaction.setCategoryId(categories.get(categoryIndex).getId());
                repository.saveConfirmedReceiptExpense(document, draft, transaction);
                Toast.makeText(requireContext(), R.string.receipt_saved, Toast.LENGTH_SHORT).show();
                Navigation.findNavController(view).popBackStack();
            } catch (Exception exception) { Toast.makeText(requireContext(), R.string.invalid_receipt, Toast.LENGTH_SHORT).show(); }
        });
    }

    private void setPurposeStyle(MaterialButton general, MaterialButton event, boolean isGeneral) {
        general.setBackgroundTintList(requireContext().getColorStateList(isGeneral ? R.color.funky_mint : R.color.funky_badge));
        event.setBackgroundTintList(requireContext().getColorStateList(isGeneral ? R.color.funky_badge : R.color.funky_mint));
        general.setTextColor(requireContext().getColor(isGeneral ? R.color.white : R.color.funky_text_secondary));
        event.setTextColor(requireContext().getColor(isGeneral ? R.color.funky_text_secondary : R.color.white));
    }

    private BigDecimal exchangeRate(Currency currency) {
        switch (currency) { case RSD: return new BigDecimal("117.20"); case AED: return new BigDecimal("3.97"); case USD: return new BigDecimal("1.08"); default: return BigDecimal.ONE; }
    }
    private String text(TextInputEditText input) { return input.getText() == null ? "" : input.getText().toString().trim(); }
}
