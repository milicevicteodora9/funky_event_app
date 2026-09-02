package com.example.funkyeventapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.funkyeventapp.R;
import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.models.TeamFee;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class TeamFeeAdapter extends RecyclerView.Adapter<TeamFeeAdapter.Holder> {
    public interface Listener { void onEdit(TeamFee fee); void onDelete(TeamFee fee); }
    private final List<TeamFee> items = new ArrayList<>();
    private final Map<String, String> eventNames = new HashMap<>();
    private final Listener listener;
    private final DecimalFormat money = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.GERMANY));
    private final DateTimeFormatter date = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    public TeamFeeAdapter(Listener listener){this.listener=listener;}
    public void submitList(List<TeamFee> list){items.clear();items.addAll(list);notifyDataSetChanged();}
    public void submitEventNames(List<Event> events){eventNames.clear();for(Event event:events)eventNames.put(event.getId(),event.getName());notifyDataSetChanged();}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup p,int t){return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_team_fee,p,false));}
    @Override public void onBindViewHolder(@NonNull Holder h,int p){h.bind(items.get(p));}
    @Override public int getItemCount(){return items.size();}
    class Holder extends RecyclerView.ViewHolder{
        final TextView amount,event,dateView;
        Holder(View v){super(v);amount=v.findViewById(R.id.textFeeAmount);event=v.findViewById(R.id.textFeeEvent);dateView=v.findViewById(R.id.textFeeDate);v.findViewById(R.id.buttonEditFee).setOnClickListener(x->listener.onEdit(items.get(getBindingAdapterPosition())));v.findViewById(R.id.buttonDeleteFee).setOnClickListener(x->listener.onDelete(items.get(getBindingAdapterPosition())));}
        void bind(TeamFee fee){amount.setText(money.format(fee.getAmount())+" "+fee.getCurrency());String eventName=fee.getEventId()==null?itemView.getContext().getString(R.string.no_event):eventNames.get(fee.getEventId());event.setText((eventName==null?itemView.getContext().getString(R.string.unknown_event):eventName)+" · "+fee.getDescription());dateView.setText(fee.getDate().format(date));}
    }
}
