package com.example.tradeup_app.auth;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tradeup_app.auth.Domain.UserModel;
import com.example.tradeup_app.auth.Helper.CurrentUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.SignInButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.example.tradeup_app.R;


public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private SignInButton googleSignInButton;
    private TextView registerLink, forgotPasswordText;

    private FirebaseAuth auth;
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;
    private final int REQ_ONE_TAP = 2; // Can be any value

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        registerLink = findViewById(R.id.registerLink);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);

        // Disable login button initially
        loginButton.setEnabled(false);

        TextWatcher inputWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String email = emailEditText.getText().toString().trim();
                String pass = passwordEditText.getText().toString().trim();
                loginButton.setEnabled(!email.isEmpty() && !pass.isEmpty());
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        emailEditText.addTextChangedListener(inputWatcher);
        passwordEditText.addTextChangedListener(inputWatcher);

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String pass = passwordEditText.getText().toString().trim();

            auth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) {
                                String uid = user.getUid();
                                FirebaseDatabase.getInstance().getReference("Users").child(uid).get()
                                        .addOnSuccessListener(snapshot -> {
                                            if (snapshot.exists()) {
                                                UserModel userModel = snapshot.getValue(UserModel.class);
                                                CurrentUser.setUser(userModel); // Gán vào singleton
                                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                                finish();
                                            }
                                        });
                            }else {
                                Toast.makeText(this, "Please verify your email", Toast.LENGTH_SHORT).show();
                                auth.signOut();
                            }
                        } else {
                            Toast.makeText(this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        googleSignInButton.setOnClickListener(v -> signInWithGoogle());

        registerLink.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );

        forgotPasswordText.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class))
        );

        setupGoogleSignIn();
    }

    private void setupGoogleSignIn() {
        oneTapClient = Identity.getSignInClient(this);
        signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                .setSupported(true)
                                .setServerClientId(getString(R.string.default_web_client_id))
                                .setFilterByAuthorizedAccounts(false)
                                .build())
                .build();
    }

    private void signInWithGoogle() {
        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this, result -> {
                    try {
                        startIntentSenderForResult(
                                result.getPendingIntent().getIntentSender(),
                                REQ_ONE_TAP,
                                null, 0, 0, 0, null);
                    } catch (IntentSender.SendIntentException e) {
                        Toast.makeText(this, "Error starting Google Sign-In: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    Toast.makeText(this, "Google Sign-in Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ONE_TAP) {
            try {
                SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(data);
                String idToken = credential.getGoogleIdToken();
                if (idToken != null) {
                    AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                    auth.signInWithCredential(firebaseCredential)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = auth.getCurrentUser();
                                    if (user == null) {
                                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    String uid = user.getUid();
                                    FirebaseDatabase.getInstance().getReference("Users").child(uid)
                                            .get()
                                            .addOnSuccessListener(snapshot -> {
                                                if (snapshot.exists()) {
                                                    // Đã có user => vào app
                                                    UserModel userModel = snapshot.getValue(UserModel.class);
                                                    if (userModel != null) {
                                                        CurrentUser.setUser(userModel);
                                                    }
                                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                                    finish();
                                                } else {
                                                    // Chưa có => đăng ký nhanh user mới
                                                    String email = user.getEmail() != null ? user.getEmail() : "";
                                                    String username = user.getDisplayName() != null ? user.getDisplayName() : "User";
                                                    String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";

                                                    UserModel newUser = new UserModel(
                                                            uid,
                                                            email,
                                                            username,
                                                            photoUrl,
                                                            "",     // bio
                                                            "",     // contact
                                                            ""      // rating
                                                    );

                                                    FirebaseDatabase.getInstance().getReference("Users").child(uid)
                                                            .setValue(newUser)
                                                            .addOnSuccessListener(unused -> {
                                                                CurrentUser.setUser(newUser);
                                                                Toast.makeText(this, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show();
                                                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                                                finish();
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                Toast.makeText(this, "Lỗi lưu user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                            });
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(this, "Lỗi truy vấn user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            });

                                } else {
                                    Toast.makeText(this, "Firebase Auth failed", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            } catch (Exception e) {
                Toast.makeText(this, "Google Sign-in Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }


}
