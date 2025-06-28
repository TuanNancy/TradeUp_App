package com.example.tradeup_app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup_app.R;
import com.example.tradeup_app.adapters.ProductAdapter;
import com.example.tradeup_app.auth.LoginActivity;
import com.example.tradeup_app.auth.UserProfileActivity;
import com.example.tradeup_app.auth.Helper.CurrentUser;
import com.example.tradeup_app.auth.Domain.UserModel;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Product;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private ImageView profileImageView;
    private TextView usernameTextView, emailTextView, ratingTextView, totalSalesTextView;
    private Button editProfileButton, myListingsButton, salesHistoryButton,
                   purchaseHistoryButton, savedItemsButton, logoutButton;
    private RecyclerView myListingsRecyclerView;

    private ProductAdapter myListingsAdapter;
    private FirebaseManager firebaseManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initViews(view);
        setupButtons();
        setupRecyclerView();
        loadUserData();
        loadUserListings();

        return view;
    }

    private void initViews(View view) {
        profileImageView = view.findViewById(R.id.profile_image_view);
        usernameTextView = view.findViewById(R.id.username_text_view);
        emailTextView = view.findViewById(R.id.email_text_view);
        ratingTextView = view.findViewById(R.id.rating_text_view);
        totalSalesTextView = view.findViewById(R.id.total_sales_text_view);
        editProfileButton = view.findViewById(R.id.edit_profile_button);
        myListingsButton = view.findViewById(R.id.my_listings_button);
        salesHistoryButton = view.findViewById(R.id.sales_history_button);
        purchaseHistoryButton = view.findViewById(R.id.purchase_history_button);
        savedItemsButton = view.findViewById(R.id.saved_items_button);
        logoutButton = view.findViewById(R.id.logout_button);
        myListingsRecyclerView = view.findViewById(R.id.my_listings_recycler_view);

        firebaseManager = FirebaseManager.getInstance();
    }

    private void setupButtons() {
        editProfileButton.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), UserProfileActivity.class));
        });

        myListingsButton.setOnClickListener(v -> {
            // TODO: Show all user's listings in a new activity/fragment
            Toast.makeText(getContext(), "Hiển thị tất cả sản phẩm của bạn", Toast.LENGTH_SHORT).show();
        });

        salesHistoryButton.setOnClickListener(v -> {
            // TODO: Show sales history
            Toast.makeText(getContext(), "Lịch sử bán hàng", Toast.LENGTH_SHORT).show();
        });

        purchaseHistoryButton.setOnClickListener(v -> {
            // TODO: Show purchase history
            Toast.makeText(getContext(), "Lịch sử mua hàng", Toast.LENGTH_SHORT).show();
        });

        savedItemsButton.setOnClickListener(v -> {
            // TODO: Show saved items
            Toast.makeText(getContext(), "Sản phẩm đã lưu", Toast.LENGTH_SHORT).show();
        });

        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            CurrentUser.setUser(null); // Clear cached user
            startActivity(new Intent(getContext(), LoginActivity.class));
            requireActivity().finish();
        });
    }

    private void setupRecyclerView() {
        myListingsAdapter = new ProductAdapter(getContext(), new ArrayList<>());
        myListingsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        myListingsRecyclerView.setAdapter(myListingsAdapter);

        myListingsAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                // TODO: Show product management options (edit, delete, mark as sold)
                showProductManagementDialog(product);
            }

            @Override
            public void onProductLongClick(Product product) {
                showProductManagementDialog(product);
            }
        });
    }

    private void loadUserData() {
        UserModel currentUser = CurrentUser.getUser();
        if (currentUser != null) {
            usernameTextView.setText(currentUser.getUsername());
            emailTextView.setText(currentUser.getEmail());

            // Load profile image with Glide
            if (currentUser.getProfilePic() != null && !currentUser.getProfilePic().isEmpty()) {
                Glide.with(this)
                        .load(currentUser.getProfilePic())
                        .placeholder(R.drawable.ic_user)
                        .error(R.drawable.ic_user)
                        .circleCrop()
                        .into(profileImageView);
            } else {
                profileImageView.setImageResource(R.drawable.ic_user);
            }

            // TODO: Load rating and sales data from Firebase
            ratingTextView.setText("4.5 ⭐");
            // Count user's sold products
            countUserSoldProducts();
        }
    }

    private void loadUserListings() {
        String userId = firebaseManager.getCurrentUserId();
        if (userId != null) {
            firebaseManager.getUserProducts(userId, new FirebaseManager.ProductCallback() {
                @Override
                public void onSuccess(List<Product> products) {
                    if (getActivity() != null) {
                        // Show only recent 4 products
                        List<Product> recentProducts = products.size() > 4 ?
                            products.subList(0, 4) : products;
                        myListingsAdapter.updateProducts(recentProducts);
                    }
                }

                @Override
                public void onFailure(String error) {
                    if (getActivity() != null) {
                        Toast.makeText(getContext(), "Lỗi tải sản phẩm: " + error, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void countUserSoldProducts() {
        String userId = firebaseManager.getCurrentUserId();
        if (userId != null) {
            firebaseManager.getUserProducts(userId, new FirebaseManager.ProductCallback() {
                @Override
                public void onSuccess(List<Product> products) {
                    if (getActivity() != null) {
                        int soldCount = 0;
                        for (Product product : products) {
                            if ("Sold".equals(product.getStatus())) {
                                soldCount++;
                            }
                        }
                        totalSalesTextView.setText(soldCount + " sản phẩm đã bán");
                    }
                }

                @Override
                public void onFailure(String error) {
                    // Ignore error for sales count
                }
            });
        }
    }

    private void showProductManagementDialog(Product product) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Quản lý sản phẩm: " + product.getTitle());

        String[] options = {"Chỉnh sửa", "Đánh dấu đã bán", "Tạm ngưng", "Xóa sản phẩm"};

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    // TODO: Edit product
                    Toast.makeText(getContext(), "Chỉnh sửa sản phẩm", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    markProductAsSold(product);
                    break;
                case 2:
                    pauseProduct(product);
                    break;
                case 3:
                    deleteProduct(product);
                    break;
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void markProductAsSold(Product product) {
        firebaseManager.updateProductStatus(product.getId(), "Sold", task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Đã đánh dấu sản phẩm là đã bán", Toast.LENGTH_SHORT).show();
                loadUserListings(); // Refresh listings
                countUserSoldProducts(); // Update sold count
            } else {
                Toast.makeText(getContext(), "Lỗi cập nhật trạng thái", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pauseProduct(Product product) {
        firebaseManager.updateProductStatus(product.getId(), "Paused", task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Đã tạm ngưng sản phẩm", Toast.LENGTH_SHORT).show();
                loadUserListings(); // Refresh listings
            } else {
                Toast.makeText(getContext(), "Lỗi cập nhật trạng thái", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteProduct(Product product) {
        androidx.appcompat.app.AlertDialog.Builder confirmBuilder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        confirmBuilder.setTitle("Xác nhận xóa");
        confirmBuilder.setMessage("Bạn có chắc chắn muốn xóa sản phẩm này không?");

        confirmBuilder.setPositiveButton("Xóa", (dialog, which) -> {
            // TODO: Implement delete product in FirebaseManager
            Toast.makeText(getContext(), "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show();
            loadUserListings(); // Refresh listings
        });

        confirmBuilder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        confirmBuilder.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData(); // Refresh data when fragment resumes
        loadUserListings();
    }
}
