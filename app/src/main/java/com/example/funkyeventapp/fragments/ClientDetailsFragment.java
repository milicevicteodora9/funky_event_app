package com.example.funkyeventapp.fragments;

import android.os.Bundle;
import android.view.View;
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
import com.example.funkyeventapp.models.Client;
import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.models.EventStatus;
import com.example.funkyeventapp.repositories.ClientRepository;
import com.example.funkyeventapp.repositories.EventRepository;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ClientDetailsFragment extends Fragment {
    private final ClientRepository clientRepository = ClientRepository.getInstance();
    private final EventRepository eventRepository = EventRepository.getInstance();
    private final List<Event> events = new ArrayList<>();
    private RecyclerView history;
    private TextView empty;
    private MaterialButton eventsTab;

    public ClientDetailsFragment() { super(R.layout.fragment_client_details); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        String clientId = getArguments() == null ? null : getArguments().getString("clientId");
        if (clientId == null) {
            showClientNotFound(view);
            return;
        }

        history = view.findViewById(R.id.recyclerClientHistory);
        history.setLayoutManager(new LinearLayoutManager(requireContext()));
        empty = view.findViewById(R.id.textEmptyClientHistory);
        eventsTab = view.findViewById(R.id.buttonClientEvents);
        view.findViewById(R.id.buttonClientInvoices).setVisibility(View.GONE);
        view.findViewById(R.id.buttonAddInvoice).setVisibility(View.GONE);
        view.findViewById(R.id.textRevenue).setVisibility(View.GONE);
        view.findViewById(R.id.textProfit).setVisibility(View.GONE);
        view.findViewById(R.id.buttonBackClient).setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());
        eventsTab.setOnClickListener(v -> showEvents(view));
        setEventsTabSelected();

        loadClient(view, clientId);
        loadClientEvents(view, clientId);
    }

    private void loadClient(@NonNull View root, @NonNull String clientId) {
        clientRepository.getClientById(clientId, new ClientRepository.Callback<Client>() {
            @Override public void onSuccess(Client client) {
                if (!isAdded() || getView() != root) return;
                if (client == null) {
                    showClientNotFound(root);
                    return;
                }
                bindClient(root, client);
            }

            @Override public void onError(@NonNull Exception error) {
                if (!isAdded() || getView() != root) return;
                Toast.makeText(requireContext(), R.string.clients_load_error,
                        Toast.LENGTH_SHORT).show();
                Navigation.findNavController(root).popBackStack();
            }
        });
    }

    private void loadClientEvents(@NonNull View root, @NonNull String clientId) {
        eventRepository.getAllEvents(new EventRepository.Callback<List<Event>>() {
            @Override public void onSuccess(List<Event> loadedEvents) {
                if (!isAdded() || getView() != root) return;
                events.clear();
                for (Event event : loadedEvents) {
                    if (clientId.equals(event.getClientId())) events.add(event);
                }
                bindSummary(root);
                showEvents(root);
            }

            @Override public void onError(@NonNull Exception error) {
                if (!isAdded() || getView() != root) return;
                Toast.makeText(requireContext(), R.string.events_load_error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindClient(@NonNull View view, @NonNull Client client) {
        ((TextView) view.findViewById(R.id.textDetailClientName)).setText(client.getName());
        ((TextView) view.findViewById(R.id.textDetailContact)).setText(
                client.getContactPerson() + "  •  " + client.getPhone());
        ((TextView) view.findViewById(R.id.textDetailEmail)).setText(client.getEmail());
        ((TextView) view.findViewById(R.id.textDetailAddress)).setText(
                client.getAddress() + "  •  PIB " + client.getTaxId());
    }

    private void bindSummary(@NonNull View view) {
        int current = 0;
        int completed = 0;
        for (Event event : events) {
            if (event.getStatus() == EventStatus.COMPLETED) completed++;
            else current++;
        }
        ((TextView) view.findViewById(R.id.textTotalEvents)).setText(
                getString(R.string.summary_total, events.size()));
        ((TextView) view.findViewById(R.id.textCurrentEvents)).setText(
                getString(R.string.summary_current, current));
        ((TextView) view.findViewById(R.id.textCompletedEvents)).setText(
                getString(R.string.summary_completed, completed));
    }

    private void showEvents(@NonNull View root) {
        ClientEventAdapter adapter = new ClientEventAdapter(event -> {
            Bundle arguments = new Bundle();
            arguments.putString("eventId", event.getId());
            Navigation.findNavController(root).navigate(
                    R.id.action_clientDetailsFragment_to_eventDetailsFragment, arguments);
        });
        adapter.submitList(events);
        history.setAdapter(adapter);
        boolean isEmpty = events.isEmpty();
        empty.setText(R.string.no_client_events);
        empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        history.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void setEventsTabSelected() {
        eventsTab.setBackgroundTintList(requireContext().getColorStateList(R.color.funky_mint));
        eventsTab.setTextColor(requireContext().getColor(R.color.white));
    }

    private void showClientNotFound(@NonNull View view) {
        Toast.makeText(requireContext(), R.string.client_not_found, Toast.LENGTH_SHORT).show();
        Navigation.findNavController(view).popBackStack();
    }
}
