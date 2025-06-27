package com.example.tradeup_app.auth;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import com.example.tradeup_app.R;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;

    private ImageView avatar;
    private EditText editDisplayName, editBio;
    private Button btnUpdate, btnLogout, btnDelete;

    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> pickImage;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_user_profile);

        mAuth = FirebaseAuth.getInstance();
        user  = mAuth.getCurrentUser();
        db    = FirebaseFirestore.getInstance();

        avatar          = findViewById(R.id.imageProfile);
        editDisplayName = findViewById(R.id.editDisplayName);
        editBio         = findViewById(R.id.editBio);
        btnUpdate       = findViewById(R.id.btnUpdate);
        btnLogout       = findViewById(R.id.btnLogout);
        btnDelete       = findViewById(R.id.btnDeleteAccount);

        if (user != null) {
            editDisplayName.setText(user.getDisplayName());
            // load bio from Firestore
            db.collection("users").document(user.getUid())
                    .get().addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            editBio.setText(doc.getString("bio"));
                        }
                    });
        }

        pickImage = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), res -> {
                    if (res.getResultCode()==RESULT_OK && res.getData()!=null) {
                        selectedImageUri = res.getData().getData();
                        try {
                            Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                            avatar.setImageBitmap(bmp);
                        } catch (IOException e) { e.printStackTrace(); }
                    }
                });

        avatar.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImage.launch(i);
        });

        btnUpdate.setOnClickListener(v -> {
            String name = editDisplayName.getText().toString().trim();
            String bio  = editBio.getText().toString().trim();
            UserProfileChangeRequest.Builder up = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name);
            if (selectedImageUri!=null) up.setPhotoUri(selectedImageUri);

            user.updateProfile(up.build())
                    .addOnSuccessListener(a -> {
                        // save bio
                        Map<String,Object> m = new HashMap<>();
                        m.put("bio", bio);
                        db.collection("users").document(user.getUid())
                                .set(m, SetOptions.merge())
                                .addOnSuccessListener(u -> Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show());
                    });
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        btnDelete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Delete Account?")
                .setMessage("This cannot be undone.")
                .setPositiveButton("Delete",(d,i)-> {
                    user.delete().addOnSuccessListener(u -> {
                        db.collection("users").document(user.getUid()).delete();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    });
                })
                .setNegativeButton("Cancel",null)
                .show());
    }
}
