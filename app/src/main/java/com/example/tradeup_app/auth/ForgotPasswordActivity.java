package com.example.tradeup_app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tradeup_app.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText emailEditText;
    private TextInputLayout emailInputLayout;
    private MaterialButton resetButton;
    private TextView backToLoginLink;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initFirebase();
        initViews();
        setupListeners();
        setupBackPressedHandler();
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
    }

    private void initViews() {
        emailEditText = findViewById(R.id.emailEditText);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        resetButton = findViewById(R.id.resetButton);
        backToLoginLink = findViewById(R.id.backToLoginLink);

        // Disable reset button initially
        resetButton.setEnabled(false);
    }

    private void setupListeners() {
        // Email validation
        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateEmail();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Reset button click
        resetButton.setOnClickListener(v -> sendPasswordResetEmail());

        // Back to login link
        backToLoginLink.setOnClickListener(v -> navigateToLogin());
    }

    private void setupBackPressedHandler() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToLogin();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void validateEmail() {
        String email = emailEditText.getText() != null ?
            emailEditText.getText().toString().trim() : "";

        if (email.isEmpty()) {
            emailInputLayout.setError(null);
            resetButton.setEnabled(false);
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError(getString(R.string.invalid_email));
            resetButton.setEnabled(false);
        } else {
            emailInputLayout.setError(null);
            resetButton.setEnabled(true);
        }
    }

    private void sendPasswordResetEmail() {
        String email = emailEditText.getText() != null ?
            emailEditText.getText().toString().trim() : "";

        setLoading(true);

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        showSuccessDialog();
                    } else {
                        String errorMessage = task.getException() != null ?
                            task.getException().getMessage() : "Failed to send reset email";

                        // Handle specific error cases
                        if (errorMessage != null && errorMessage.contains("There is no user record")) {
                            Toast.makeText(this, R.string.no_user_found, Toast.LENGTH_LONG).show();
                        } else if (errorMessage != null && errorMessage.contains("network error")) {
                            Toast.makeText(this, R.string.network_error, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_email_sent)
                .setMessage(R.string.reset_email_message)
                .setPositiveButton(R.string.ok, (dialog, which) -> navigateToLogin())
                .setNeutralButton(R.string.resend, (dialog, which) -> sendPasswordResetEmail())
                .setCancelable(false)
                .show();
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void setLoading(boolean loading) {
        resetButton.setEnabled(!loading);
        emailEditText.setEnabled(!loading);

        if (loading) {
            resetButton.setText(R.string.sending);
        } else {
            resetButton.setText(R.string.send_reset_email);
        }
    }
}
