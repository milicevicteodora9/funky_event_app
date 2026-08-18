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

import com.example.funkyeventapp.R;
import com.example.funkyeventapp.adapters.EventAdapter;
import com.example.funkyeventapp.models.EventStatus;
import com.example.funkyeventapp.repositories.MockDataRepository;
import com.google.android.material.button.MaterialButton;

public class EventsFragment extends Fragment {
    private final MockDataRepository repository = new MockDataRepository();
    private EventAdapter adapter;
    private MaterialButton currentButton;
    private MaterialButton pastButton;

    public EventsFragment() { super(R.layout.fragment_events); }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new EventAdapter(event -> Toast.makeText(requireContext(), event.getName(), Toast.LENGTH_SHORT).show());
        RecyclerView list = view.findViewById(R.id.recyclerEvents);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
        list.setHasFixedSize(true);

        currentButton = view.findViewById(R.id.buttonCurrent);
        pastButton = view.findViewById(R.id.buttonPast);
        currentButton.setText(getString(R.string.current_events, repository.getEvents(EventStatus.CURRENT).size()));
        pastButton.setText(getString(R.string.past_events, repository.getEvents(EventStatus.COMPLETED).size()));
        currentButton.setOnClickListener(v -> showEvents(EventStatus.CURRENT));
        pastButton.setOnClickListener(v -> showEvents(EventStatus.COMPLETED));

        int[] informationalViews = {R.id.buttonClients, R.id.buttonTeam, R.id.buttonCashbox,
                R.id.buttonUsers, R.id.buttonAdmin, R.id.buttonLogout};
        for (int id : informationalViews) view.findViewById(id).setOnClickListener(this::showComingLater);
        showEvents(EventStatus.CURRENT);
    }

    private void showEvents(EventStatus status) {
        boolean current = status == EventStatus.CURRENT;
        adapter.submitList(repository.getEvents(status));
        currentButton.setTextColor(requireContext().getColor(current ? R.color.funky_mint : R.color.funky_text_secondary));
        pastButton.setTextColor(requireContext().getColor(current ? R.color.funky_text_secondary : R.color.funky_mint));
        currentButton.setStrokeWidth(current ? dp(3) : 0);
        pastButton.setStrokeWidth(current ? 0 : dp(3));
    }

    private void showComingLater(View view) {
        String label = view.getContentDescription() == null ? ((TextView) view).getText().toString() : view.getContentDescription().toString();
        Toast.makeText(requireContext(), getString(R.string.coming_later, label), Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
