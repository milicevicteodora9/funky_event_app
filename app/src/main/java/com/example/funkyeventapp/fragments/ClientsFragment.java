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
import com.example.funkyeventapp.models.BudgetType;
import com.example.funkyeventapp.models.Client;
import com.example.funkyeventapp.repositories.ClientRepository;
import com.example.funkyeventapp.repositories.MockDataRepository;
import com.example.funkyeventapp.ui.AuthenticatedHeader;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class ClientsFragment extends Fragment {
    private final ClientRepository clientRepository = ClientRepository.getInstance();
    private final MockDataRepository mockRepository = MockDataRepository.getInstance();
    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.GERMANY));
    private TextView invoicedLabel, invoicedValue, actualLabel, actualValue;
    private TextView clientsTitle;
    private ClientAdapter adapter;

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
        bindFinancialOverview();

        clientsTitle.setText(getString(R.string.clients_count, 0));
        loadClients(view, adapter, clientsTitle);
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
        if (invoicedValue != null) bindFinancialOverview();
    }

    private void loadClients(View root, ClientAdapter adapter, TextView clientsTitle) {
        clientRepository.getAllClients(new ClientRepository.Callback<List<Client>>() {
            @Override public void onSuccess(List<Client> clients) {
                if (!isAdded() || getView() != root) return;
                adapter.submitList(clients);
                clientsTitle.setText(getString(R.string.clients_count, clients.size()));
            }

            @Override public void onError(@NonNull Exception error) {
                if (!isAdded() || getView() != root) return;
                Toast.makeText(requireContext(), R.string.clients_load_error,
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
                                loadClients(root, adapter, clientsTitle);
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
                                    loadClients(root, adapter, clientsTitle);
                                })
                                .addOnFailureListener(error -> {
                                    if (!isAdded() || getView() != root) return;
                                    Toast.makeText(requireContext(), R.string.client_delete_error,
                                            Toast.LENGTH_SHORT).show();
                                }))
                .show();
    }

    private void bindFinancialOverview() {
        invoicedLabel.setText(R.string.clients_total_external);
        actualLabel.setText(R.string.clients_actual_profit);
        BigDecimal external = mockRepository.getTotalForAllBudgets(BudgetType.EXTERNAL);
        BigDecimal actual = mockRepository.getTotalForAllBudgets(BudgetType.ACTUAL);
        invoicedValue.setText(moneyFormat.format(external) + " EUR");
        actualValue.setText(moneyFormat.format(external.subtract(actual)) + " EUR");
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
