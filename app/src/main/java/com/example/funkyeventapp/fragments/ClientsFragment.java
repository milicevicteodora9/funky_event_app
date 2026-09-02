package com.example.funkyeventapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.funkyeventapp.R;
import com.example.funkyeventapp.adapters.ClientAdapter;
import com.example.funkyeventapp.models.Client;
import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.repositories.BudgetRepository;
import com.example.funkyeventapp.repositories.ClientRepository;
import com.example.funkyeventapp.repositories.EventRepository;
import com.example.funkyeventapp.services.ClientFinanceCalculator;
import com.example.funkyeventapp.ui.AuthenticatedHeader;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ClientsFragment extends Fragment {
    private final ClientRepository clientRepository = ClientRepository.getInstance();
    private final EventRepository eventRepository = EventRepository.getInstance();
    private final BudgetRepository budgetRepository = BudgetRepository.getInstance();
    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.GERMANY));
    private TextView invoicedLabel, invoicedValue, actualLabel, actualValue;
    private TextView clientsTitle;
    private ClientAdapter adapter;
    private boolean skipNextResumeRefresh;

    public ClientsFragment() { super(R.layout.fragment_clients); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!AuthenticatedHeader.bind(this, view)) return;
        clientsTitle = view.findViewById(R.id.textClientsTitle);
        adapter = new ClientAdapter(client -> {
            Bundle arguments = new Bundle();
            arguments.putString("clientId", client.getId());
            Navigation.findNavController(view).navigate(R.id.action_clientsFragment_to_clientDetailsFragment, arguments);
        }, client -> showClientDialog(view, client), client -> confirmDeleteClient(view, client));
        RecyclerView clientsList = view.findViewById(R.id.recyclerClients);
        clientsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        clientsList.setAdapter(adapter);
        clientsList.setHasFixedSize(true);

        invoicedLabel = view.findViewById(R.id.textInvoicedLabel);
        invoicedValue = view.findViewById(R.id.textInvoicedValue);
        actualLabel = view.findViewById(R.id.textActualLabel);
        actualValue = view.findViewById(R.id.textActualValue);
        bindFinancialOverview(ClientFinanceCalculator.zero(), LocalDate.now().getYear());

        clientsTitle.setText(getString(R.string.clients_count, 0));
        loadOverview(view);
        skipNextResumeRefresh = true;
        view.findViewById(R.id.buttonAddClient).setOnClickListener(v ->
                showClientDialog(view, null));
        view.findViewById(R.id.buttonEvents).setOnClickListener(this::returnToEvents);
        view.findViewById(R.id.buttonCashbox).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_clientsFragment_to_cashboxFragment));
        view.findViewById(R.id.buttonTeam).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_clientsFragment_to_teamFragment));
        view.findViewById(R.id.buttonAdmin).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.userManagementFragment));
        view.findViewById(R.id.buttonUsers).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_clientsFragment_to_allCashboxesFragment));
        int[] informationalViews = {};
        for (int id : informationalViews) view.findViewById(id).setOnClickListener(this::showComingLater);
    }

    @Override public void onResume() {
        super.onResume();
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false;
        } else if (invoicedValue != null && getView() != null) {
            loadOverview(getView());
        }
    }

    private void loadOverview(View root) {
        clientRepository.getAllClients(new ClientRepository.Callback<List<Client>>() {
            @Override public void onSuccess(List<Client> clients) {
                if (!isAdded() || getView() != root) return;
                adapter.submitList(clients);
                clientsTitle.setText(getString(R.string.clients_count, clients.size()));
                loadFinancialOverview(root);
            }

            @Override public void onError(@NonNull Exception error) {
                if (!isAdded() || getView() != root) return;
                Toast.makeText(requireContext(), R.string.clients_load_error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFinancialOverview(View root) {
        eventRepository.getAllEvents(new EventRepository.Callback<List<Event>>() {
            @Override public void onSuccess(List<Event> events) {
                if (!isAdded() || getView() != root) return;
                budgetRepository.getFinancialsForEvents(events,
                        new BudgetRepository.Callback<java.util.Map<String, BudgetRepository.EventFinancials>>() {
                            @Override public void onSuccess(
                                    java.util.Map<String, BudgetRepository.EventFinancials> financials) {
                                if (!isAdded() || getView() != root) return;
                                adapter.submitFinancials(ClientFinanceCalculator.byClient(events, financials));
                                int year = LocalDate.now().getYear();
                                bindFinancialOverview(
                                        ClientFinanceCalculator.forYear(events, financials, year), year);
                            }

                            @Override public void onError(@NonNull Exception error) {
                                if (!isAdded() || getView() != root) return;
                                adapter.submitFinancials(ClientFinanceCalculator.byClient(
                                        events, Collections.emptyMap()));
                                bindFinancialOverview(ClientFinanceCalculator.zero(),
                                        LocalDate.now().getYear());
                                Toast.makeText(requireContext(), R.string.budget_load_error,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override public void onError(@NonNull Exception error) {
                if (!isAdded() || getView() != root) return;
                Toast.makeText(requireContext(), R.string.events_load_error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showClientDialog(View root, @Nullable Client existing) {
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_client, null);
        TextInputEditText name = content.findViewById(R.id.inputClientName);
        TextInputEditText taxId = content.findViewById(R.id.inputClientTaxId);
        TextInputEditText address = content.findViewById(R.id.inputClientAddress);
        TextInputEditText email = content.findViewById(R.id.inputClientEmail);
        TextInputEditText phone = content.findViewById(R.id.inputClientPhone);
        TextInputEditText contactPerson = content.findViewById(R.id.inputClientContactPerson);
        if (existing != null) {
            name.setText(existing.getName());
            taxId.setText(existing.getTaxId());
            address.setText(existing.getAddress());
            email.setText(existing.getEmail());
            phone.setText(existing.getPhone());
            contactPerson.setText(existing.getContactPerson());
        }
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(existing == null ? R.string.new_client : R.string.edit_client)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(ignored -> {
            Window window = dialog.getWindow();
            if (window != null) {
                int width = (int) (getResources().getDisplayMetrics().widthPixels * .92f);
                int height = (int) (getResources().getDisplayMetrics().heightPixels * .86f);
                window.setLayout(width, height);
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String clientName = value(name);
                    if (clientName.isEmpty()) {
                        name.setError(getString(R.string.client_name_required));
                        return;
                    }
                    Client client = new Client(existing == null ? null : existing.getId(), clientName,
                            existing == null ? "" : existing.getLogoUri(), value(taxId),
                            value(address), value(email), value(phone), value(contactPerson));
                    View saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    saveButton.setEnabled(false);
                    Task<Void> saveTask = existing == null
                            ? clientRepository.createClient(client)
                            : clientRepository.updateClient(client);
                    saveTask
                            .addOnSuccessListener(unused -> {
                                if (!isAdded() || getView() != root) return;
                                dialog.dismiss();
                                Toast.makeText(requireContext(), existing == null
                                                ? R.string.client_saved : R.string.client_updated,
                                        Toast.LENGTH_SHORT).show();
                                loadOverview(root);
                            })
                            .addOnFailureListener(error -> {
                                if (!isAdded() || getView() != root) return;
                                saveButton.setEnabled(true);
                                Toast.makeText(requireContext(), existing == null
                                                ? R.string.client_save_error : R.string.client_update_error,
                                        Toast.LENGTH_SHORT).show();
                            });
                });
        });
        dialog.show();
    }

    private String value(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void confirmDeleteClient(View root, Client client) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_client)
                .setMessage(getString(R.string.delete_client_question, client.getName()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) ->
                        clientRepository.deleteClient(client.getId())
                                .addOnSuccessListener(unused -> {
                                    if (!isAdded() || getView() != root) return;
                                    Toast.makeText(requireContext(), R.string.client_deleted,
                                            Toast.LENGTH_SHORT).show();
                                    loadOverview(root);
                                })
                                .addOnFailureListener(error -> {
                                    if (!isAdded() || getView() != root) return;
                                    Toast.makeText(requireContext(), R.string.client_delete_error,
                                            Toast.LENGTH_SHORT).show();
                                }))
                .show();
    }

    private void bindFinancialOverview(ClientFinanceCalculator.Totals totals, int year) {
        invoicedLabel.setText(getString(R.string.clients_total_external_year, year));
        actualLabel.setText(getString(R.string.clients_actual_profit_year, year));
        invoicedValue.setText(moneyFormat.format(totals.getRevenue()) + " EUR");
        actualValue.setText(moneyFormat.format(totals.getProfit()) + " EUR");
    }

    private void returnToEvents(View view) {
        NavController navController = Navigation.findNavController(view);
        if (!navController.popBackStack(R.id.eventsFragment, false)) {
            navController.navigate(R.id.eventsFragment);
        }
    }

    private void showComingLater(View view) {
        String label = view.getContentDescription() == null
                ? ((TextView) view).getText().toString()
                : view.getContentDescription().toString();
        Toast.makeText(requireContext(), getString(R.string.coming_later, label), Toast.LENGTH_SHORT).show();
    }
}
