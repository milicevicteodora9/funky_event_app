package com.example.funkyeventapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.funkyeventapp.R;
import com.example.funkyeventapp.models.TeamMember;
import com.example.funkyeventapp.repositories.MockDataRepository;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TeamMemberAdapter extends RecyclerView.Adapter<TeamMemberAdapter.TeamMemberViewHolder> {
    public interface Listener { void onOpen(TeamMember member); void onEdit(TeamMember member); }

    private final List<TeamMember> members = new ArrayList<>();
    private final MockDataRepository repository;
    private final Listener listener;
    private final DecimalFormat money = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.GERMANY));

    public TeamMemberAdapter(MockDataRepository repository, Listener listener) {
        this.repository = repository;
        this.listener = listener;
    }

    public void submitList(List<TeamMember> updated) {
        members.clear();
        members.addAll(updated);
        notifyDataSetChanged();
    }

    @NonNull @Override public TeamMemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TeamMemberViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_team_member, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull TeamMemberViewHolder holder, int position) { holder.bind(members.get(position)); }
    @Override public int getItemCount() { return members.size(); }

    class TeamMemberViewHolder extends RecyclerView.ViewHolder {
        private final TextView name, contact, debt;
        private final ImageButton edit;

        TeamMemberViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textTeamMemberName);
            contact = itemView.findViewById(R.id.textTeamMemberContact);
            debt = itemView.findViewById(R.id.textTeamMemberDebt);
            edit = itemView.findViewById(R.id.buttonEditTeamMember);
        }

        void bind(TeamMember member) {
            name.setText(member.getFullName());
            StringBuilder details = new StringBuilder();
            if (!empty(member.getPhone())) details.append("☎ ").append(member.getPhone());
            if (!empty(member.getCity())) {
                if (details.length() > 0) details.append("    ");
                details.append("⌖ ").append(member.getCity());
            }
            contact.setText(details);
            contact.setVisibility(details.length() == 0 ? View.GONE : View.VISIBLE);
            BigDecimal memberDebt = repository.getDebtForMember(member.getId());
            debt.setVisibility(memberDebt.signum() > 0 ? View.VISIBLE : View.GONE);
            debt.setText(money.format(memberDebt) + " €");
            itemView.setAlpha(member.isActive() ? 1f : .55f);
            itemView.setOnClickListener(v -> listener.onOpen(member));
            edit.setOnClickListener(v -> listener.onEdit(member));
        }

        private boolean empty(String value) { return value == null || value.trim().isEmpty(); }
    }
}
