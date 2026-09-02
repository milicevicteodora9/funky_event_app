package com.example.funkyeventapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.funkyeventapp.R;
import com.example.funkyeventapp.models.Client;
import com.example.funkyeventapp.services.ClientFinanceCalculator;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class ClientAdapter extends RecyclerView.Adapter<ClientAdapter.ClientViewHolder> {
    public interface OnClientClickListener { void onClientClick(Client client); }
    public interface OnClientEditListener { void onClientEdit(Client client); }
    public interface OnClientDeleteListener { void onClientDelete(Client client); }

    private final List<Client> clients = new ArrayList<>();
    private final Map<String, ClientFinanceCalculator.Totals> financialsByClientId = new HashMap<>();
    private final DecimalFormat moneyFormat = new DecimalFormat(
            "#,##0.00", DecimalFormatSymbols.getInstance(Locale.GERMANY));
    private final OnClientClickListener listener;
    private final OnClientEditListener editListener;
    private final OnClientDeleteListener deleteListener;

    public ClientAdapter(OnClientClickListener listener, OnClientEditListener editListener,
                         OnClientDeleteListener deleteListener) {
        this.listener = listener;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    public void submitList(List<Client> newClients) {
        clients.clear();
        clients.addAll(newClients);
        notifyDataSetChanged();
    }

    public void submitFinancials(Map<String, ClientFinanceCalculator.Totals> financials) {
        financialsByClientId.clear();
        financialsByClientId.putAll(financials);
        notifyDataSetChanged();
    }

    @NonNull @Override public ClientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_client, parent, false);
        return new ClientViewHolder(view);
    }

    @Override public void onBindViewHolder(@NonNull ClientViewHolder holder, int position) { holder.bind(clients.get(position)); }
    @Override public int getItemCount() { return clients.size(); }

    class ClientViewHolder extends RecyclerView.ViewHolder {
        private final ImageView logo;
        private final TextView initial, name, contact, email, phone, eventCount;
        private final ImageButton edit, delete;

        ClientViewHolder(@NonNull View itemView) {
            super(itemView);
            logo = itemView.findViewById(R.id.imageClientLogo);
            initial = itemView.findViewById(R.id.textClientInitial);
            name = itemView.findViewById(R.id.textClientName);
            contact = itemView.findViewById(R.id.textContactPerson);
            email = itemView.findViewById(R.id.textClientEmail);
            phone = itemView.findViewById(R.id.textClientPhone);
            eventCount = itemView.findViewById(R.id.textClientEventCount);
            edit = itemView.findViewById(R.id.buttonEditClient);
            delete = itemView.findViewById(R.id.buttonDeleteClient);
        }

        void bind(Client client) {
            name.setText(client.getName());
            contact.setText(client.getContactPerson());
            email.setText(client.getEmail());
            phone.setText(client.getPhone());
            ClientFinanceCalculator.Totals totals = financialsByClientId.getOrDefault(
                    client.getId(), ClientFinanceCalculator.zero());
            String eventLabel = itemView.getResources().getQuantityString(
                    R.plurals.client_event_count, totals.getEventCount(), totals.getEventCount());
            eventCount.setText(itemView.getResources().getString(R.string.client_financial_line,
                    eventLabel, moneyFormat.format(totals.getRevenue()),
                    moneyFormat.format(totals.getProfit())));
            logo.setVisibility(View.GONE);
            initial.setVisibility(View.VISIBLE);
            String clientName = client.getName();
            initial.setText(clientName == null || clientName.isEmpty() ? "?" : clientName.substring(0, 1));
            itemView.setOnClickListener(v -> listener.onClientClick(client));
            edit.setOnClickListener(v -> editListener.onClientEdit(client));
            delete.setOnClickListener(v -> deleteListener.onClientDelete(client));
        }
    }
}
