package com.example.tradeup_app.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.text.TextWatcher;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Patterns;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.AuthResult;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import androidx.appcompat.app.AlertDialog;
import android.widget.EditText;
import android.content.DialogInterface;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tradeup_app.auth.Helper.FileUtils;
import com.example.tradeup_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {

    EditText emailEditText, passwordEditText, confirmPasswordEditText, usernameEditText;
    Button registerButton;
    ImageView profileImageView;
    ProgressBar progressBar;
    GoogleSignInClient mGoogleSignInClient;
    private final int RC_SIGN_IN = 9001;

    FirebaseAuth mAuth;
    Uri selectedImageUri;

    private final int PICK_IMAGE_REQUEST = 1001;
    private final String CLOUDINARY_UPLOAD_URL = "https://api.cloudinary.com/v1_1/YOUR_CLOUD_NAME/image/upload";
    private final String CLOUDINARY_UPLOAD_PRESET = "YOUR_UPLOAD_PRESET";

    private void validateInputsRealtime() {
        boolean isValid = true;

        // Email
        String email = emailEditText.getText().toString().trim();
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Email không hợp lệ");
            emailEditText.setBackgroundResource(R.drawable.input_error_background);
            isValid = false;
        } else {
            emailEditText.setError(null);
            emailEditText.setBackgroundResource(android.R.drawable.edit_text);
        }

        // Username
        String username = usernameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(username)) {
            usernameEditText.setError("Không được để trống tên người dùng");
            usernameEditText.setBackgroundResource(R.drawable.input_error_background);
            isValid = false;
        } else {
            usernameEditText.setError(null);
            usernameEditText.setBackgroundResource(android.R.drawable.edit_text);
        }

        // Password
        String password = passwordEditText.getText().toString();
        if (password.length() < 6) {
            passwordEditText.setError("Mật khẩu phải ít nhất 6 ký tự");
            passwordEditText.setBackgroundResource(R.drawable.input_error_background);
            isValid = false;
        } else {
            passwordEditText.setError(null);
            passwordEditText.setBackgroundResource(android.R.drawable.edit_text);
        }

        // Confirm password
        String confirmPassword = confirmPasswordEditText.getText().toString();
        if (!confirmPassword.equals(password)) {
            confirmPasswordEditText.setError("Mật khẩu nhập lại không khớp");
            confirmPasswordEditText.setBackgroundResource(R.drawable.input_error_background);
            isValid = false;
        } else {
            confirmPasswordEditText.setError(null);
            confirmPasswordEditText.setBackgroundResource(android.R.drawable.edit_text);
        }

        registerButton.setEnabled(isValid);
    }
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        showLoading();

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    hideLoading();
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserInDatabase(user);
                        }
                    } else {
                        Toast.makeText(this, "Google Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
    private void checkUserInDatabase(FirebaseUser user) {
        FirebaseDatabase.getInstance().getReference("Users").child(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        // Đã có user => vào app luôn
                        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        // Chưa có user => lưu user mới vào database
                        String email = user.getEmail() != null ? user.getEmail() : "";
                        String username = user.getDisplayName() != null ? user.getDisplayName() : "User";
                        String profilePic = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";

                        saveUserToDatabase(user.getUid(), email, username, profilePic);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi kiểm tra user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void askUserToSetPassword(FirebaseUser user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Đặt mật khẩu cho tài khoản");

        final EditText input = new EditText(this);
        input.setHint("Nhập mật khẩu (ít nhất 6 ký tự)");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Xác nhận", null);
        builder.setNegativeButton("Hủy", (dialog, which) -> {
            mAuth.signOut();
            mGoogleSignInClient.signOut();
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String password = input.getText().toString().trim();
                if (password.length() < 6) {
                    input.setError("Mật khẩu phải ít nhất 6 ký tự");
                    return;
                }

                showLoading();
                user.updatePassword(password)
                        .addOnCompleteListener(task -> {
                            hideLoading();
                            if (task.isSuccessful()) {
                                // Lưu user vào database với username tạm là email trước, ảnh rỗng
                                saveUserToDatabase(
                                        user.getUid(),
                                        user.getEmail() != null ? user.getEmail() : "",
                                        user.getEmail() != null ? user.getEmail().split("@")[0] : "User",
                                        ""
                                );
                                dialog.dismiss();
                                Toast.makeText(this, "Đặt mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Lỗi đặt mật khẩu: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            });
        });

        dialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        profileImageView = findViewById(R.id.profileImageView); // ← Dòng này bị thiếu

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        registerButton = findViewById(R.id.registerButton);

        progressBar = findViewById(R.id.progressBar);

        // Khởi đầu disable nút đăng ký
        registerButton.setEnabled(false);

        // Định nghĩa TextWatcher để validate realtime
        TextWatcher validatingWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateInputsRealtime();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        // Đăng ký TextWatcher cho các EditText
        usernameEditText.addTextChangedListener(validatingWatcher);
        emailEditText.addTextChangedListener(validatingWatcher);
        passwordEditText.addTextChangedListener(validatingWatcher);
        confirmPasswordEditText.addTextChangedListener(validatingWatcher);

        profileImageView.setOnClickListener(v -> openImageChooser());

        registerButton.setOnClickListener(v -> registerUser());
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // lấy từ google-services.json
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.googleSignInButton).setOnClickListener(v -> signInWithGoogle());
        findViewById(R.id.loginLink).setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish(); // kết thúc RegisterActivity để không quay lại bằng nút back
        });

    }


    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(Exception.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Phần chọn ảnh vẫn giữ nguyên
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            profileImageView.setImageURI(selectedImageUri);
        }
    }

    private void registerUser() {
        String email = emailEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        if (TextUtils.isEmpty(username)) {
            usernameEditText.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.requestFocus();
            return;
        }
        if (password.length() < 6) {
            passwordEditText.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.requestFocus();
            return;
        }

        showLoading();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        user.sendEmailVerification();
                        if (selectedImageUri != null) {
                            uploadToCloudinary(selectedImageUri, imageUrl -> {
                                saveUserToDatabase(user.getUid(), email, username, imageUrl);
                            });
                        } else {
                            saveUserToDatabase(user.getUid(), email, username, "");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void uploadToCloudinary(Uri imageUri, CloudinaryCallback callback) {
        try {
            File file = FileUtils.getFile(this, imageUri);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(),
                            RequestBody.create(file, MediaType.parse("image/*")))
                    .addFormDataPart("upload_preset", CLOUDINARY_UPLOAD_PRESET)
                    .build();

            Request request = new Request.Builder()
                    .url(CLOUDINARY_UPLOAD_URL)
                    .post(requestBody)
                    .build();

            new OkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        hideLoading();
                        Toast.makeText(RegisterActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body().string();
                    try {
                        JSONObject json = new JSONObject(body);
                        String imageUrl = json.getString("secure_url");
                        runOnUiThread(() -> callback.onUploaded(imageUrl));
                    } catch (JSONException e) {
                        runOnUiThread(() -> {
                            hideLoading();
                            Toast.makeText(RegisterActivity.this, "Upload error", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        } catch (Exception e) {
            hideLoading();
            Toast.makeText(this, "Error uploading: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserToDatabase(String uid, String email, String username, String profileUrl) {
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("uid", uid);
        userMap.put("email", email);
        userMap.put("username", username);
        userMap.put("profilePic", profileUrl);
        userMap.put("bio", "");
        userMap.put("contact", "");
        userMap.put("rating", 0);

        FirebaseDatabase.getInstance().getReference("Users")
                .child(uid)
                .setValue(userMap)
                .addOnSuccessListener(unused -> {
                    hideLoading();
                    Toast.makeText(this, "Registered! Please verify email.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    Toast.makeText(this, "DB error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        registerButton.setEnabled(false);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        registerButton.setEnabled(true);
    }

    private interface CloudinaryCallback {
        void onUploaded(String imageUrl);
    }
}
