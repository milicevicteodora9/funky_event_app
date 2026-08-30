package com.example.funkyeventapp.fragments;

import android.os.Bundle;
import android.view.View;
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
import com.example.funkyeventapp.repositories.UserRepository;
import com.example.funkyeventapp.services.AuthorizationService;
import com.example.funkyeventapp.services.AuthService;
import com.example.funkyeventapp.ui.AuthenticatedHeader;

import java.util.List;

public class UserManagementFragment extends Fragment {
    private final UserRepository repository = UserRepository.getInstance();
    private UserManagementAdapter adapter;

    public UserManagementFragment() { super(R.layout.fragment_user_management); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!AuthenticatedHeader.bind(this, view)) return;
        User current = AuthService.getInstance().getCurrentUser();
        if (!AuthorizationService.canAccessUserManagement(current)) {
            Toast.makeText(requireContext(), R.string.access_denied, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigateUp();
            return;
        }
        adapter = new UserManagementAdapter(current.getId(), new UserManagementAdapter.Listener() {
            @Override public void onRoleChanged(User user, UserRole role) {
                if (user.getRole() == role) return;
                repository.updateUserRole(user.getId(), role)
                        .addOnSuccessListener(unused -> {
                            user.setRole(role);
                            if (adapter != null) adapter.notifyDataSetChanged();
                        })
                        .addOnFailureListener(error -> {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), R.string.user_role_update_error,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
            @Override public void onActiveChanged(User user) { }
        });
        RecyclerView recycler = view.findViewById(R.id.recyclerUsers);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
        view.findViewById(R.id.buttonBack).setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        view.findViewById(R.id.buttonAddUser).setOnClickListener(v -> { });
        view.findViewById(R.id.buttonEvents).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_userManagementFragment_to_eventsFragment));
        view.findViewById(R.id.buttonClients).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_userManagementFragment_to_clientsFragment));
        view.findViewById(R.id.buttonTeam).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_userManagementFragment_to_teamFragment));
        view.findViewById(R.id.buttonUsers).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.allCashboxesFragment));
        refresh();
    }

    private void refresh() {
        repository.getAllUsers(new UserRepository.Callback<List<User>>() {
            @Override public void onSuccess(List<User> users) {
                if (adapter != null) adapter.submitList(users);
            }

            @Override public void onError(@NonNull Exception error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.users_load_error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
