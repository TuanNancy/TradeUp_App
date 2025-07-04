package com.example.tradeup_app.auth;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tradeup_app.auth.Domain.UserModel;
import com.example.tradeup_app.auth.Helper.CurrentUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.example.tradeup_app.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailEditText, passwordEditText;
    private TextInputLayout emailInputLayout, passwordInputLayout;
    private MaterialButton loginButton, googleSignInButton;
    private TextView registerLink, forgotPasswordText;

    private FirebaseAuth auth;
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;
    private final int REQ_ONE_TAP = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        // Initialize views first
        initViews();
        setupListeners();
        setupGoogleSignIn();

        // Then check if user is logged in
        checkUserLoginStatus();
    }

    private void initViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        emailInputLayout = (TextInputLayout) emailEditText.getParent().getParent();
        passwordInputLayout = (TextInputLayout) passwordEditText.getParent().getParent();
        loginButton = findViewById(R.id.loginButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        registerLink = findViewById(R.id.registerLink);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);

        // Disable login button initially
        loginButton.setEnabled(false);
    }

    private void setupListeners() {
        // Email validation
        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateEmail();
                validateForm();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Password validation
        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePassword();
                validateForm();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loginButton.setOnClickListener(v -> performLogin());
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
        registerLink.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        forgotPasswordText.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void validateEmail() {
        String email = emailEditText.getText() != null ?
            emailEditText.getText().toString().trim() : "";
        if (email.isEmpty()) {
            emailInputLayout.setError(null);
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError(getString(R.string.invalid_email));
        } else {
            emailInputLayout.setError(null);
        }
    }

    private void validatePassword() {
        String password = passwordEditText.getText() != null ?
            passwordEditText.getText().toString() : "";
        if (password.isEmpty()) {
            passwordInputLayout.setError(null);
        } else if (password.length() < 6) {
            passwordInputLayout.setError(getString(R.string.password_too_short));
        } else {
            passwordInputLayout.setError(null);
        }
    }

    private void validateForm() {
        String email = emailEditText.getText() != null ?
            emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ?
            passwordEditText.getText().toString() : "";

        boolean isEmailValid = !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
        boolean isPasswordValid = password.length() >= 6;

        loginButton.setEnabled(isEmailValid && isPasswordValid);
    }

    private void performLogin() {
        String email = emailEditText.getText() != null ?
            emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ?
            passwordEditText.getText().toString() : "";

        setLoading(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                loadUserDataAndNavigate(user.getUid());
                            } else {
                                showEmailVerificationDialog(user);
                            }
                        }
                    } else {
                        String errorMessage = task.getException() != null ?
                            task.getException().getMessage() : "Login failed";
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showEmailVerificationDialog(FirebaseUser user) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.email_verification_required)
                .setMessage(R.string.please_verify_email)
                .setPositiveButton(R.string.resend, (dialog, which) ->
                    user.sendEmailVerification()
                            .addOnSuccessListener(aVoid ->
                                Toast.makeText(this, R.string.verification_email_sent, Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                Toast.makeText(this, R.string.failed_send_verification, Toast.LENGTH_SHORT).show()))
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.ive_verified, (dialog, which) -> {
                    // Reload user and check verification status
                    user.reload().addOnCompleteListener(reloadTask -> {
                        if (user.isEmailVerified()) {
                            loadUserDataAndNavigate(user.getUid());
                        } else {
                            Toast.makeText(this, R.string.email_not_verified, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();

        auth.signOut(); // Sign out unverified user
    }

    private void loadUserDataAndNavigate(String uid) {
        FirebaseDatabase.getInstance().getReference("Users").child(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        UserModel userModel = snapshot.getValue(UserModel.class);
                        if (userModel != null) {
                            // Set user data in singleton before navigating
                            CurrentUser.setUser(userModel);
                            // Now navigate to MainActivity
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        } else {
                            // User exists but data is corrupted, redirect to profile setup
                            Toast.makeText(this, "Please complete your profile", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, UserProfileActivity.class));
                            finish();
                        }
                    } else {
                        // User authenticated but no profile data exists
                        Toast.makeText(this, "Please complete your profile", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, UserProfileActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    // Failed to load user data, stay on login screen
                    Toast.makeText(this, "Failed to load user data. Please login again.", Toast.LENGTH_SHORT).show();
                    auth.signOut();
                });
    }

    private void checkUserLoginStatus() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            // User is logged in and verified, load user data and navigate
            loadUserDataAndNavigateFromLogin(currentUser.getUid());
        }
        // If no user or not verified, stay on login screen
    }

    private void loadUserDataAndNavigateFromLogin(String uid) {
        FirebaseDatabase.getInstance().getReference("Users").child(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        UserModel userModel = snapshot.getValue(UserModel.class);
                        if (userModel != null) {
                            // Set user data in singleton before navigating
                            CurrentUser.setUser(userModel);
                            // Now navigate to MainActivity
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        } else {
                            // User exists but data is corrupted, redirect to profile setup
                            Toast.makeText(this, "Please complete your profile", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, UserProfileActivity.class));
                            finish();
                        }
                    } else {
                        // User authenticated but no profile data exists
                        Toast.makeText(this, "Please complete your profile", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, UserProfileActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    // Failed to load user data, stay on login screen
                    Toast.makeText(this, "Failed to load user data. Please login again.", Toast.LENGTH_SHORT).show();
                    auth.signOut();
                });
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
        setLoading(true);
        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this, result -> {
                    try {
                        startIntentSenderForResult(
                                result.getPendingIntent().getIntentSender(),
                                REQ_ONE_TAP,
                                null, 0, 0, 0, null);
                    } catch (IntentSender.SendIntentException e) {
                        setLoading(false);
                        Toast.makeText(this, R.string.error_starting_google_signin, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    setLoading(false);
                    Toast.makeText(this, R.string.google_signin_not_available, Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ONE_TAP) {
            setLoading(false);
            try {
                SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(data);
                String idToken = credential.getGoogleIdToken();
                if (idToken != null) {
                    setLoading(true);
                    AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                    auth.signInWithCredential(firebaseCredential)
                            .addOnCompleteListener(task -> {
                                setLoading(false);
                                if (task.isSuccessful()) {
                                    FirebaseUser user = auth.getCurrentUser();
                                    if (user != null) {
                                        handleGoogleSignInSuccess(user);
                                    }
                                } else {
                                    Toast.makeText(this, R.string.google_authentication_failed, Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            } catch (Exception e) {
                Toast.makeText(this, R.string.google_signin_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleGoogleSignInSuccess(FirebaseUser user) {
        String uid = user.getUid();
        FirebaseDatabase.getInstance().getReference("Users").child(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        // Existing user
                        UserModel userModel = snapshot.getValue(UserModel.class);
                        if (userModel != null) {
                            CurrentUser.setUser(userModel);
                        }
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        // New user - create profile
                        createGoogleUserProfile(user);
                    }
                })
                .addOnFailureListener(e ->
                    Toast.makeText(this, R.string.failed_load_user_data, Toast.LENGTH_SHORT).show());
    }

    private void createGoogleUserProfile(FirebaseUser user) {
        String uid = user.getUid();
        String email = user.getEmail() != null ? user.getEmail() : "";
        String username = user.getDisplayName() != null ? user.getDisplayName() : "User";
        String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";

        UserModel newUser = new UserModel(
                uid,
                email,
                username,
                photoUrl,
                "", // bio
                "", // contact
                "0" // rating
        );

        FirebaseDatabase.getInstance().getReference("Users").child(uid)
                .setValue(newUser)
                .addOnSuccessListener(unused -> {
                    CurrentUser.setUser(newUser);
                    Toast.makeText(this, R.string.welcome_to_tradeup, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                    Toast.makeText(this, R.string.failed_save_user_profile, Toast.LENGTH_SHORT).show());
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        googleSignInButton.setEnabled(!loading);
        emailEditText.setEnabled(!loading);
        passwordEditText.setEnabled(!loading);

        if (loading) {
            loginButton.setText(R.string.signing_in);
        } else {
            loginButton.setText(R.string.sign_in);
        }
    }
}
