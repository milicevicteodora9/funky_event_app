package com.example.funkyeventapp.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.funkyeventapp.R;
import com.example.funkyeventapp.models.User;
import com.example.funkyeventapp.models.UserRole;
import com.example.funkyeventapp.services.AuthService;
import com.google.android.material.textfield.TextInputEditText;

public class LoginFragment extends Fragment {
    private final AuthService auth = AuthService.getInstance();
    public LoginFragment() { super(R.layout.fragment_login); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        if (auth.isAuthenticated()) { route(view, auth.getCurrentUser()); return; }
        TextInputEditText email = view.findViewById(R.id.inputLoginEmail);
        TextInputEditText password = view.findViewById(R.id.inputLoginPassword);
        view.findViewById(R.id.buttonLogin).setOnClickListener(v -> {
            String emailValue = text(email), passwordValue = text(password);
            if (emailValue.isEmpty() || passwordValue.isEmpty()) {
                Toast.makeText(requireContext(), R.string.enter_credentials, Toast.LENGTH_SHORT).show(); return;
            }
            AuthService.Result result = auth.login(emailValue, passwordValue);
            if (result == AuthService.Result.SUCCESS) route(v, auth.getCurrentUser());
            else Toast.makeText(requireContext(), result == AuthService.Result.INACTIVE_USER
                    ? R.string.inactive_account : R.string.invalid_credentials, Toast.LENGTH_SHORT).show();
        });
        view.findViewById(R.id.buttonCreateAccount).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_loginFragment_to_signUpFragment));
        view.findViewById(R.id.buttonForgotPassword).setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.coming_later_short, Toast.LENGTH_SHORT).show());
    }

    private void route(View view, User user) {
        int action = user.getRole() == UserRole.COORDINATOR
                ? R.id.action_loginFragment_to_cashboxFragment : R.id.action_loginFragment_to_eventsFragment;
        Navigation.findNavController(view).navigate(action);
    }
    private String text(TextInputEditText input) { return input.getText() == null ? "" : input.getText().toString().trim(); }
}
