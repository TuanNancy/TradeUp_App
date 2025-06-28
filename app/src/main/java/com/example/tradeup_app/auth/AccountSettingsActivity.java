package com.example.tradeup_app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.tradeup_app.R;
import com.example.tradeup_app.auth.Domain.UserModel;
import com.example.tradeup_app.auth.Helper.CurrentUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class AccountSettingsActivity extends AppCompatActivity {

    private TextView accountStatusText;
    private View statusIndicator;
    private LinearLayout deactivateAccountLayout, deleteAccountLayout;

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private UserModel userModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        initFirebase();
        initViews();
        setupListeners();
        updateAccountStatus();
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        userModel = CurrentUser.getUser();
    }

    private void initViews() {
        accountStatusText = findViewById(R.id.accountStatusText);
        statusIndicator = findViewById(R.id.statusIndicator);
        deactivateAccountLayout = findViewById(R.id.deactivateAccountLayout);
        deleteAccountLayout = findViewById(R.id.deleteAccountLayout);
    }

    private void setupListeners() {
        deactivateAccountLayout.setOnClickListener(v -> showDeactivateDialog());
        deleteAccountLayout.setOnClickListener(v -> showDeleteDialogWithValidation());
    }

    private void updateAccountStatus() {
        if (userModel != null) {
            // Check if account is active (you can add a field to UserModel for this)
            accountStatusText.setText(R.string.account_active);
            accountStatusText.setTextColor(ContextCompat.getColor(this, R.color.success));
            statusIndicator.setBackgroundResource(R.drawable.circle_active);
        }
    }

    private void showDeactivateDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.deactivate_account)
                .setMessage(R.string.deactivate_account_message)
                .setPositiveButton(R.string.deactivate, (dialog, which) -> deactivateAccount())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showDeleteDialogWithValidation() {
        android.widget.EditText confirmationInput = new android.widget.EditText(this);
        confirmationInput.setHint(R.string.type_delete_to_confirm);
        confirmationInput.setPadding(50, 30, 50, 30);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.delete_account)
                .setMessage(R.string.delete_account_warning)
                .setView(confirmationInput)
                .setPositiveButton(R.string.delete_forever, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(dialogInterface ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String input = confirmationInput.getText().toString().trim();
                if ("DELETE".equals(input)) {
                    dialog.dismiss();
                    deleteAccount();
                } else {
                    confirmationInput.setError(getString(R.string.please_type_delete));
                    confirmationInput.requestFocus();
                }
            }));

        dialog.show();
    }

    private void deactivateAccount() {
        if (userModel == null || currentUser == null) {
            Toast.makeText(this, R.string.error_user_data_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        // Update user status to deactivated in database
        FirebaseDatabase.getInstance().getReference("Users")
                .child(currentUser.getUid())
                .child("isActive")
                .setValue(false)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.account_deactivated_success, Toast.LENGTH_SHORT).show();
                    // Sign out user
                    auth.signOut();
                    CurrentUser.setUser(null);

                    // Navigate to login
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                    Toast.makeText(this, getString(R.string.failed_deactivate_account) + e.getMessage(),
                                 Toast.LENGTH_LONG).show());
    }

    private void deleteAccount() {
        if (currentUser == null) {
            Toast.makeText(this, R.string.error_user_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // First delete user data from database
        FirebaseDatabase.getInstance().getReference("Users")
                .child(userId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Then delete Firebase Auth account
                    currentUser.delete()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, R.string.account_deleted_success, Toast.LENGTH_SHORT).show();
                                    CurrentUser.setUser(null);

                                    // Navigate to login
                                    Intent intent = new Intent(this, LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(this, getString(R.string.failed_delete_account) +
                                                 (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                                 Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .addOnFailureListener(e ->
                    Toast.makeText(this, getString(R.string.failed_delete_user_data) + e.getMessage(),
                                 Toast.LENGTH_LONG).show());
    }
}
