package com.example.funkyeventapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.funkyeventapp.R;
import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.models.EventStatus;

import java.util.ArrayList;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    public interface OnEventClickListener { void onEventClick(Event event); }
    private final List<Event> events = new ArrayList<>();
    private final OnEventClickListener listener;

    public EventAdapter(OnEventClickListener listener) { this.listener = listener; }

    public void submitList(List<Event> newEvents) {
        events.clear();
        events.addAll(newEvents);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new EventViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull EventViewHolder holder, int position) { holder.bind(events.get(position)); }
    @Override public int getItemCount() { return events.size(); }

    class EventViewHolder extends RecyclerView.ViewHolder {
        private final ImageView logo;
        private final TextView logoInitial, name, type, date, location, client, status;
        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            logo = itemView.findViewById(R.id.imageClientLogo);
            logoInitial = itemView.findViewById(R.id.textLogoInitial);
            name = itemView.findViewById(R.id.textEventName);
            type = itemView.findViewById(R.id.textEventType);
            date = itemView.findViewById(R.id.textEventDate);
            location = itemView.findViewById(R.id.textEventLocation);
            client = itemView.findViewById(R.id.textClientName);
            status = itemView.findViewById(R.id.textEventStatus);
        }
        void bind(Event event) {
            name.setText(event.getName());
            type.setText(event.getType().getLabel());
            date.setText(event.getDateDisplay());
            location.setText(event.getLocation());
            client.setText(event.getClientName());
            status.setVisibility(event.getStatus() == EventStatus.COMPLETED ? View.VISIBLE : View.GONE);
            if (event.getLogoResource() != 0) {
                logo.setImageResource(event.getLogoResource());
                logo.setVisibility(View.VISIBLE);
                logoInitial.setVisibility(View.GONE);
            } else {
                logo.setVisibility(View.GONE);
                logoInitial.setVisibility(View.VISIBLE);
                logoInitial.setText(event.getClientName().substring(0, 1));
            }
            itemView.setOnClickListener(v -> listener.onEventClick(event));
        }
    }
}
