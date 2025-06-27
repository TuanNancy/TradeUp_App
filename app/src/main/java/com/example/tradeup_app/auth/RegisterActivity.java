package com.example.tradeup_app.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.example.tradeup_app.R;

public class RegisterActivity extends AppCompatActivity {
    private EditText inputEmail, inputPassword;
    private Button btnRegister;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_register);

        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnRegister = findViewById(R.id.btnRegister);
        mAuth = FirebaseAuth.getInstance();

        btnRegister.setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();
            String pass  = inputPassword.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                inputEmail.setError("Invalid email"); return;
            }
            if (pass.length() < 6) {
                inputPassword.setError("Min 6 chars"); return;
            }
            mAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(r -> {
                        r.getUser().sendEmailVerification();
                        Toast.makeText(this, "Registered. Verify email.", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Reg failed: "+e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }
}
