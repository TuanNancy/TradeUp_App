package com.example.tradeup_app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.example.tradeup_app.R;

public class LoginActivity extends AppCompatActivity {
    private EditText inputEmail, inputPassword;
    private Button btnLogin;
    private TextView textRegister, textForgot;
    private SignInButton btnGoogle;
    private FirebaseAuth mAuth;
    private static final int RC_GOOGLE = 1001;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_login);

        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        textForgot = findViewById(R.id.textForgot);
        textRegister = findViewById(R.id.textRegister);
        btnGoogle = findViewById(R.id.btnGoogleSignIn);

        mAuth = FirebaseAuth.getInstance();

        SimpleTextWatcher watcher = new SimpleTextWatcher() {
            @Override public void onTextChanged() {
                String e = inputEmail.getText().toString().trim();
                String p = inputPassword.getText().toString().trim();
                btnLogin.setEnabled(!e.isEmpty() && !p.isEmpty());
            }
        };
        inputEmail.addTextChangedListener(watcher);
        inputPassword.addTextChangedListener(watcher);

        btnLogin.setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();
            String pass  = inputPassword.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                inputEmail.setError("Invalid email"); return;
            }
            mAuth.signInWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(auth -> {
                        FirebaseUser u = mAuth.getCurrentUser();
                        if (u != null && u.isEmailVerified()) {
                            startActivity(new Intent(this, UserProfileActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Verify your email first", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Login failed: "+e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        textForgot.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        textRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        btnGoogle.setOnClickListener(v -> {
            GoogleSignInOptions opts = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail().build();
            GoogleSignInClient client = GoogleSignIn.getClient(this, opts);
            startActivityForResult(client.getSignInIntent(), RC_GOOGLE);
        });
    }

    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (req == RC_GOOGLE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount acct = task.getResult(ApiException.class);
                AuthCredential cred = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
                mAuth.signInWithCredential(cred)
                        .addOnSuccessListener(a -> {
                            startActivity(new Intent(this, UserProfileActivity.class));
                            finish();
                        });
            } catch (Exception e) {
                Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
