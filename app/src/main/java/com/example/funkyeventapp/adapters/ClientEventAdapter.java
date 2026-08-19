package com.example.funkyeventapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.funkyeventapp.R;
import com.example.funkyeventapp.models.Event;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClientEventAdapter extends RecyclerView.Adapter<ClientEventAdapter.Holder> {
    public interface Listener { void onClick(Event event); }
    private final List<Event> items = new ArrayList<>();
    private final Listener listener;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    public ClientEventAdapter(Listener listener) { this.listener = listener; }
    public void submitList(List<Event> events) { items.clear(); items.addAll(events); notifyDataSetChanged(); }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_client_event, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder holder, int position) { holder.bind(items.get(position)); }
    @Override public int getItemCount() { return items.size(); }
    class Holder extends RecyclerView.ViewHolder {
        final TextView name, status, meta, location;
        Holder(View view) { super(view); name=view.findViewById(R.id.textHistoryEventName); status=view.findViewById(R.id.textHistoryEventStatus); meta=view.findViewById(R.id.textHistoryEventMeta); location=view.findViewById(R.id.textHistoryEventLocation); }
        void bind(Event event) {
            name.setText(event.getName());
            status.setText(event.getStatus().name());
            String dates = event.getStartDate().format(formatter);
            if (event.getEndDate() != null && !event.getEndDate().equals(event.getStartDate())) dates += " – " + event.getEndDate().format(formatter);
            meta.setText(event.getType().getLabel() + "  •  " + dates);
            location.setText(event.getLocation());
            itemView.setOnClickListener(v -> listener.onClick(event));
        }
    }
}
