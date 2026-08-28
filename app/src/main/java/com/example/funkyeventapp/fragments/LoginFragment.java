package com.example.funkyeventapp.fragments;

import android.os.Bundle;
import android.util.Patterns;
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
        TextInputEditText email = view.findViewById(R.id.inputLoginEmail);
        TextInputEditText password = view.findViewById(R.id.inputLoginPassword);
        View loginButton = view.findViewById(R.id.buttonLogin);
        View createAccountButton = view.findViewById(R.id.buttonCreateAccount);

        loginButton.setOnClickListener(v -> {
            String emailValue = text(email);
            String passwordValue = text(password);
            if (emailValue.isEmpty() || passwordValue.isEmpty()) {
                Toast.makeText(requireContext(), R.string.enter_credentials, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
                email.setError(getString(R.string.invalid_email));
                return;
            }
            setLoading(loginButton, createAccountButton, true);
            auth.login(emailValue, passwordValue, (result, user) -> {
                if (!isAdded() || getView() != view) return;
                setLoading(loginButton, createAccountButton, false);
                if (result == AuthService.Result.SUCCESS) route(view, user);
                else showError(result);
            });
        });

        createAccountButton.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_loginFragment_to_signUpFragment));
        view.findViewById(R.id.buttonForgotPassword).setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.coming_later_short, Toast.LENGTH_SHORT).show());

        if (auth.isAuthenticated()) {
            route(view, auth.getCurrentUser());
        } else if (auth.hasFirebaseSession()) {
            setLoading(loginButton, createAccountButton, true);
            auth.restoreSession((result, user) -> {
                if (!isAdded() || getView() != view) return;
                setLoading(loginButton, createAccountButton, false);
                if (result == AuthService.Result.SUCCESS) route(view, user);
                else showError(result);
            });
        }
    }

    private void route(View view, User user) {
        if (user == null) return;
        int action = user.getRole() == UserRole.COORDINATOR
                ? R.id.action_loginFragment_to_cashboxFragment
                : R.id.action_loginFragment_to_eventsFragment;
        Navigation.findNavController(view).navigate(action);
    }

    private void showError(AuthService.Result result) {
        int message;
        switch (result) {
            case INACTIVE_USER: message = R.string.inactive_account; break;
            case NETWORK_ERROR: message = R.string.network_error; break;
            case MISSING_USER_DOCUMENT: message = R.string.missing_user_document; break;
            case INVALID_EMAIL: message = R.string.invalid_email; break;
            case INVALID_CREDENTIALS: message = R.string.invalid_credentials; break;
            default: message = R.string.authentication_error;
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    private void setLoading(View login, View createAccount, boolean loading) {
        login.setEnabled(!loading);
        createAccount.setEnabled(!loading);
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}
