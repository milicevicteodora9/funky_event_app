package com.example.funkyeventapp.adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.funkyeventapp.R;
import com.example.funkyeventapp.models.User;
import com.example.funkyeventapp.models.UserRole;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class UserManagementAdapter extends RecyclerView.Adapter<UserManagementAdapter.Holder> {
    public interface Listener {
        void onRoleChanged(User user, UserRole role);
        void onActiveChanged(User user);
    }

    private final Listener listener;
    private final String currentUserId;
    private final List<User> users = new ArrayList<>();

    public UserManagementAdapter(String currentUserId, Listener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void submitList(List<User> values) {
        users.clear();
        users.addAll(values);
        notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_management, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        User user = users.get(position);
        boolean current = currentUserId.equals(user.getId());
        holder.name.setText(user.getFullName());
        holder.you.setVisibility(current ? View.VISIBLE : View.GONE);
        holder.badge.setText(user.isActive() ? label(user.getRole()) : holder.itemView.getContext().getString(R.string.inactive));
        holder.badge.setTextColor(Color.parseColor(user.isActive() ? badgeText(user.getRole()) : "#737987"));
        holder.role.setText(label(user.getRole()));
        holder.role.setEnabled(!current && user.isActive());
        holder.role.setAlpha(holder.role.isEnabled() ? 1f : .48f);
        holder.action.setVisibility(current ? View.INVISIBLE : View.VISIBLE);
        holder.action.setImageResource(user.isActive() ? R.drawable.ic_delete : R.drawable.ic_activate_user);
        holder.action.setContentDescription(holder.itemView.getContext().getString(user.isActive() ? R.string.deactivate : R.string.activate));
        holder.itemView.setAlpha(user.isActive() ? 1f : .62f);
        holder.role.setOnClickListener(v -> showRoles(holder.role, user));
        holder.action.setOnClickListener(v -> listener.onActiveChanged(user));
    }

    private void showRoles(View anchor, User user) {
        PopupMenu menu = new PopupMenu(anchor.getContext(), anchor);
        for (UserRole role : UserRole.values()) menu.getMenu().add(label(role));
        menu.setOnMenuItemClickListener(item -> {
            listener.onRoleChanged(user, UserRole.valueOf(item.getTitle().toString().toUpperCase()));
            return true;
        });
        menu.show();
    }

    private String label(UserRole role) {
        String value = role.name().toLowerCase();
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
    private String badgeText(UserRole role) {
        if (role == UserRole.ADMIN) return "#E53935";
        if (role == UserRole.MANAGER) return "#8E5AB8";
        return "#66778A";
    }

    @Override public int getItemCount() { return users.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView name, you, badge;
        final MaterialButton role;
        final ImageButton action;
        Holder(View view) {
            super(view);
            name = view.findViewById(R.id.textUserName);
            you = view.findViewById(R.id.textYouBadge);
            badge = view.findViewById(R.id.textRoleBadge);
            role = view.findViewById(R.id.buttonUserRole);
            action = view.findViewById(R.id.buttonUserActive);
        }
    }
}
