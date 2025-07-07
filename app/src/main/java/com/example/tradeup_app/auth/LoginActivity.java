package com.example.tradeup_app.auth;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tradeup_app.auth.Domain.UserModel;
import com.example.tradeup_app.auth.Helper.CurrentUser;
import com.example.tradeup_app.activities.MainActivity;
import com.example.tradeup_app.services.BackgroundMessageService;
import com.example.tradeup_app.utils.NetworkUtils;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;
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
    private ProgressBar loadingProgressBar;

    private FirebaseAuth auth;
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;
    private final int REQ_ONE_TAP = 2;

    // ✅ Add timeout handling
    private static final int LOGIN_TIMEOUT_MS = 15000; // 15 seconds
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

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

        // ✅ Add loading progress bar for better UX
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        if (loadingProgressBar == null) {
            // Create programmatically if not in layout
            loadingProgressBar = new ProgressBar(this);
            loadingProgressBar.setVisibility(View.GONE);
        }

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
        // ✅ Kiểm tra kết nối mạng trước khi đăng nhập
        if (!NetworkUtils.isNetworkAvailable(this)) {
            String networkType = NetworkUtils.getNetworkType(this);
            showNetworkErrorDialog(networkType);
            return;
        }

        String email = emailEditText.getText() != null ?
            emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ?
            passwordEditText.getText().toString() : "";

        // Validate inputs before calling Firebase
        if (email.isEmpty()) {
            emailInputLayout.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Please enter a valid email");
            emailEditText.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordInputLayout.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordInputLayout.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return;
        }

        // Clear any previous errors
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);

        setLoading(true);

        // ✅ Set timeout for login operation
        setupLoginTimeout();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    clearLoginTimeout();
                    setLoading(false);

                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                // ✅ Load user data with faster navigation
                                loadUserDataAndNavigateFast(user.getUid());
                            } else {
                                showEmailVerificationDialog(user);
                            }
                        }
                    } else {
                        String errorMessage = getFirebaseErrorMessage(task.getException());
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ✅ Hiển thị dialog lỗi mạng với thông tin chi tiết
    private void showNetworkErrorDialog(String networkType) {
        String message = "Không thể kết nối internet.\n\n" +
                        "Loại kết nối hiện tại: " + networkType + "\n\n" +
                        "Vui lòng kiểm tra:\n" +
                        "• Kết nối WiFi hoặc Mobile Data\n" +
                        "• Cài đặt mạng của thiết bị\n" +
                        "• Firewall hoặc proxy";

        new AlertDialog.Builder(this)
                .setTitle("Lỗi kết nối mạng")
                .setMessage(message)
                .setPositiveButton("Thử lại", (dialog, which) -> {
                    // Kiểm tra lại kết nối
                    if (NetworkUtils.isNetworkAvailable(this)) {
                        Toast.makeText(this, "Kết nối đã được khôi phục!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Vẫn chưa có kết nối internet", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cài đặt", (dialog, which) -> {
                    // Mở cài đặt mạng
                    try {
                        Intent intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, "Không thể mở cài đặt", Toast.LENGTH_SHORT).show();
                    }
                })
                .setCancelable(false)
                .show();
    }

    // ✅ Add timeout handling for login operations
    private void setupLoginTimeout() {
        timeoutRunnable = () -> {
            setLoading(false);
            Toast.makeText(this, "Login timeout. Please check your internet connection and try again.", Toast.LENGTH_LONG).show();
        };
        timeoutHandler.postDelayed(timeoutRunnable, LOGIN_TIMEOUT_MS);
    }

    private void clearLoginTimeout() {
        if (timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
    }

    // ✅ Improved error handling
    private String getFirebaseErrorMessage(Exception exception) {
        if (exception == null) return "Login failed";

        String message = exception.getMessage();
        if (message != null) {
            if (message.contains("INVALID_EMAIL")) {
                return "Invalid email address";
            } else if (message.contains("WRONG_PASSWORD")) {
                return "Incorrect password";
            } else if (message.contains("USER_NOT_FOUND")) {
                return "No account found with this email";
            } else if (message.contains("USER_DISABLED")) {
                return "This account has been disabled";
            } else if (message.contains("TOO_MANY_REQUESTS")) {
                return "Too many failed attempts. Please try again later";
            } else if (message.contains("NETWORK_ERROR")) {
                return "Network error. Please check your internet connection";
            }
        }
        return "Login failed. Please try again";
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
                            loadUserDataAndNavigateFast(user.getUid());
                        } else {
                            Toast.makeText(this, R.string.email_not_verified, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();

        auth.signOut(); // Sign out unverified user
    }

    // ✅ Optimized user data loading with faster navigation
    private void loadUserDataAndNavigateFast(String uid) {
        FirebaseDatabase.getInstance().getReference("Users").child(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        UserModel userModel = snapshot.getValue(UserModel.class);
                        if (userModel != null) {
                            // Set user data in singleton
                            CurrentUser.setUser(userModel);

                            // Check if account is deactivated
                            if (userModel.isDeactivated()) {
                                // Navigate to deactivated account screen
                                Intent intent = new Intent(this, DeactivatedAccountActivity.class);
                                startActivity(intent);
                                finish();
                                return;
                            }

                            // ✅ Navigate immediately, start service in background after navigation
                            navigateToMainActivity();

                            // Start BackgroundMessageService after navigation to avoid blocking UI
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                BackgroundMessageService.startService(this);
                            }, 500); // Delay 500ms to ensure smooth navigation
                        } else {
                            handleCorruptedUserData();
                        }
                    } else {
                        handleMissingUserProfile();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user data. Please login again.", Toast.LENGTH_SHORT).show();
                    auth.signOut();
                });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        // ✅ Add flags for smoother navigation
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        // ✅ Add transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void handleCorruptedUserData() {
        Toast.makeText(this, "Please complete your profile", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleMissingUserProfile() {
        Toast.makeText(this, "Please complete your profile", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void checkUserLoginStatus() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            // ✅ Show loading indicator for auto-login
            setLoading(true);
            loadUserDataAndNavigateFromLogin(currentUser.getUid());
        }
    }

    private void loadUserDataAndNavigateFromLogin(String uid) {
        FirebaseDatabase.getInstance().getReference("Users").child(uid).get()
                .addOnSuccessListener(snapshot -> {
                    setLoading(false);
                    if (snapshot.exists()) {
                        UserModel userModel = snapshot.getValue(UserModel.class);
                        if (userModel != null) {
                            CurrentUser.setUser(userModel);

                            // Check if account is deactivated
                            if (userModel.isDeactivated()) {
                                // Navigate to deactivated account screen
                                Intent intent = new Intent(this, DeactivatedAccountActivity.class);
                                startActivity(intent);
                                finish();
                                return;
                            }

                            navigateToMainActivity();

                            // Start service after navigation
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                BackgroundMessageService.startService(this);
                            }, 500);
                        } else {
                            handleCorruptedUserData();
                        }
                    } else {
                        handleMissingUserProfile();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
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
                .setAutoSelectEnabled(true)
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
                        android.util.Log.e("LoginActivity", "Error starting Google Sign In", e);
                        Toast.makeText(this, R.string.error_starting_google_signin, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    setLoading(false);
                    android.util.Log.e("LoginActivity", "Google Sign In failed", e);

                    // Try fallback method if One Tap fails
                    signInWithGoogleFallback();
                });
    }

    // Fallback method using traditional Google Sign In
    private void signInWithGoogleFallback() {
        try {
            // Use GoogleSignInOptions as fallback
            GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(
                    GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();

            GoogleSignInClient googleSignInClient =
                GoogleSignIn.getClient(this, gso);

            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, 9001);

        } catch (Exception e) {
            android.util.Log.e("LoginActivity", "Fallback Google Sign In failed", e);
            Toast.makeText(this, R.string.google_signin_not_available, Toast.LENGTH_SHORT).show();
        }
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
        } else if (requestCode == 9001) {
            // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
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
                0 // rating
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

    // Handle fallback Google Sign In result
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        setLoading(false);
        try {
            GoogleSignInAccount account = completedTask.getResult(com.google.android.gms.common.api.ApiException.class);
            if (account != null) {
                String idToken = account.getIdToken();
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
                                    android.util.Log.e("LoginActivity", "Firebase auth failed", task.getException());
                                    Toast.makeText(this, R.string.google_authentication_failed, Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    android.util.Log.e("LoginActivity", "ID Token is null");
                    Toast.makeText(this, R.string.google_signin_failed, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (com.google.android.gms.common.api.ApiException e) {
            android.util.Log.e("LoginActivity", "Google Sign In failed with status code: " + e.getStatusCode(), e);

            // Handle specific error codes
            switch (e.getStatusCode()) {
                case 10: // DEVELOPER_ERROR
                    Toast.makeText(this, "Google Sign In configuration error. Please check SHA-1 fingerprint.", Toast.LENGTH_LONG).show();
                    break;
                case 12500: // SIGN_IN_REQUIRED
                    Toast.makeText(this, "Please sign in to your Google account first.", Toast.LENGTH_SHORT).show();
                    break;
                case 7: // NETWORK_ERROR
                    Toast.makeText(this, "Network error. Please check your internet connection.", Toast.LENGTH_SHORT).show();
                    break;
                case 12501: // SIGN_IN_CANCELLED
                    Toast.makeText(this, "Google Sign In was cancelled.", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(this, "Google Sign In failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
                    break;
            }
        } catch (Exception e) {
            android.util.Log.e("LoginActivity", "Google Sign In failed", e);
            Toast.makeText(this, R.string.google_signin_failed, Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ Improved loading state management
    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        googleSignInButton.setEnabled(!loading);
        emailEditText.setEnabled(!loading);
        passwordEditText.setEnabled(!loading);

        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }

        if (loading) {
            loginButton.setText(R.string.signing_in);
        } else {
            loginButton.setText(R.string.sign_in);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearLoginTimeout();
    }
}
