package com.example.tradeup_app.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.tradeup_app.R;
import com.example.tradeup_app.auth.AccountSettingsActivity;


import com.example.tradeup_app.auth.Helper.CurrentUser;
import com.example.tradeup_app.auth.LoginActivity;
import com.example.tradeup_app.auth.Domain.UserModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class ProfileFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1001;

    private ImageView profileImageView;
    private TextView usernameTextView, emailTextView, ratingTextView, totalSalesTextView, bioTextView;
    private MaterialButton editProfileButton;
    private LinearLayout myListingsButton, purchaseHistoryButton, savedItemsButton,
            accountSettingsButton, logoutButton;

    private UserModel currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initViews(view);
        setupListeners(view);
        loadUserData();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        loadUserData();
    }

    private void initViews(View view) {
        profileImageView = view.findViewById(R.id.profile_image_view);
        usernameTextView = view.findViewById(R.id.username_text_view);
        emailTextView = view.findViewById(R.id.email_text_view);
        ratingTextView = view.findViewById(R.id.rating_text_view);
        totalSalesTextView = view.findViewById(R.id.total_sales_text_view);
        bioTextView = view.findViewById(R.id.bio_text_view);
        editProfileButton = view.findViewById(R.id.edit_profile_button);
        myListingsButton = view.findViewById(R.id.my_listings_button);
        purchaseHistoryButton = view.findViewById(R.id.purchase_history_button);
        savedItemsButton = view.findViewById(R.id.saved_items_button);
        accountSettingsButton = view.findViewById(R.id.account_settings_button);
        logoutButton = view.findViewById(R.id.logout_button);
    }

    private void setupListeners(View view) {
        editProfileButton.setOnClickListener(v -> showEditProfileDialog());





        myListingsButton.setOnClickListener(v ->
                Toast.makeText(getContext(), "My Listings feature coming soon", Toast.LENGTH_SHORT).show());





        purchaseHistoryButton.setOnClickListener(v -> {

            Intent intent = new Intent(getContext(), com.example.tradeup_app.activities.PaymentHistoryActivity.class);
            startActivity(intent);
        });

        savedItemsButton.setOnClickListener(v ->
                Toast.makeText(getContext(), "Saved Items feature coming soon", Toast.LENGTH_SHORT).show());





        accountSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AccountSettingsActivity.class);
            startActivity(intent);
        });



        // Admin Dashboard - Check and setup click listener
        View adminDashboardButton = view.findViewById(R.id.admin_dashboard_button);
        if (adminDashboardButton != null) {
            // Always set click listener
            adminDashboardButton.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), com.example.tradeup_app.activities.AdminDashboardActivity.class);
                startActivity(intent);
            });

            // Check if should be visible (will be updated again in updateUI)
            if (isAdminUser()) {
                adminDashboardButton.setVisibility(View.VISIBLE);
            }
        }


        logoutButton.setOnClickListener(v -> showLogoutDialog());

        profileImageView.setOnClickListener(v -> pickImageFromGallery());
    }

    private void showEditProfileDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_updateprofile);

        EditText editUsername = dialog.findViewById(R.id.editUsername);
        EditText editBio = dialog.findViewById(R.id.editBio);
        EditText editContact = dialog.findViewById(R.id.editContact);
        MaterialButton saveButton = dialog.findViewById(R.id.saveProfileButton);

        if (currentUser != null) {
            editUsername.setText(currentUser.getUsername());
            editBio.setText(currentUser.getBio());
            editContact.setText(currentUser.getContact());
        }

        saveButton.setOnClickListener(v -> {
            String newUsername = editUsername.getText().toString().trim();
            String newBio = editBio.getText().toString().trim();
            String newContact = editContact.getText().toString().trim();

            if (currentUser != null) {
                currentUser.setUsername(newUsername);
                currentUser.setBio(newBio);
                currentUser.setContact(newContact);

                FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                if (firebaseUser != null) {
                    FirebaseDatabase.getInstance().getReference("Users")
                            .child(firebaseUser.getUid())
                            .setValue(currentUser)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                                CurrentUser.setUser(currentUser);
                                updateUI();
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Update failed", Toast.LENGTH_SHORT).show());
                }
            }
        });

        dialog.show();
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            Glide.with(this).load(imageUri).into(profileImageView);
            if (currentUser != null) {
                currentUser.setProfilePic(imageUri.toString());
                FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                if (firebaseUser != null) {
                    FirebaseDatabase.getInstance().getReference("Users")
                            .child(firebaseUser.getUid())
                            .setValue(currentUser);
                }
            }
        }
    }

    private void loadUserData() {

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {

            redirectToLogin();
            return;
        }


        currentUser = CurrentUser.getUser();

        if (currentUser != null) {
            updateUI();
        } else {

            loadUserFromFirebase(firebaseUser.getUid());
        }
    }

    private void loadUserFromFirebase(String uid) {
        FirebaseDatabase.getInstance().getReference("Users").child(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        UserModel userModel = snapshot.getValue(UserModel.class);
                        if (userModel != null) {
                            CurrentUser.setUser(userModel);
                            currentUser = userModel;
                            updateUI();
                        } else {
                            redirectToLogin();

                        }
                    } else {
                        redirectToLogin();

                    }
                })
                .addOnFailureListener(e -> redirectToLogin());










    }

    private void updateUI() {
        if (currentUser == null) return;


        usernameTextView.setText(currentUser.getUsername());


        emailTextView.setText(currentUser.getEmail());


        String profilePicUrl = currentUser.getProfilePic();
        if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
            Glide.with(this)
                    .load(profilePicUrl)
                    .placeholder(R.drawable.ic_user_placeholder)
                    .into(profileImageView);
        } else {
            profileImageView.setImageResource(R.drawable.ic_user_placeholder);
        }


        double rating = currentUser.getRating();
        if (rating > 0) {
            ratingTextView.setText(getString(R.string.rating_format, rating));
        } else {
            ratingTextView.setText(R.string.new_user);
        }


        String bio = currentUser.getBio();
        bioTextView.setText(bio != null && !bio.trim().isEmpty() ? bio : getString(R.string.no_bio_available));







        totalSalesTextView.setText("0");

        // Ensure admin dashboard button visibility is updated after user data is loaded
        View adminDashboardButton = getView() != null ? getView().findViewById(R.id.admin_dashboard_button) : null;
        if (adminDashboardButton != null) {
            adminDashboardButton.setVisibility(isAdminUser() ? View.VISIBLE : View.GONE);
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())


                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void performLogout() {

        FirebaseAuth.getInstance().signOut();
        CurrentUser.setUser(null);


        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();


        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(getContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private boolean isAdminUser() {

        return currentUser != null && currentUser.getIsAdmin();
    }
}

