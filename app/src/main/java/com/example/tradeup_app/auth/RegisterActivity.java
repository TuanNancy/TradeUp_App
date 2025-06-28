package com.example.tradeup_app.auth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.tradeup_app.R;
import com.example.tradeup_app.auth.Domain.UserModel;
import com.example.tradeup_app.auth.Helper.CurrentUser;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText usernameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private TextInputLayout usernameInputLayout, emailInputLayout, passwordInputLayout, confirmPasswordInputLayout;
    private ImageView profileImageView;
    private CheckBox termsCheckBox;
    private MaterialButton registerButton, googleSignUpButton;
    private TextView loginLink;

    private FirebaseAuth auth;
    private FirebaseStorage storage;
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;

    private Uri selectedImageUri;
    private final int PICK_IMAGE_REQUEST = 1001;
    private final int PERMISSION_REQUEST_CODE = 100;
    private final int REQ_ONE_TAP = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initFirebase();
        initViews();
        setupListeners();
        setupGoogleSignIn();
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    private void initViews() {
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);

        usernameInputLayout = findViewById(R.id.usernameInputLayout);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);

        profileImageView = findViewById(R.id.profileImageView);
        termsCheckBox = findViewById(R.id.termsCheckBox);
        registerButton = findViewById(R.id.registerButton);
        googleSignUpButton = findViewById(R.id.googleSignUpButton);
        loginLink = findViewById(R.id.loginLink);

        // Disable register button initially
        registerButton.setEnabled(false);
    }

    private void setupListeners() {
        // Profile image click
        profileImageView.setOnClickListener(v -> selectImage());

        // Text watchers for validation
        usernameEditText.addTextChangedListener(createTextWatcher());
        emailEditText.addTextChangedListener(createTextWatcher());
        passwordEditText.addTextChangedListener(createTextWatcher());
        confirmPasswordEditText.addTextChangedListener(createTextWatcher());

        // Terms checkbox
        termsCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> validateForm());

        // Buttons
        registerButton.setOnClickListener(v -> performRegistration());
        googleSignUpButton.setOnClickListener(v -> signUpWithGoogle());
        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private TextWatcher createTextWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateForm();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
    }

    private void validateForm() {
        boolean isUsernameValid = validateUsername();
        boolean isEmailValid = validateEmail();
        boolean isPasswordValid = validatePassword();
        boolean isConfirmPasswordValid = validateConfirmPassword();
        boolean isTermsAccepted = termsCheckBox.isChecked();

        registerButton.setEnabled(isUsernameValid && isEmailValid && isPasswordValid &&
                                 isConfirmPasswordValid && isTermsAccepted);
    }

    private boolean validateUsername() {
        String username = usernameEditText.getText() != null ?
            usernameEditText.getText().toString().trim() : "";
        if (username.isEmpty()) {
            usernameInputLayout.setError(null);
            return false;
        } else if (username.length() < 3) {
            usernameInputLayout.setError("Username must be at least 3 characters");
            return false;
        } else {
            usernameInputLayout.setError(null);
            return true;
        }
    }

    private boolean validateEmail() {
        String email = emailEditText.getText() != null ?
            emailEditText.getText().toString().trim() : "";
        if (email.isEmpty()) {
            emailInputLayout.setError(null);
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Invalid email format");
            return false;
        } else {
            emailInputLayout.setError(null);
            return true;
        }
    }

    private boolean validatePassword() {
        String password = passwordEditText.getText() != null ?
            passwordEditText.getText().toString() : "";
        if (password.isEmpty()) {
            passwordInputLayout.setError(null);
            return false;
        } else if (password.length() < 6) {
            passwordInputLayout.setError("Password must be at least 6 characters");
            return false;
        } else {
            passwordInputLayout.setError(null);
            return true;
        }
    }

    private boolean validateConfirmPassword() {
        String password = passwordEditText.getText() != null ?
            passwordEditText.getText().toString() : "";
        String confirmPassword = confirmPasswordEditText.getText() != null ?
            confirmPasswordEditText.getText().toString() : "";

        if (confirmPassword.isEmpty()) {
            confirmPasswordInputLayout.setError(null);
            return false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordInputLayout.setError("Passwords do not match");
            return false;
        } else {
            confirmPasswordInputLayout.setError(null);
            return true;
        }
    }

    private void selectImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else {
            openImagePicker();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            Glide.with(this)
                    .load(selectedImageUri)
                    .placeholder(R.drawable.ic_user_placeholder)
                    .into(profileImageView);
        } else if (requestCode == REQ_ONE_TAP && resultCode == RESULT_OK) {
            handleGoogleSignInResult(data);
        }
    }

    private void performRegistration() {
        String username = usernameEditText.getText() != null ?
            usernameEditText.getText().toString().trim() : "";
        String email = emailEditText.getText() != null ?
            emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ?
            passwordEditText.getText().toString() : "";

        setLoading(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            sendEmailVerification(user, username, email);
                        }
                    } else {
                        setLoading(false);
                        String errorMessage = task.getException() != null ?
                            task.getException().getMessage() : "Registration failed";
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void sendEmailVerification(FirebaseUser user, String username, String email) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        // Create user profile after sending verification email
                        createUserProfile(user.getUid(), username, email, "");
                        showEmailVerificationDialog();
                    } else {
                        Toast.makeText(this, "Failed to send verification email", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createUserProfile(String uid, String username, String email, String photoUrl) {
        if (selectedImageUri != null) {
            uploadImageAndCreateProfile(uid, username, email);
        } else {
            saveUserProfile(uid, username, email, photoUrl);
        }
    }

    private void uploadImageAndCreateProfile(String uid, String username, String email) {
        StorageReference storageRef = storage.getReference()
                .child("profile_images")
                .child(uid + ".jpg");

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                    storageRef.getDownloadUrl().addOnSuccessListener(uri ->
                        saveUserProfile(uid, username, email, uri.toString())))
                .addOnFailureListener(e -> {
                    // Save profile without image if upload fails
                    saveUserProfile(uid, username, email, "");
                });
    }

    private void saveUserProfile(String uid, String username, String email, String photoUrl) {
        UserModel userModel = new UserModel(
                uid,
                email,
                username,
                photoUrl,
                "", // bio
                "", // contact
                "0" // rating
        );

        FirebaseDatabase.getInstance().getReference("Users").child(uid)
                .setValue(userModel)
                .addOnSuccessListener(aVoid -> CurrentUser.setUser(userModel))
                .addOnFailureListener(e ->
                    Toast.makeText(this, "Failed to save user profile", Toast.LENGTH_SHORT).show());
    }

    private void showEmailVerificationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Verify Your Email")
                .setMessage("We've sent a verification email to your address. Please check your email and click the verification link to activate your account.")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Sign out user until they verify their email
                    auth.signOut();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setCancelable(false)
                .show();
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

    private void signUpWithGoogle() {
        setLoading(true);
        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this, result -> {
                    try {
                        startIntentSenderForResult(
                                result.getPendingIntent().getIntentSender(),
                                REQ_ONE_TAP,
                                null, 0, 0, 0, null);
                    } catch (Exception e) {
                        setLoading(false);
                        Toast.makeText(this, "Error starting Google Sign-Up", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    setLoading(false);
                    Toast.makeText(this, "Google Sign-up not available", Toast.LENGTH_SHORT).show();
                });
    }

    private void handleGoogleSignInResult(Intent data) {
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
                                    handleGoogleSignUpSuccess(user);
                                }
                            } else {
                                Toast.makeText(this, "Google authentication failed", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        } catch (Exception e) {
            Toast.makeText(this, "Google Sign-up failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleGoogleSignUpSuccess(FirebaseUser user) {
        String uid = user.getUid();
        String email = user.getEmail() != null ? user.getEmail() : "";
        String username = user.getDisplayName() != null ? user.getDisplayName() : "User";
        String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";

        // Check if user already exists
        FirebaseDatabase.getInstance().getReference("Users").child(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        // User already exists, sign in
                        UserModel userModel = snapshot.getValue(UserModel.class);
                        if (userModel != null) {
                            CurrentUser.setUser(userModel);
                        }
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        // New user, create profile
                        saveUserProfile(uid, username, email, photoUrl);
                        Toast.makeText(this, "Welcome to TradeUp!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e ->
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show());
    }

    private void setLoading(boolean loading) {
        registerButton.setEnabled(!loading);
        googleSignUpButton.setEnabled(!loading);
        usernameEditText.setEnabled(!loading);
        emailEditText.setEnabled(!loading);
        passwordEditText.setEnabled(!loading);
        confirmPasswordEditText.setEnabled(!loading);
        termsCheckBox.setEnabled(!loading);

        if (loading) {
            registerButton.setText(R.string.creating_account);
        } else {
            registerButton.setText(R.string.create_account);
        }
    }
}
