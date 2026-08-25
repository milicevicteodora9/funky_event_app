package com.example.funkyeventapp.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.funkyeventapp.R;
import com.example.funkyeventapp.adapters.UserManagementAdapter;
import com.example.funkyeventapp.models.User;
import com.example.funkyeventapp.models.UserRole;
import com.example.funkyeventapp.repositories.MockDataRepository;
import com.example.funkyeventapp.services.AuthorizationService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;

public class UserManagementFragment extends Fragment {
    private static final String CURRENT_USER_ID = "user_teodora";
    private final MockDataRepository repository = MockDataRepository.getInstance();
    private UserManagementAdapter adapter;

    public UserManagementFragment() { super(R.layout.fragment_user_management); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        User current = repository.getUserById(CURRENT_USER_ID);
        if (!AuthorizationService.canAccessUserManagement(current)) {
            Toast.makeText(requireContext(), R.string.access_denied, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigateUp();
            return;
        }
        adapter = new UserManagementAdapter(CURRENT_USER_ID, new UserManagementAdapter.Listener() {
            @Override public void onRoleChanged(User user, UserRole role) {
                if (!repository.setUserRole(user.getId(), role)) showAdminRequired();
                refresh();
            }
            @Override public void onActiveChanged(User user) { confirmActiveChange(user); }
        });
        RecyclerView recycler = view.findViewById(R.id.recyclerUsers);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
        view.findViewById(R.id.buttonBack).setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        view.findViewById(R.id.buttonAddUser).setOnClickListener(v -> showAddUserDialog());
        view.findViewById(R.id.buttonEvents).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_userManagementFragment_to_eventsFragment));
        view.findViewById(R.id.buttonClients).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_userManagementFragment_to_clientsFragment));
        view.findViewById(R.id.buttonTeam).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_userManagementFragment_to_teamFragment));
        refresh();
    }

    private void refresh() { adapter.submitList(repository.getUsers()); }

    private void confirmActiveChange(User user) {
        if (CURRENT_USER_ID.equals(user.getId())) return;
        if (!user.isActive()) {
            repository.setUserActive(user.getId(), true);
            refresh();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.deactivate_user)
                .setMessage(R.string.deactivate_user_question)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.deactivate, (dialog, which) -> {
                    if (!repository.setUserActive(user.getId(), false)) showAdminRequired();
                    refresh();
                }).show();
    }

    private void showAddUserDialog() {
        View form = getLayoutInflater().inflate(R.layout.dialog_add_user, null, false);
        EditText first = form.findViewById(R.id.inputFirstName);
        EditText last = form.findViewById(R.id.inputLastName);
        EditText email = form.findViewById(R.id.inputEmail);
        AutoCompleteTextView role = form.findViewById(R.id.inputRole);
        role.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line,
                Arrays.asList("Admin", "Manager", "Coordinator")));
        role.setText("Coordinator", false);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_user).setView(form).setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String firstValue = first.getText().toString().trim();
            String lastValue = last.getText().toString().trim();
            String emailValue = email.getText().toString().trim();
            String roleValue = role.getText().toString().trim();
            if (firstValue.isEmpty() || lastValue.isEmpty() || emailValue.isEmpty() || roleValue.isEmpty()
                    || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
                Toast.makeText(requireContext(), R.string.invalid_user, Toast.LENGTH_SHORT).show(); return;
            }
            if (repository.emailExists(emailValue)) {
                email.setError(getString(R.string.email_exists)); return;
            }
            repository.addUser(new User(null, firstValue, lastValue, emailValue,
                    UserRole.valueOf(roleValue.toUpperCase()), true));
            refresh(); dialog.dismiss();
        }));
        dialog.show();
    }

    private void showAdminRequired() {
        Toast.makeText(requireContext(), R.string.admin_required, Toast.LENGTH_SHORT).show();
    }
}
