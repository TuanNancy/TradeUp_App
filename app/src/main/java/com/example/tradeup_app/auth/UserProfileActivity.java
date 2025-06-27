package com.example.tradeup_app.auth;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build; // Thêm import này
import android.provider.Settings;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.tradeup_app.auth.Domain.UserModel;
import com.example.tradeup_app.auth.Helper.CurrentUser;
import com.example.tradeup_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList; // Thêm import này
import java.util.List;     // Thêm import này
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_IMAGE_PICK = 1001; // Hằng số cho yêu cầu chọn ảnh
    private static final int PERMISSION_REQUEST_CODE_READ_MEDIA = 1002; // Đổi tên hằng số cho rõ ràng hơn

    private static final String CLOUDINARY_UPLOAD_PRESET = "my_profile_upload";

    private ImageView profileImageView;
    private TextView displayNameTextView, ratingTextView;
    private EditText bioEditText, contactEditText;
    private Button updateProfileButton, deactivateAccountButton, deleteAccountButton;
    private TextView userListTextView, backTextView;

    private DatabaseReference userRef;
    private FirebaseUser currentUser;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        initUI();
        initFirebase();

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId);
            loadUserProfile();
            setupListeners();
        } else {
            Toast.makeText(this, "Người dùng chưa đăng nhập. Vui lòng đăng nhập để xem hồ sơ của bạn.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initUI() {
        profileImageView = findViewById(R.id.profileImageView);
        displayNameTextView = findViewById(R.id.displayNameTextView);
        ratingTextView = findViewById(R.id.ratingTextView);
        bioEditText = findViewById(R.id.bioEditText);
        contactEditText = findViewById(R.id.contactEditText);
        updateProfileButton = findViewById(R.id.updateProfileButton);
        deactivateAccountButton = findViewById(R.id.deactivateAccountButton);
        deleteAccountButton = findViewById(R.id.deleteAccountButton);
        userListTextView = findViewById(R.id.userlist);
        backTextView = findViewById(R.id.textView7);
    }

    private void initFirebase() {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    private void setupListeners() {
        profileImageView.setOnClickListener(v -> openImagePicker());
        updateProfileButton.setOnClickListener(v -> showUpdateProfileDialog());
        deactivateAccountButton.setOnClickListener(v -> confirmDeactivateAccount());
        deleteAccountButton.setOnClickListener(v -> confirmDeleteAccount());



        backTextView.setOnClickListener(v -> {
            Intent intent = new Intent(UserProfileActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
    }

    private void loadUserProfile() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserModel user = CurrentUser.getUser();
                if (user != null) {
                    displayNameTextView.setText(user.getUsername());
                    ratingTextView.setText(getString(R.string.rating_format, user.getRating()));
                    bioEditText.setText(user.getBio());
                    contactEditText.setText(user.getContact());

                    Glide.with(UserProfileActivity.this)
                            .load(user.getProfilePic())
                            .placeholder(R.drawable.ic_user)
                            .error(R.drawable.ic_user)
                            .into(profileImageView);
                } else {
                    Toast.makeText(UserProfileActivity.this, "Không tìm thấy dữ liệu người dùng.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserProfileActivity.this, "Không thể tải hồ sơ người dùng: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Mở trình chọn ảnh.
     * Kiểm tra và yêu cầu quyền thích hợp dựa trên phiên bản Android.
     */
    private void openImagePicker() {
        String permissionToRequest;
        boolean granted;
        boolean shouldShowRationale;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (API 33) trở lên
            permissionToRequest = Manifest.permission.READ_MEDIA_IMAGES;
            granted = ContextCompat.checkSelfPermission(this, permissionToRequest) == PackageManager.PERMISSION_GRANTED;
            shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permissionToRequest);
        } else { // Android 12 (API 32) trở xuống
            permissionToRequest = Manifest.permission.READ_EXTERNAL_STORAGE;
            granted = ContextCompat.checkSelfPermission(this, permissionToRequest) == PackageManager.PERMISSION_GRANTED;
            shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permissionToRequest);
        }

        if (!granted) {
            if (shouldShowRationale) {
                // Người dùng đã từ chối quyền nhưng chưa chọn "Don't ask again"
                new AlertDialog.Builder(this)
                        .setTitle("Cần quyền truy cập bộ nhớ")
                        .setMessage("Ứng dụng cần quyền đọc bộ nhớ để chọn ảnh từ thư viện của bạn.")
                        .setPositiveButton("Đồng ý", (dialog, which) -> ActivityCompat.requestPermissions(UserProfileActivity.this,
                                new String[]{permissionToRequest},
                                PERMISSION_REQUEST_CODE_READ_MEDIA))
                        .setNegativeButton("Hủy", (dialog, which) -> Toast.makeText(UserProfileActivity.this, "Quyền truy cập bộ nhớ bị từ chối.", Toast.LENGTH_SHORT).show())
                        .show();
            } else {
                // Người dùng đã từ chối vĩnh viễn (chọn "Don't ask again") hoặc lần đầu tiên yêu cầu
                // Trong trường hợp này, cần hướng dẫn người dùng vào cài đặt
                new AlertDialog.Builder(this)
                        .setTitle("Quyền bị từ chối vĩnh viễn")
                        .setMessage("Quyền truy cập bộ nhớ đã bị từ chối vĩnh viễn. Vui lòng cấp quyền thủ công trong Cài đặt ứng dụng để sử dụng chức năng này.")
                        .setPositiveButton("Đi đến Cài đặt", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        })
                        .setNegativeButton("Hủy", (dialog, which) -> Toast.makeText(UserProfileActivity.this, "Không thể chọn ảnh nếu không có quyền.", Toast.LENGTH_SHORT).show())
                        .show();
            }
        } else {
            // Quyền đã được cấp, mở trình chọn ảnh
            startImagePickerIntent();
        }
    }

    private void startImagePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_CODE_IMAGE_PICK);
    }

    private void showUpdateProfileDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_updateprofile, null);
        EditText editUsername = dialogView.findViewById(R.id.editUsername);
        EditText editBio = dialogView.findViewById(R.id.editBio);
        EditText editContact = dialogView.findViewById(R.id.editContact);

        editUsername.setText(displayNameTextView.getText().toString());
        editBio.setText(bioEditText.getText().toString());
        editContact.setText(contactEditText.getText().toString());

        new AlertDialog.Builder(this)
                .setTitle("Cập nhật Hồ sơ")
                .setView(dialogView)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    String newUsername = editUsername.getText().toString().trim();
                    String newBio = editBio.getText().toString().trim();
                    String newContact = editContact.getText().toString().trim();
                    performProfileUpdate(newUsername, newBio, newContact);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performProfileUpdate(String username, String bio, String contact) {
        Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("username", username);
        updates.put("bio", bio);
        updates.put("contact", contact);

        userRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                displayNameTextView.setText(username);
                bioEditText.setText(bio);
                contactEditText.setText(contact);
                Toast.makeText(this, "Hồ sơ đã được cập nhật thành công!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Không thể cập nhật hồ sơ: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeactivateAccount() {
        showEmailConfirmDialog(() -> {
            userRef.child("deactivated").setValue(true).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Tài khoản đã được hủy kích hoạt thành công.", Toast.LENGTH_SHORT).show();
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(UserProfileActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Không thể hủy kích hoạt tài khoản: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void confirmDeleteAccount() {
        showEmailConfirmDialog(() -> {
            userRef.removeValue().addOnCompleteListener(dbTask -> {
                if (dbTask.isSuccessful()) {
                    currentUser.delete().addOnCompleteListener(authTask -> {
                        if (authTask.isSuccessful()) {
                            Toast.makeText(this, "Tài khoản đã được xóa thành công.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(UserProfileActivity.this, LoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Không thể xóa tài khoản khỏi xác thực: " + authTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    Toast.makeText(this, "Không thể xóa dữ liệu người dùng: " + dbTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showEmailConfirmDialog(Runnable onConfirmed) {
        View view = getLayoutInflater().inflate(R.layout.dialog_confirmemail, null);
        EditText emailInput = view.findViewById(R.id.emailInput);

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận Email")
                .setMessage("Vui lòng nhập email của bạn để xác nhận hành động này.")
                .setView(view)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    String enteredEmail = emailInput.getText().toString().trim();
                    if (currentUser != null && enteredEmail.equals(currentUser.getEmail())) {
                        onConfirmed.run();
                    } else {
                        Toast.makeText(this, "Email không khớp hoặc người dùng chưa đăng nhập.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_IMAGE_PICK && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            uploadImageToCloudinary(selectedImageUri);
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Việc chọn ảnh đã bị hủy.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE_READ_MEDIA) { // Đổi tên hằng số ở đây
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Quyền được cấp, tiếp tục mở trình chọn ảnh
                startImagePickerIntent();
            } else {
                // Quyền bị từ chối
                // Cần kiểm tra lại nếu đây là trường hợp từ chối vĩnh viễn hay chỉ từ chối tạm thời
                String permissionToCheck = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ?
                        Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissionToCheck)) {
                    // Người dùng từ chối nhưng chưa chọn "Don't ask again"
                    Toast.makeText(this, "Quyền truy cập bộ nhớ bị từ chối. Vui lòng cấp quyền để chọn ảnh.", Toast.LENGTH_LONG).show();
                } else {
                    // Người dùng đã từ chối vĩnh viễn (hoặc là lần đầu tiên yêu cầu)
                    new AlertDialog.Builder(this)
                            .setTitle("Quyền bị từ chối vĩnh viễn")
                            .setMessage("Quyền truy cập bộ nhớ đã bị từ chối vĩnh viễn. Vui lòng cấp quyền thủ công trong Cài đặt ứng dụng để sử dụng chức năng này.")
                            .setPositiveButton("Đi đến Cài đặt", (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            })
                            .setNegativeButton("Hủy", (dialog, which) -> Toast.makeText(UserProfileActivity.this, "Không thể chọn ảnh nếu không có quyền.", Toast.LENGTH_SHORT).show())
                            .show();
                }
            }
        }
    }

    private void uploadImageToCloudinary(Uri uri) {
        if (uri == null) {
            Toast.makeText(this, "Không có hình ảnh được chọn để tải lên.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();

        MediaManager.get().upload(uri)
                .unsigned(CLOUDINARY_UPLOAD_PRESET)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        if (imageUrl != null) {
                            userRef.child("profilePic").setValue(imageUrl).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Glide.with(UserProfileActivity.this).load(imageUrl).into(profileImageView);
                                    Toast.makeText(UserProfileActivity.this, "Ảnh đại diện đã được cập nhật!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(UserProfileActivity.this, "Không thể lưu URL ảnh vào cơ sở dữ liệu: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(UserProfileActivity.this, "Tải ảnh lên thất bại: không tìm thấy URL an toàn.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(UserProfileActivity.this, "Tải ảnh lên thất bại: " + error.getDescription(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }
}