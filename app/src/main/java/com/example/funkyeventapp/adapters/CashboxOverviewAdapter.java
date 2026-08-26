package com.example.funkyeventapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.funkyeventapp.R;
import com.example.funkyeventapp.models.Cashbox;
import com.example.funkyeventapp.models.TransactionType;
import com.example.funkyeventapp.models.User;
import com.example.funkyeventapp.repositories.MockDataRepository;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CashboxOverviewAdapter extends RecyclerView.Adapter<CashboxOverviewAdapter.Holder> {
    public interface Listener { void onOpen(Cashbox cashbox); }
    private final MockDataRepository repository; private final Listener listener; private final List<Cashbox> items=new ArrayList<>();
    private final DecimalFormat money=new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.GERMANY));
    public CashboxOverviewAdapter(MockDataRepository repository,Listener listener){this.repository=repository;this.listener=listener;}
    public void submitList(List<Cashbox> values){items.clear();items.addAll(values);notifyDataSetChanged();}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type){return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cashbox_overview,parent,false));}
    @Override public void onBindViewHolder(@NonNull Holder h,int position){Cashbox box=items.get(position);User owner=box.getUserId()==null?null:repository.getUserById(box.getUserId());h.name.setText(owner==null?h.itemView.getContext().getString(R.string.general_expenses):owner.getFullName());h.received.setText(money.format(repository.getCashboxTotal(box.getId(), TransactionType.INCOME))+" €");h.spent.setText(money.format(repository.getCashboxTotal(box.getId(),TransactionType.EXPENSE))+" €");h.balance.setText(money.format(repository.getCashboxBalance(box.getId()))+" €");h.balance.setTextColor(h.itemView.getContext().getColor(repository.getCashboxBalance(box.getId()).signum()<0?R.color.funky_expense:R.color.funky_mint));h.itemView.setOnClickListener(v->listener.onOpen(box));}
    @Override public int getItemCount(){return items.size();}
    static class Holder extends RecyclerView.ViewHolder{final TextView name,received,spent,balance;Holder(View v){super(v);name=v.findViewById(R.id.textCashboxName);received=v.findViewById(R.id.textCashboxOverviewReceived);spent=v.findViewById(R.id.textCashboxOverviewSpent);balance=v.findViewById(R.id.textCashboxOverviewBalance);}}
}
