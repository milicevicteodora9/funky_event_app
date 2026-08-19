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
import com.example.funkyeventapp.repositories.MockDataRepository;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class ClientsFragment extends Fragment {
    private final MockDataRepository repository = MockDataRepository.getInstance();
    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.GERMANY));
    private TextView invoicedLabel, invoicedValue, actualLabel, actualValue;

    public ClientsFragment() { super(R.layout.fragment_clients); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ClientAdapter adapter = new ClientAdapter(client -> {
            Bundle arguments = new Bundle();
            arguments.putString("clientId", client.getId());
            Navigation.findNavController(view).navigate(R.id.action_clientsFragment_to_clientDetailsFragment, arguments);
        });
        RecyclerView clientsList = view.findViewById(R.id.recyclerClients);
        clientsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        clientsList.setAdapter(adapter);
        clientsList.setHasFixedSize(true);
        adapter.submitList(repository.getClients());

        invoicedLabel = view.findViewById(R.id.textInvoicedLabel);
        invoicedValue = view.findViewById(R.id.textInvoicedValue);
        actualLabel = view.findViewById(R.id.textActualLabel);
        actualValue = view.findViewById(R.id.textActualValue);
        bindFinancialOverview();

        ((TextView) view.findViewById(R.id.textClientsTitle)).setText(
                getString(R.string.clients_count, repository.getClients().size()));
        view.findViewById(R.id.buttonAddClient).setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.add_client_coming, Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.buttonEvents).setOnClickListener(this::returnToEvents);
        view.findViewById(R.id.buttonCashbox).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_clientsFragment_to_cashboxFragment));
        view.findViewById(R.id.buttonTeam).setOnClickListener(this::showComingLater);
        int[] informationalViews = {R.id.buttonUsers, R.id.buttonAdmin, R.id.buttonLogout};
        for (int id : informationalViews) view.findViewById(id).setOnClickListener(this::showComingLater);
    }

    @Override public void onResume() {
        super.onResume();
        if (invoicedValue != null) bindFinancialOverview();
    }

    private void bindFinancialOverview() {
        invoicedLabel.setText(R.string.clients_total_external);
        actualLabel.setText(R.string.clients_actual_profit);
        BigDecimal external = repository.getTotalForAllBudgets(BudgetType.EXTERNAL);
        BigDecimal actual = repository.getTotalForAllBudgets(BudgetType.ACTUAL);
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
