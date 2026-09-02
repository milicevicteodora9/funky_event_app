package com.example.funkyeventapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.funkyeventapp.R;
import com.example.funkyeventapp.models.TeamMember;
import com.example.funkyeventapp.repositories.TeamRepository;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

public final class TeamMemberDialog {
    public interface OnSavedListener { void onSaved(TeamMember member); }

    private TeamMemberDialog() { }

    public static void show(Fragment fragment, TeamRepository repository,
                            @Nullable TeamMember existing, OnSavedListener listener) {
        View content = LayoutInflater.from(fragment.requireContext()).inflate(R.layout.dialog_team_member, null);
        TextInputEditText fullName = content.findViewById(R.id.inputTeamFullName);
        TextInputEditText phone = content.findViewById(R.id.inputTeamPhone);
        TextInputEditText email = content.findViewById(R.id.inputTeamEmail);
        TextInputEditText city = content.findViewById(R.id.inputTeamCity);
        TextInputEditText account = content.findViewById(R.id.inputTeamBankAccount);
        TextInputEditText notes = content.findViewById(R.id.inputTeamNotes);
        SwitchMaterial active = content.findViewById(R.id.switchTeamActive);
        if (existing != null) {
            fullName.setText(existing.getFullName()); phone.setText(existing.getPhone()); email.setText(existing.getEmail());
            city.setText(existing.getCity()); account.setText(existing.getBankAccount()); notes.setText(existing.getNotes());
            active.setChecked(existing.isActive());
        } else {
            active.setChecked(true);
            active.setVisibility(View.GONE);
        }
        AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(existing == null ? R.string.new_team_member : R.string.edit_team_member)
                .setView(content).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.save, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = value(fullName);
            if (name.isEmpty()) { fullName.setError(fragment.getString(R.string.full_name_required)); return; }
            TeamMember member = new TeamMember(existing == null ? null : existing.getId(), name, value(phone), value(email),
                    value(city), value(account), value(notes), active.isChecked());
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.cashbox_saving);
            TeamRepository.Callback<TeamMember> callback = new TeamRepository.Callback<TeamMember>() {
                @Override public void onSuccess(TeamMember saved) {
                    if (!fragment.isAdded()) return;
                    dialog.dismiss();
                    listener.onSaved(saved);
                    Toast.makeText(fragment.requireContext(), existing == null
                            ? R.string.team_member_saved : R.string.team_member_updated,
                            Toast.LENGTH_SHORT).show();
                }

                @Override public void onError(@NonNull Exception error) {
                    if (!fragment.isAdded()) return;
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.save);
                    Toast.makeText(fragment.requireContext(), R.string.team_member_save_error,
                            Toast.LENGTH_LONG).show();
                }
            };
            if (existing == null) repository.addTeamMember(member, callback);
            else repository.updateTeamMember(member, callback);
        }));
        dialog.show();
    }

    private static String value(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}
