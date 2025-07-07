package com.example.tradeup_app.auth.Helper;

import android.util.Log;
import com.example.tradeup_app.auth.Domain.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CurrentUser {
    private static final String TAG = "CurrentUser";
    private static UserModel currentUser;
    private static boolean isLoading = false;

    public static void setUser(UserModel user) {
        currentUser = user;
        Log.d(TAG, "User set: " + (user != null ? user.getUsername() : "null"));
    }

    public static UserModel getUser() {
        // Nếu currentUser null, thử load từ Firebase
        if (currentUser == null && !isLoading) {
            loadCurrentUserFromFirebase();
        }
        return currentUser;
    }

    public static void clear() {
        currentUser = null;
        Log.d(TAG, "User cleared");
    }

    // Tự động load user từ Firebase nếu bị null
    private static void loadCurrentUserFromFirebase() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Log.w(TAG, "No Firebase user logged in");
            return;
        }

        isLoading = true;
        String uid = firebaseUser.getUid();
        Log.d(TAG, "Loading user from Firebase with UID: " + uid);

        FirebaseDatabase.getInstance().getReference("Users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        isLoading = false;
                        if (snapshot.exists()) {
                            UserModel user = snapshot.getValue(UserModel.class);
                            if (user != null) {
                                user.setUid(uid); // Đảm bảo UID được set
                                currentUser = user;
                                Log.d(TAG, "User loaded from Firebase: " + user.getUsername());
                            }
                        } else {
                            Log.w(TAG, "User data not found in Firebase");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        isLoading = false;
                        Log.e(TAG, "Failed to load user from Firebase", error.toException());
                    }
                });
    }

    // Đồng bộ load user từ Firebase (chờ kết quả)
    public static void loadUserSynchronously(LoadUserCallback callback) {
        if (currentUser != null) {
            callback.onUserLoaded(currentUser);
            return;
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            callback.onError("No user logged in");
            return;
        }

        String uid = firebaseUser.getUid();
        FirebaseDatabase.getInstance().getReference("Users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            UserModel user = snapshot.getValue(UserModel.class);
                            if (user != null) {
                                user.setUid(uid);
                                currentUser = user;
                                callback.onUserLoaded(user);
                            } else {
                                callback.onError("Failed to parse user data");
                            }
                        } else {
                            callback.onError("User data not found");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        callback.onError("Database error: " + error.getMessage());
                    }
                });
    }

    public interface LoadUserCallback {
        void onUserLoaded(UserModel user);
        void onError(String error);
    }
}
