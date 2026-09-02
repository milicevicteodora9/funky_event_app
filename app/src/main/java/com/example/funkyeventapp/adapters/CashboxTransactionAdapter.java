package com.example.funkyeventapp.adapters;

import android.view.LayoutInflater; import android.view.View; import android.view.ViewGroup; import android.widget.TextView;
import androidx.annotation.NonNull; import androidx.recyclerview.widget.RecyclerView;
import com.example.funkyeventapp.R; import com.example.funkyeventapp.models.CashboxTransaction; import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.models.ExpensePurpose; import com.example.funkyeventapp.models.TransactionType;
import java.text.DecimalFormat; import java.text.DecimalFormatSymbols; import java.time.format.DateTimeFormatter; import java.util.ArrayList; import java.util.HashMap; import java.util.List; import java.util.Locale; import java.util.Map;

public class CashboxTransactionAdapter extends RecyclerView.Adapter<CashboxTransactionAdapter.Holder> {
    public interface Listener { void onReceipt(CashboxTransaction item); void onEdit(CashboxTransaction item); void onDelete(CashboxTransaction item); }
    private final List<CashboxTransaction> items=new ArrayList<>(); private final Listener listener;
    private final Map<String,String> eventNames=new HashMap<>();
    private final DecimalFormat money=new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.GERMANY));
    private final DateTimeFormatter date=DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    public CashboxTransactionAdapter(Listener listener){this.listener=listener;}
    public void submitList(List<CashboxTransaction> list){items.clear();items.addAll(list);notifyDataSetChanged();}
    public void submitEventNames(List<Event> events){eventNames.clear();for(Event event:events)eventNames.put(event.getId(),event.getName());notifyDataSetChanged();}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup p,int t){return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_cashbox_transaction,p,false));}
    @Override public void onBindViewHolder(@NonNull Holder h,int p){h.bind(items.get(p));} @Override public int getItemCount(){return items.size();}
    class Holder extends RecyclerView.ViewHolder {
        TextView amount,name,dateText,purpose; View receiptButton,editButton,deleteButton; Holder(View v){super(v);amount=v.findViewById(R.id.textTransactionAmount);name=v.findViewById(R.id.textTransactionName);dateText=v.findViewById(R.id.textTransactionDate);purpose=v.findViewById(R.id.textTransactionPurpose);receiptButton=v.findViewById(R.id.buttonTransactionReceipt);editButton=v.findViewById(R.id.buttonEditTransaction);deleteButton=v.findViewById(R.id.buttonDeleteTransaction);receiptButton.setOnClickListener(x->listener.onReceipt(items.get(getBindingAdapterPosition())));editButton.setOnClickListener(x->listener.onEdit(items.get(getBindingAdapterPosition())));deleteButton.setOnClickListener(x->listener.onDelete(items.get(getBindingAdapterPosition())));}
        void bind(CashboxTransaction tx){
            boolean income=tx.getTransactionType()==TransactionType.INCOME;
            String sign=income?"+":"-";
            amount.setText((income?"↗  ":"↘  ")+sign+money.format(tx.getAmount())+" "+tx.getCurrency().name()+"   ≈ "+money.format(tx.getAmountInEur())+" EUR");
            amount.setTextColor(itemView.getContext().getColor(income?R.color.funky_mint:R.color.funky_expense));
            name.setText(tx.getDescription()==null||tx.getDescription().isEmpty()||tx.getDescription().equals(tx.getName())?tx.getName():tx.getName()+" · "+tx.getDescription());
            dateText.setText(tx.getDate().format(date));
            String purposeLabel=tx.getExpensePurpose()==ExpensePurpose.GENERAL
                    ?itemView.getContext().getString(R.string.general_expense)
                    :eventNames.containsKey(tx.getEventId())?eventNames.get(tx.getEventId()):itemView.getContext().getString(R.string.unknown_event);
            purpose.setText(purposeLabel);
            receiptButton.setVisibility(tx.getReceiptId() == null ? View.GONE : View.VISIBLE);
            editButton.setVisibility(income ? View.GONE : View.VISIBLE);
            deleteButton.setVisibility(income ? View.GONE : View.VISIBLE);
        }
    }
}
