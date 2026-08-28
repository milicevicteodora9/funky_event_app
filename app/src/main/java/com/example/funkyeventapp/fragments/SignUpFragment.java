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

public class SignUpFragment extends Fragment {
    private final AuthService auth = AuthService.getInstance();

    public SignUpFragment() { super(R.layout.fragment_sign_up); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        TextInputEditText first = view.findViewById(R.id.inputSignUpFirstName);
        TextInputEditText last = view.findViewById(R.id.inputSignUpLastName);
        TextInputEditText email = view.findViewById(R.id.inputSignUpEmail);
        TextInputEditText password = view.findViewById(R.id.inputSignUpPassword);
        TextInputEditText confirm = view.findViewById(R.id.inputSignUpConfirmPassword);
        View signUpButton = view.findViewById(R.id.buttonSignUp);
        View backButton = view.findViewById(R.id.buttonSignUpBack);

        backButton.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        signUpButton.setOnClickListener(v -> {
            String f = text(first), l = text(last), e = text(email);
            String p = text(password), c = text(confirm);
            if (f.isEmpty() || l.isEmpty()) {
                Toast.makeText(requireContext(), R.string.invalid_sign_up, Toast.LENGTH_SHORT).show();
                return;
            }
            if (e.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(e).matches()) {
                email.setError(getString(R.string.invalid_email));
                return;
            }
            if (p.length() < 6) {
                password.setError(getString(R.string.password_minimum));
                return;
            }
            if (!p.equals(c)) {
                confirm.setError(getString(R.string.passwords_do_not_match));
                return;
            }

            setLoading(signUpButton, backButton, true);
            User profile = new User(null, f, l, e, UserRole.COORDINATOR, true);
            auth.signUp(profile, p, (result, user) -> {
                if (!isAdded() || getView() != view) return;
                setLoading(signUpButton, backButton, false);
                if (result == AuthService.Result.SUCCESS) {
                    Toast.makeText(requireContext(), R.string.account_created, Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(view).navigate(R.id.action_signUpFragment_to_loginFragment);
                    return;
                }
                showError(result, email, password);
            });
        });
    }

    private void showError(AuthService.Result result, TextInputEditText email, TextInputEditText password) {
        if (result == AuthService.Result.EMAIL_EXISTS) {
            email.setError(getString(R.string.email_exists));
            return;
        }
        if (result == AuthService.Result.INVALID_EMAIL) {
            email.setError(getString(R.string.invalid_email));
            return;
        }
        if (result == AuthService.Result.WEAK_PASSWORD) {
            password.setError(getString(R.string.weak_password));
            return;
        }
        int message = result == AuthService.Result.NETWORK_ERROR
                ? R.string.network_error : R.string.profile_create_error;
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    private void setLoading(View signUp, View back, boolean loading) {
        signUp.setEnabled(!loading);
        back.setEnabled(!loading);
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}
