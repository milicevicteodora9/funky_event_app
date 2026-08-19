package com.example.funkyeventapp.fragments;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.funkyeventapp.R;
import com.example.funkyeventapp.adapters.ClientEventAdapter;
import com.example.funkyeventapp.adapters.InvoiceAdapter;
import com.example.funkyeventapp.models.Client;
import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.models.EventStatus;
import com.example.funkyeventapp.models.Invoice;
import com.example.funkyeventapp.models.InvoiceStatus;
import com.example.funkyeventapp.repositories.MockDataRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClientDetailsFragment extends Fragment {
    private final MockDataRepository repository = MockDataRepository.getInstance();
    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.GERMANY));
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private RecyclerView history;
    private TextView empty;
    private MaterialButton eventsTab, invoicesTab;
    private MaterialButton addInvoiceButton;
    private View root;
    private Client client;
    private List<Event> events;
    private List<Invoice> invoices;

    public ClientDetailsFragment() { super(R.layout.fragment_client_details); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        root = view;
        String clientId = getArguments() == null ? null : getArguments().getString("clientId");
        client = clientId == null ? null : repository.getClientById(clientId);
        if (client == null) {
            Toast.makeText(requireContext(), R.string.client_not_found, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack();
            return;
        }
        events = repository.getEventsForClient(clientId);
        invoices = repository.getInvoicesForClient(clientId);
        history = view.findViewById(R.id.recyclerClientHistory);
        history.setLayoutManager(new LinearLayoutManager(requireContext()));
        empty = view.findViewById(R.id.textEmptyClientHistory);
        eventsTab = view.findViewById(R.id.buttonClientEvents);
        invoicesTab = view.findViewById(R.id.buttonClientInvoices);
        addInvoiceButton = view.findViewById(R.id.buttonAddInvoice);
        bindClient(view);
        bindSummary(view);
        view.findViewById(R.id.buttonBackClient).setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
        eventsTab.setOnClickListener(v -> showEvents(view));
        invoicesTab.setOnClickListener(v -> showInvoices());
        addInvoiceButton.setOnClickListener(v -> showAddInvoiceDialog());
        showEvents(view);
    }

    private void bindClient(View view) {
        ((TextView) view.findViewById(R.id.textDetailClientName)).setText(client.getName());
        ((TextView) view.findViewById(R.id.textDetailContact)).setText(client.getContactPerson() + "  •  " + client.getPhone());
        ((TextView) view.findViewById(R.id.textDetailEmail)).setText(client.getEmail());
        ((TextView) view.findViewById(R.id.textDetailAddress)).setText(client.getAddress() + "  •  PIB " + client.getTaxId());
    }

    private void bindSummary(View view) {
        int current = 0, completed = 0;
        for (Event event : events) if (event.getStatus() == EventStatus.COMPLETED) completed++; else current++;
        ((TextView) view.findViewById(R.id.textTotalEvents)).setText(getString(R.string.summary_total, events.size()));
        ((TextView) view.findViewById(R.id.textCurrentEvents)).setText(getString(R.string.summary_current, current));
        ((TextView) view.findViewById(R.id.textCompletedEvents)).setText(getString(R.string.summary_completed, completed));
        Map<String, BigDecimal> paidByCurrency = new LinkedHashMap<>();
        for (Invoice invoice : invoices) if (invoice.getStatus() == InvoiceStatus.PAID)
            paidByCurrency.put(invoice.getCurrency(), paidByCurrency.getOrDefault(invoice.getCurrency(), BigDecimal.ZERO).add(invoice.getAmount()));
        StringBuilder revenue = new StringBuilder();
        for (Map.Entry<String, BigDecimal> entry : paidByCurrency.entrySet()) {
            if (revenue.length() > 0) revenue.append(" + ");
            revenue.append(moneyFormat.format(entry.getValue())).append(" ").append(entry.getKey());
        }
        ((TextView) view.findViewById(R.id.textRevenue)).setText(getString(R.string.summary_revenue, revenue.length() == 0 ? getString(R.string.not_available) : revenue.toString()));
        ((TextView) view.findViewById(R.id.textProfit)).setText(getString(R.string.summary_profit, getString(R.string.not_available)));
    }

    private void showEvents(View root) {
        ClientEventAdapter adapter = new ClientEventAdapter(event -> {
            Bundle args = new Bundle(); args.putString("eventId", event.getId());
            Navigation.findNavController(root).navigate(R.id.action_clientDetailsFragment_to_eventDetailsFragment, args);
        });
        adapter.submitList(events); history.setAdapter(adapter);
        addInvoiceButton.setVisibility(View.GONE);
        setSelectedTab(true); showEmpty(events.isEmpty(), R.string.no_client_events);
    }

    private void showInvoices() {
        InvoiceAdapter adapter = new InvoiceAdapter(this::showInvoiceDialog);
        adapter.submitList(invoices); history.setAdapter(adapter);
        addInvoiceButton.setVisibility(View.VISIBLE);
        setSelectedTab(false); showEmpty(invoices.isEmpty(), R.string.no_client_invoices);
    }

    private void setSelectedTab(boolean eventsSelected) {
        eventsTab.setBackgroundTintList(requireContext().getColorStateList(eventsSelected ? R.color.funky_mint : R.color.funky_badge));
        invoicesTab.setBackgroundTintList(requireContext().getColorStateList(eventsSelected ? R.color.funky_badge : R.color.funky_mint));
        eventsTab.setTextColor(requireContext().getColor(eventsSelected ? R.color.white : R.color.funky_text_secondary));
        invoicesTab.setTextColor(requireContext().getColor(eventsSelected ? R.color.funky_text_secondary : R.color.white));
    }

    private void showEmpty(boolean isEmpty, int message) {
        empty.setText(message); empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        history.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showInvoiceDialog(Invoice invoice) {
        Event event = repository.getEventById(invoice.getEventId());
        String eventName = event == null ? getString(R.string.unknown_event) : event.getName();
        String details = getString(R.string.invoice_dialog_details, eventName, invoice.getIssueDate().format(dateFormat),
                invoice.getDueDate().format(dateFormat), moneyFormat.format(invoice.getAmount()), invoice.getCurrency(),
                invoice.getStatus().name(), invoice.getNotes());
        new MaterialAlertDialogBuilder(requireContext()).setTitle(invoice.getInvoiceNumber()).setMessage(details)
                .setPositiveButton(android.R.string.ok, null).show();
    }

    private void showAddInvoiceDialog() {
        if (events.isEmpty()) {
            Toast.makeText(requireContext(), R.string.invoice_requires_event, Toast.LENGTH_SHORT).show();
            return;
        }
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(padding, 0, padding, 0);

        Spinner eventSpinner = new Spinner(requireContext());
        String[] eventNames = new String[events.size()];
        for (int i = 0; i < events.size(); i++) eventNames[i] = events.get(i).getName();
        eventSpinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, eventNames));
        form.addView(eventSpinner, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        EditText number = field(R.string.invoice_number, InputType.TYPE_CLASS_TEXT);
        EditText amount = field(R.string.amount, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText currency = field(R.string.currency, InputType.TYPE_CLASS_TEXT);
        currency.setText("EUR");
        EditText notes = field(R.string.notes, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        form.addView(number); form.addView(amount); form.addView(currency); form.addView(notes);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.new_invoice).setView(form).setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.add, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                String invoiceNumber = number.getText().toString().trim();
                BigDecimal invoiceAmount = new BigDecimal(amount.getText().toString().trim().replace(',', '.'));
                if (invoiceNumber.isEmpty() || invoiceAmount.signum() <= 0) throw new NumberFormatException();
                Event selectedEvent = events.get(eventSpinner.getSelectedItemPosition());
                LocalDate issueDate = LocalDate.now();
                repository.addInvoice(new Invoice(null, selectedEvent.getId(), client.getId(), invoiceNumber,
                        issueDate, issueDate.plusDays(30), invoiceAmount, currency.getText().toString().trim().isEmpty() ? "EUR" : currency.getText().toString().trim().toUpperCase(Locale.ROOT),
                        InvoiceStatus.DRAFT, "", notes.getText().toString().trim()));
                invoices = repository.getInvoicesForClient(client.getId());
                bindSummary(root); showInvoices(); dialog.dismiss();
                Toast.makeText(requireContext(), R.string.invoice_added, Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException exception) {
                Toast.makeText(requireContext(), R.string.invalid_invoice, Toast.LENGTH_SHORT).show();
            }
        }));
        dialog.show();
    }

    private EditText field(int hint, int inputType) {
        EditText field = new EditText(requireContext());
        field.setHint(hint); field.setInputType(inputType); field.setTextSize(14);
        field.setSingleLine((inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        field.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return field;
    }
}
