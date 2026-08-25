package com.example.funkyeventapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.funkyeventapp.R;
import com.example.funkyeventapp.models.TeamPayment;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TeamPaymentAdapter extends RecyclerView.Adapter<TeamPaymentAdapter.Holder> {
    public interface Listener { void onEdit(TeamPayment payment); void onDelete(TeamPayment payment); }
    private final List<TeamPayment> items=new ArrayList<>(); private final Listener listener;
    private final DecimalFormat money=new DecimalFormat("#,##0.00",DecimalFormatSymbols.getInstance(Locale.GERMANY));
    private final DateTimeFormatter date=DateTimeFormatter.ofPattern("dd MMM yyyy",Locale.ENGLISH);
    public TeamPaymentAdapter(Listener listener){this.listener=listener;}
    public void submitList(List<TeamPayment> list){items.clear();items.addAll(list);notifyDataSetChanged();}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup p,int t){return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_team_payment,p,false));}
    @Override public void onBindViewHolder(@NonNull Holder h,int p){h.bind(items.get(p));}@Override public int getItemCount(){return items.size();}
    class Holder extends RecyclerView.ViewHolder{final TextView amount,meta,notes;Holder(View v){super(v);amount=v.findViewById(R.id.textPaymentAmount);meta=v.findViewById(R.id.textPaymentMeta);notes=v.findViewById(R.id.textPaymentNotes);v.findViewById(R.id.buttonEditPayment).setOnClickListener(x->listener.onEdit(items.get(getBindingAdapterPosition())));v.findViewById(R.id.buttonDeletePayment).setOnClickListener(x->listener.onDelete(items.get(getBindingAdapterPosition())));}void bind(TeamPayment p){amount.setText(money.format(p.getAmount())+" "+p.getCurrency());meta.setText(p.getPaymentDate().format(date)+" · "+p.getPaymentMethod());notes.setText(p.getNotes());notes.setVisibility(p.getNotes()==null||p.getNotes().trim().isEmpty()?View.GONE:View.VISIBLE);}}
}
