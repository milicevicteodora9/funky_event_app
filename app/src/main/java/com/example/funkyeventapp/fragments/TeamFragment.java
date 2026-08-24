package com.example.funkyeventapp.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.funkyeventapp.R;
import com.example.funkyeventapp.adapters.TeamMemberAdapter;
import com.example.funkyeventapp.models.TeamMember;
import com.example.funkyeventapp.repositories.MockDataRepository;
import com.google.android.material.textfield.TextInputEditText;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TeamFragment extends Fragment {
    private final MockDataRepository repository = MockDataRepository.getInstance();
    private final DecimalFormat money = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.GERMANY));
    private final List<TeamMember> allMembers = new ArrayList<>();
    private TeamMemberAdapter adapter;
    private TextView title, totalDebt;
    private TextInputEditText search;

    public TeamFragment() { super(R.layout.fragment_team); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        title = view.findViewById(R.id.textTeamTitle);
        totalDebt = view.findViewById(R.id.textTotalTeamDebt);
        search = view.findViewById(R.id.inputSearchTeam);
        adapter = new TeamMemberAdapter(repository, new TeamMemberAdapter.Listener() {
            @Override public void onOpen(TeamMember member) {
                Toast.makeText(requireContext(), R.string.team_member_details_coming, Toast.LENGTH_SHORT).show();
            }
            @Override public void onEdit(TeamMember member) { showMemberDialog(member); }
        });
        androidx.recyclerview.widget.RecyclerView list = view.findViewById(R.id.recyclerTeam);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) { }
        });
        view.findViewById(R.id.buttonAddTeamMember).setOnClickListener(v -> showMemberDialog(null));
        view.findViewById(R.id.buttonEvents).setOnClickListener(this::openEvents);
        view.findViewById(R.id.buttonClients).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.clientsFragment));
        view.findViewById(R.id.buttonCashbox).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.cashboxFragment));
        int[] informational = {R.id.buttonUsers, R.id.buttonAdmin, R.id.buttonLogout};
        for (int id : informational) view.findViewById(id).setOnClickListener(this::showComingLater);
        refresh();
    }

    private void refresh() {
        allMembers.clear();
        allMembers.addAll(repository.getTeamMembers());
        title.setText(getString(R.string.team_count, allMembers.size()));
        totalDebt.setText(money.format(repository.getTotalTeamDebt()) + " €");
        filter(search == null || search.getText() == null ? "" : search.getText().toString());
    }

    private void filter(String rawQuery) {
        String query = rawQuery.trim().toLowerCase(Locale.ROOT);
        List<TeamMember> result = new ArrayList<>();
        for (TeamMember member : allMembers) {
            if (query.isEmpty() || contains(member.getFullName(), query) || contains(member.getPhone(), query)
                    || contains(member.getCity(), query) || contains(member.getEmail(), query)) result.add(member);
        }
        adapter.submitList(result);
    }

    private boolean contains(String value, String query) { return value != null && value.toLowerCase(Locale.ROOT).contains(query); }

    private void showMemberDialog(@Nullable TeamMember existing) {
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_team_member, null);
        TextInputEditText fullName = content.findViewById(R.id.inputTeamFullName);
        TextInputEditText phone = content.findViewById(R.id.inputTeamPhone);
        TextInputEditText email = content.findViewById(R.id.inputTeamEmail);
        TextInputEditText city = content.findViewById(R.id.inputTeamCity);
        TextInputEditText account = content.findViewById(R.id.inputTeamBankAccount);
        TextInputEditText notes = content.findViewById(R.id.inputTeamNotes);
        com.google.android.material.switchmaterial.SwitchMaterial active = content.findViewById(R.id.switchTeamActive);
        if (existing != null) {
            fullName.setText(existing.getFullName()); phone.setText(existing.getPhone()); email.setText(existing.getEmail());
            city.setText(existing.getCity()); account.setText(existing.getBankAccount()); notes.setText(existing.getNotes()); active.setChecked(existing.isActive());
        } else { active.setChecked(true); active.setVisibility(View.GONE); }
        AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(existing == null ? R.string.new_team_member : R.string.edit_team_member)
                .setView(content).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.save, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = value(fullName);
            if (name.isEmpty()) { fullName.setError(getString(R.string.full_name_required)); return; }
            TeamMember member = new TeamMember(existing == null ? null : existing.getId(), name, value(phone), value(email),
                    value(city), value(account), value(notes), active.isChecked());
            if (existing == null) repository.addTeamMember(member); else repository.updateTeamMember(member);
            dialog.dismiss(); refresh();
            Toast.makeText(requireContext(), existing == null ? R.string.team_member_saved : R.string.team_member_updated, Toast.LENGTH_SHORT).show();
        }));
        dialog.show();
    }

    private String value(TextInputEditText input) { return input.getText() == null ? "" : input.getText().toString().trim(); }
    private void openEvents(View view) {
        NavController controller = Navigation.findNavController(view);
        if (!controller.popBackStack(R.id.eventsFragment, false)) controller.navigate(R.id.eventsFragment);
    }
    private void showComingLater(View view) {
        CharSequence label = view.getContentDescription();
        Toast.makeText(requireContext(), getString(R.string.coming_later, label == null ? "" : label), Toast.LENGTH_SHORT).show();
    }
}
