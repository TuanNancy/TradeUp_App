package com.example.tradeup_app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup_app.R;
import com.example.tradeup_app.adapters.ProductAdapter;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Product;
import com.example.tradeup_app.activities.ChatActivity;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerViewFeatured, recyclerViewRecent;
    private TextInputEditText searchBar;
    private ProductAdapter featuredAdapter, recentAdapter;
    private FirebaseManager firebaseManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        setupRecyclerViews();
        loadFeaturedItems();
        loadRecentItems();
        setupSearchBar();

        return view;
    }

    private void initViews(View view) {
        recyclerViewFeatured = view.findViewById(R.id.recycler_featured);
        recyclerViewRecent = view.findViewById(R.id.recycler_recent);
        searchBar = view.findViewById(R.id.search_bar);
        firebaseManager = FirebaseManager.getInstance();
    }

    private void setupRecyclerViews() {
        // Featured products (horizontal)
        featuredAdapter = new ProductAdapter(getContext(), new ArrayList<>());
        recyclerViewFeatured.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerViewFeatured.setAdapter(featuredAdapter);

        // Recent products (vertical)
        recentAdapter = new ProductAdapter(getContext(), new ArrayList<>());
        recyclerViewRecent.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewRecent.setAdapter(recentAdapter);

        // Set click listeners
        featuredAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                openProductChat(product);
            }

            @Override
            public void onProductLongClick(Product product) {
                // TODO: Show product details or options menu
            }
        });

        recentAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                openProductChat(product);
            }

            @Override
            public void onProductLongClick(Product product) {
                // TODO: Show product details or options menu
            }
        });
    }

    private void loadFeaturedItems() {
        firebaseManager.getProducts(new FirebaseManager.ProductCallback() {
            @Override
            public void onProductsLoaded(List<Product> products) {
                if (getActivity() != null) {
                    // Get featured products (e.g., top 10 most viewed)
                    List<Product> featuredProducts = products.size() > 10 ?
                        products.subList(0, 10) : products;
                    featuredAdapter.updateProducts(featuredProducts);
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    Toast.makeText(getContext(), "L��i tải sản phẩm nổi bật: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadRecentItems() {
        firebaseManager.getProducts(new FirebaseManager.ProductCallback() {
            @Override
            public void onProductsLoaded(List<Product> products) {
                recentAdapter.updateProducts(products);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearchBar() {
        searchBar.setOnClickListener(v -> {
            // Switch to search fragment
            if (getActivity() != null) {
                ((androidx.fragment.app.FragmentActivity) getActivity()).getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SearchFragment())
                    .addToBackStack(null)
                    .commit();
            }
        });
    }

    private void openProductChat(Product product) {
        Intent intent = new Intent(getContext(), ChatActivity.class);
        intent.putExtra("productId", product.getId());
        intent.putExtra("sellerId", product.getSellerId());
        startActivity(intent);

        // Increment view count
        firebaseManager.incrementProductViews(product.getId());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment resumes
        refreshData();
    }

    // Add public method to refresh data
    public void refreshData() {
        if (isAdded() && getContext() != null) {
            loadFeaturedItems();
            loadRecentItems();
        }
    }
}
