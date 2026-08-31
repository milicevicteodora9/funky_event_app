package com.example.funkyeventapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.funkyeventapp.R;
import com.example.funkyeventapp.adapters.EventAdapter;
import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.models.EventStatus;
import com.example.funkyeventapp.repositories.EventRepository;
import com.example.funkyeventapp.ui.AuthenticatedHeader;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class EventsFragment extends Fragment {
    private final EventRepository repository = EventRepository.getInstance();
    private final List<Event> events = new ArrayList<>();
    private EventAdapter adapter;
    private MaterialButton currentButton;
    private MaterialButton pastButton;
    private View currentIndicator;
    private View pastIndicator;
    private EventStatus selectedStatus = EventStatus.CURRENT;

    public EventsFragment() { super(R.layout.fragment_events); }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!AuthenticatedHeader.bind(this, view)) return;
        adapter = new EventAdapter(event -> {
            Bundle arguments = new Bundle();
            arguments.putString("eventId", event.getId());
            NavController navController = Navigation.findNavController(view);
            navController.navigate(R.id.action_eventsFragment_to_eventDetailsFragment, arguments);
        });
        RecyclerView list = view.findViewById(R.id.recyclerEvents);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
        list.setHasFixedSize(true);

        currentButton = view.findViewById(R.id.buttonCurrent);
        pastButton = view.findViewById(R.id.buttonPast);
        currentIndicator = view.findViewById(R.id.indicatorCurrent);
        pastIndicator = view.findViewById(R.id.indicatorPast);
        currentButton.setText(getString(R.string.current_events, 0));
        pastButton.setText(getString(R.string.past_events, 0));
        currentButton.setOnClickListener(v -> {
            selectedStatus = EventStatus.CURRENT;
            showEvents(selectedStatus);
        });
        pastButton.setOnClickListener(v -> {
            selectedStatus = EventStatus.COMPLETED;
            showEvents(selectedStatus);
        });

        view.findViewById(R.id.buttonClients).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_eventsFragment_to_clientsFragment));
        view.findViewById(R.id.buttonCashbox).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_eventsFragment_to_cashboxFragment));
        view.findViewById(R.id.buttonTeam).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_eventsFragment_to_teamFragment));
        view.findViewById(R.id.buttonAdmin).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_eventsFragment_to_userManagementFragment));

        view.findViewById(R.id.buttonUsers).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_eventsFragment_to_allCashboxesFragment));
        view.findViewById(R.id.buttonAddEvent).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_eventsFragment_to_addEventFragment));
        showEvents(selectedStatus);
        loadEvents(view);
    }

    private void showEvents(EventStatus status) {
        boolean current = status == EventStatus.CURRENT;
        List<Event> filtered = new ArrayList<>();
        for (Event event : events) {
            if (event.getStatus() == status) filtered.add(event);
        }
        adapter.submitList(filtered);
        currentButton.setTextColor(requireContext().getColor(current ? R.color.funky_mint : R.color.funky_text_secondary));
        pastButton.setTextColor(requireContext().getColor(current ? R.color.funky_text_secondary : R.color.funky_mint));
        currentIndicator.setVisibility(current ? View.VISIBLE : View.INVISIBLE);
        pastIndicator.setVisibility(current ? View.INVISIBLE : View.VISIBLE);
    }

    private void loadEvents(View root) {
        repository.getAllEvents(new EventRepository.Callback<List<Event>>() {
            @Override public void onSuccess(List<Event> loadedEvents) {
                if (!isAdded() || getView() != root) return;
                events.clear();
                events.addAll(loadedEvents);
                int currentCount = 0;
                int pastCount = 0;
                for (Event event : events) {
                    if (event.getStatus() == EventStatus.CURRENT) currentCount++;
                    else if (event.getStatus() == EventStatus.COMPLETED) pastCount++;
                }
                currentButton.setText(getString(R.string.current_events, currentCount));
                pastButton.setText(getString(R.string.past_events, pastCount));
                showEvents(selectedStatus);
            }

            @Override public void onError(@NonNull Exception error) {
                if (!isAdded() || getView() != root) return;
                Toast.makeText(requireContext(), R.string.events_load_error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showComingLater(View view) {
        String label = view.getContentDescription() == null ? ((TextView) view).getText().toString() : view.getContentDescription().toString();
        Toast.makeText(requireContext(), getString(R.string.coming_later, label), Toast.LENGTH_SHORT).show();
    }

}
