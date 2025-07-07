package com.example.tradeup_app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tradeup_app.R;
import com.example.tradeup_app.auth.Domain.UserModel;
import com.example.tradeup_app.auth.Helper.CurrentUser;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class DeactivatedAccountActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private UserModel userModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initFirebase();
        showDeactivatedDialog();
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        userModel = CurrentUser.getUser();
    }

    private void showDeactivatedDialog() {
        // Create custom dialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_account_deactivated, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Get buttons from dialog view
        MaterialButton logoutButton = dialogView.findViewById(R.id.logoutButton);
        MaterialButton reactivateButton = dialogView.findViewById(R.id.reactivateButton);

        // Logout button click
        logoutButton.setOnClickListener(v -> {
            dialog.dismiss();
            logoutUser();
        });

        // Reactivate button click
        reactivateButton.setOnClickListener(v -> {
            dialog.dismiss();
            reactivateAccount();
        });

        dialog.show();
    }

    private void logoutUser() {
        // Sign out user
        auth.signOut();
        CurrentUser.setUser(null);

        // Navigate to login
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void reactivateAccount() {
        if (userModel == null || currentUser == null) {
            Toast.makeText(this, "Không thể tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update user status to active in database
        FirebaseDatabase.getInstance().getReference("Users")
                .child(currentUser.getUid())
                .child("deactivated")
                .setValue(false)
                .addOnSuccessListener(aVoid -> {
                    // Update local user model
                    userModel.setDeactivated(false);
                    CurrentUser.setUser(userModel);

                    Toast.makeText(this, "Tài khoản đã được kích hoạt lại thành công!", Toast.LENGTH_SHORT).show();

                    // Navigate to main activity
                    Intent intent = new Intent(this, com.example.tradeup_app.activities.MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Không thể kích hoạt lại tài khoản: " + e.getMessage(),
                                 Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onBackPressed() {
        // Prevent back button from closing the dialog
        // User must choose either logout or reactivate
    }
}
