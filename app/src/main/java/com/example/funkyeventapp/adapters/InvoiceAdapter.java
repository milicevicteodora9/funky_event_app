package com.example.funkyeventapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.funkyeventapp.R;
import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.models.Invoice;
import com.example.funkyeventapp.repositories.MockDataRepository;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.Holder> {
    public interface Listener { void onClick(Invoice invoice); }
    private final List<Invoice> items = new ArrayList<>();
    private final Listener listener;
    private final MockDataRepository repository = MockDataRepository.getInstance();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private final DecimalFormat moneyFormatter = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.GERMANY));
    public InvoiceAdapter(Listener listener) { this.listener = listener; }
    public void submitList(List<Invoice> invoices) { items.clear(); items.addAll(invoices); notifyDataSetChanged(); }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invoice, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder holder, int position) { holder.bind(items.get(position)); }
    @Override public int getItemCount() { return items.size(); }
    class Holder extends RecyclerView.ViewHolder {
        final TextView number, status, eventName, date, amount;
        Holder(View view) { super(view); number=view.findViewById(R.id.textInvoiceNumber); status=view.findViewById(R.id.textInvoiceStatus); eventName=view.findViewById(R.id.textInvoiceEvent); date=view.findViewById(R.id.textInvoiceDate); amount=view.findViewById(R.id.textInvoiceAmount); }
        void bind(Invoice invoice) {
            Event event = repository.getEventById(invoice.getEventId());
            number.setText(invoice.getInvoiceNumber()); status.setText(invoice.getStatus().name());
            eventName.setText(event == null ? itemView.getContext().getString(R.string.unknown_event) : event.getName());
            date.setText(invoice.getIssueDate().format(dateFormatter));
            amount.setText(moneyFormatter.format(invoice.getAmount()) + " " + invoice.getCurrency());
            itemView.setOnClickListener(v -> listener.onClick(invoice));
        }
    }
}
