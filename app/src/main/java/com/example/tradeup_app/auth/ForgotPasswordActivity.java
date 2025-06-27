package com.example.tradeup_app.auth;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.example.tradeup_app.R;

public class ForgotPasswordActivity extends AppCompatActivity {
    private EditText inputEmail;
    private Button btnReset;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_forgot_password);

        inputEmail = findViewById(R.id.inputEmail);
        btnReset   = findViewById(R.id.btnReset);
        mAuth      = FirebaseAuth.getInstance();

        btnReset.setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();
            if (email.isEmpty()) {
                inputEmail.setError("Required"); return;
            }
            mAuth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(u ->
                            Toast.makeText(this, "Check your email", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed: "+e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }
}
