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
import com.example.funkyeventapp.models.EventStatus;
import com.example.funkyeventapp.repositories.MockDataRepository;
import com.google.android.material.button.MaterialButton;

public class EventsFragment extends Fragment {
    private final MockDataRepository repository = MockDataRepository.getInstance();
    private EventAdapter adapter;
    private MaterialButton currentButton;
    private MaterialButton pastButton;
    private View currentIndicator;
    private View pastIndicator;

    public EventsFragment() { super(R.layout.fragment_events); }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
        currentButton.setText(getString(R.string.current_events, repository.getEvents(EventStatus.CURRENT).size()));
        pastButton.setText(getString(R.string.past_events, repository.getEvents(EventStatus.COMPLETED).size()));
        currentButton.setOnClickListener(v -> showEvents(EventStatus.CURRENT));
        pastButton.setOnClickListener(v -> showEvents(EventStatus.COMPLETED));

        view.findViewById(R.id.buttonClients).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_eventsFragment_to_clientsFragment));
        view.findViewById(R.id.buttonCashbox).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_eventsFragment_to_cashboxFragment));

        int[] informationalViews = {R.id.buttonTeam,
                R.id.buttonUsers, R.id.buttonAdmin, R.id.buttonLogout, R.id.buttonAddEvent};
        for (int id : informationalViews) view.findViewById(id).setOnClickListener(this::showComingLater);
        showEvents(EventStatus.CURRENT);
    }

    private void showEvents(EventStatus status) {
        boolean current = status == EventStatus.CURRENT;
        adapter.submitList(repository.getEvents(status));
        currentButton.setTextColor(requireContext().getColor(current ? R.color.funky_mint : R.color.funky_text_secondary));
        pastButton.setTextColor(requireContext().getColor(current ? R.color.funky_text_secondary : R.color.funky_mint));
        currentIndicator.setVisibility(current ? View.VISIBLE : View.INVISIBLE);
        pastIndicator.setVisibility(current ? View.INVISIBLE : View.VISIBLE);
    }

    private void showComingLater(View view) {
        String label = view.getContentDescription() == null ? ((TextView) view).getText().toString() : view.getContentDescription().toString();
        Toast.makeText(requireContext(), getString(R.string.coming_later, label), Toast.LENGTH_SHORT).show();
    }

}
