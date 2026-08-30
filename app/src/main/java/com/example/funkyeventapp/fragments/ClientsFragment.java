package com.example.funkyeventapp.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    public ClientsFragment() { super(R.layout.fragment_clients); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!AuthenticatedHeader.bind(this, view)) return;
        ClientAdapter adapter = new ClientAdapter(client -> {
            Bundle arguments = new Bundle();
            arguments.putString("clientId", client.getId());
            Navigation.findNavController(view).navigate(R.id.action_clientsFragment_to_clientDetailsFragment, arguments);
        });
        RecyclerView clientsList = view.findViewById(R.id.recyclerClients);
        clientsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        clientsList.setAdapter(adapter);
        clientsList.setHasFixedSize(true);

        invoicedLabel = view.findViewById(R.id.textInvoicedLabel);
        invoicedValue = view.findViewById(R.id.textInvoicedValue);
        actualLabel = view.findViewById(R.id.textActualLabel);
        actualValue = view.findViewById(R.id.textActualValue);
        bindFinancialOverview();

        TextView clientsTitle = view.findViewById(R.id.textClientsTitle);
        clientsTitle.setText(getString(R.string.clients_count, 0));
        clientRepository.getAllClients(new ClientRepository.Callback<List<Client>>() {
            @Override public void onSuccess(List<Client> clients) {
                if (!isAdded() || getView() != view) return;
                adapter.submitList(clients);
                clientsTitle.setText(getString(R.string.clients_count, clients.size()));
            }

            @Override public void onError(@NonNull Exception error) {
                if (!isAdded() || getView() != view) return;
                Toast.makeText(requireContext(), R.string.clients_load_error,
                        Toast.LENGTH_SHORT).show();
            }
        });
        view.findViewById(R.id.buttonAddClient).setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.add_client_coming, Toast.LENGTH_SHORT).show());
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
