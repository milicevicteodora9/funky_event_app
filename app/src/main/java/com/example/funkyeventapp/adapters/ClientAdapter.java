package com.example.funkyeventapp.adapters;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.funkyeventapp.R;
import com.example.funkyeventapp.models.Client;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;

public class ClientAdapter extends RecyclerView.Adapter<ClientAdapter.ClientViewHolder> {
    public interface OnClientClickListener { void onClientClick(Client client); }

    private final List<Client> clients = new ArrayList<>();
    private final OnClientClickListener listener;

    public ClientAdapter(OnClientClickListener listener) { this.listener = listener; }

    public void submitList(List<Client> newClients) {
        clients.clear();
        clients.addAll(newClients);
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
        private final TextView initial, name, contact, email, phone;

        ClientViewHolder(@NonNull View itemView) {
            super(itemView);
            logo = itemView.findViewById(R.id.imageClientLogo);
            initial = itemView.findViewById(R.id.textClientInitial);
            name = itemView.findViewById(R.id.textClientName);
            contact = itemView.findViewById(R.id.textContactPerson);
            email = itemView.findViewById(R.id.textClientEmail);
            phone = itemView.findViewById(R.id.textClientPhone);
        }

        void bind(Client client) {
            name.setText(client.getName());
            contact.setText(client.getContactPerson());
            email.setText(client.getEmail());
            phone.setText(client.getPhone());
            String clientName = client.getName();
            initial.setText(clientName == null || clientName.isEmpty() ? "?" : clientName.substring(0, 1));
            bindLogo(client.getLogoUri());
            itemView.setOnClickListener(v -> listener.onClientClick(client));
        }

        private void bindLogo(String logoUri) {
            logo.setTag(logoUri);
            logo.setImageDrawable(null);
            if (logoUri == null || logoUri.trim().isEmpty()) {
                showInitial(logoUri);
                return;
            }
            logo.setVisibility(View.VISIBLE);
            initial.setVisibility(View.GONE);
            try {
                FirebaseStorage.getInstance().getReferenceFromUrl(logoUri)
                        .getBytes(5L * 1024L * 1024L)
                        .addOnSuccessListener(bytes -> {
                            if (!logoUri.equals(logo.getTag())) return;
                            logo.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                        })
                        .addOnFailureListener(error -> showInitial(logoUri));
            } catch (IllegalArgumentException error) {
                showInitial(logoUri);
            }
        }

        private void showInitial(String expectedLogoUri) {
            if (expectedLogoUri != null && !expectedLogoUri.equals(logo.getTag())) return;
            logo.setVisibility(View.GONE);
            initial.setVisibility(View.VISIBLE);
        }
    }
}
