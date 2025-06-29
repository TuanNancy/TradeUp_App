package com.example.tradeup_app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup_app.R;
import com.example.tradeup_app.adapters.OfferAdapter;
import com.example.tradeup_app.dialogs.MakeOfferDialog;
import com.example.tradeup_app.dialogs.RatingDialog;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Offer;
import com.example.tradeup_app.models.Product;
import com.example.tradeup_app.models.Transaction;
import com.example.tradeup_app.models.Rating;
import com.example.tradeup_app.auth.Helper.CurrentUser;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class OffersActivity extends AppCompatActivity implements OfferAdapter.OnOfferActionListener {

    private RecyclerView offersRecyclerView;
    private OfferAdapter offerAdapter;
    private CircularProgressIndicator progressIndicator;
    private View emptyView;
    private MaterialButton makeOfferButton;

    private FirebaseManager firebaseManager;
    private Product product;
    private boolean isSellerView;
    private List<Offer> offers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offers);

        // Get data from intent
        product = (Product) getIntent().getSerializableExtra("product");
        isSellerView = getIntent().getBooleanExtra("isSellerView", false);

        if (product == null) {
            Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupListeners();
        loadOffers();
    }

    private void initViews() {
        offersRecyclerView = findViewById(R.id.offersRecyclerView);
        progressIndicator = findViewById(R.id.progressIndicator);
        emptyView = findViewById(R.id.emptyView);
        makeOfferButton = findViewById(R.id.makeOfferButton);

        firebaseManager = FirebaseManager.getInstance();

        // Show/hide make offer button based on view type
        if (isSellerView || product.getSellerId().equals(firebaseManager.getCurrentUserId())) {
            makeOfferButton.setVisibility(View.GONE);
        } else {
            makeOfferButton.setVisibility(View.VISIBLE);
        }

        // Set activity title
        setTitle(isSellerView ? "Offers Received" : "Your Offers");
    }

    private void setupRecyclerView() {
        offerAdapter = new OfferAdapter(this, offers, isSellerView);
        offerAdapter.setOnOfferActionListener(this);
        offersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        offersRecyclerView.setAdapter(offerAdapter);
    }

    private void setupListeners() {
        makeOfferButton.setOnClickListener(v -> showMakeOfferDialog());
    }

    private void loadOffers() {
        showLoading(true);

        if (isSellerView) {
            // Load offers for products owned by current user (seller view)
            firebaseManager.getOffersForSeller(firebaseManager.getCurrentUserId(), new FirebaseManager.OfferCallback() {
                @Override
                public void onOffersLoaded(List<Offer> loadedOffers) {
                    offers.clear();
                    // Filter offers for this specific product
                    for (Offer offer : loadedOffers) {
                        if (product.getId().equals(offer.getProductId())) {
                            offers.add(offer);
                        }
                    }
                    updateUI();
                }

                @Override
                public void onError(String error) {
                    showLoading(false);
                    Toast.makeText(OffersActivity.this, "Error loading offers: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Load offers made by current user (buyer view)
            firebaseManager.getOffersForBuyer(firebaseManager.getCurrentUserId(), new FirebaseManager.OfferCallback() {
                @Override
                public void onOffersLoaded(List<Offer> loadedOffers) {
                    offers.clear();
                    // Filter offers for this specific product
                    for (Offer offer : loadedOffers) {
                        if (product.getId().equals(offer.getProductId())) {
                            offers.add(offer);
                        }
                    }
                    updateUI();
                }

                @Override
                public void onError(String error) {
                    showLoading(false);
                    Toast.makeText(OffersActivity.this, "Error loading offers: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateUI() {
        showLoading(false);
        offerAdapter.updateOffers(offers);

        if (offers.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            offersRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            offersRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        offersRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showMakeOfferDialog() {
        MakeOfferDialog dialog = new MakeOfferDialog(this, product, (offerPrice, message) -> {
            submitOffer(offerPrice, message);
        });
        dialog.show();
    }

    private void submitOffer(double offerPrice, String message) {
        String currentUserId = firebaseManager.getCurrentUserId();
        String currentUserName = CurrentUser.getUser() != null ? CurrentUser.getUser().getUsername() : "Anonymous";

        Offer offer = new Offer(
            product.getId(),
            currentUserId,
            currentUserName,
            product.getSellerId(),
            product.getPrice(),
            offerPrice,
            message
        );

        firebaseManager.submitOffer(offer, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Offer submitted successfully!", Toast.LENGTH_SHORT).show();
                loadOffers(); // Refresh the list
            } else {
                Toast.makeText(this, "Failed to submit offer", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // OfferAdapter.OnOfferActionListener implementations
    @Override
    public void onAcceptOffer(Offer offer) {
        // Accept the offer and create transaction
        firebaseManager.updateOfferStatus(offer.getId(), "ACCEPTED", task -> {
            if (task.isSuccessful()) {
                createTransaction(offer);
                Toast.makeText(this, "Offer accepted!", Toast.LENGTH_SHORT).show();
                loadOffers(); // Refresh the list
            } else {
                Toast.makeText(this, "Failed to accept offer", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRejectOffer(Offer offer) {
        firebaseManager.updateOfferStatus(offer.getId(), "REJECTED", task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Offer rejected", Toast.LENGTH_SHORT).show();
                loadOffers(); // Refresh the list
            } else {
                Toast.makeText(this, "Failed to reject offer", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCounterOffer(Offer offer) {
        // Show dialog to input counter offer
        MakeOfferDialog dialog = new MakeOfferDialog(this, product, (counterPrice, counterMessage) -> {
            firebaseManager.counterOffer(offer.getId(), counterPrice, counterMessage, task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Counter offer sent!", Toast.LENGTH_SHORT).show();
                    loadOffers(); // Refresh the list
                } else {
                    Toast.makeText(this, "Failed to send counter offer", Toast.LENGTH_SHORT).show();
                }
            });
        });
        dialog.show();
    }

    @Override
    public void onViewOffer(Offer offer) {
        // TODO: Show offer details dialog
        Toast.makeText(this, "Offer details: $" + offer.getOfferPrice(), Toast.LENGTH_SHORT).show();
    }

    private void createTransaction(Offer offer) {
        Transaction transaction = new Transaction(
            product.getId(),
            product.getTitle(),
            offer.getBuyerId(),
            offer.getBuyerName(),
            offer.getSellerId(),
            product.getSellerName(),
            offer.getOfferPrice()
        );
        transaction.setOfferId(offer.getId());

        firebaseManager.createTransaction(transaction, task -> {
            if (task.isSuccessful()) {
                // Mark product as sold
                markProductAsSold();

                // Show rating dialog after some delay (simulate transaction completion)
                showRatingDialog(transaction, offer);
            }
        });
    }

    private void markProductAsSold() {
        // Update product status to sold
        firebaseManager.getDatabase().getReference(FirebaseManager.PRODUCTS_NODE)
            .child(product.getId())
            .child("status")
            .setValue("Sold");
    }

    private void showRatingDialog(Transaction transaction, Offer offer) {
        String otherUserName = isSellerView ? offer.getBuyerName() : product.getSellerName();
        String otherUserPhotoUrl = ""; // TODO: Get from user profile

        RatingDialog ratingDialog = new RatingDialog(this, otherUserName, otherUserPhotoUrl, new RatingDialog.OnRatingSubmitListener() {
            @Override
            public void onRatingSubmit(int stars, String review) {
                submitRating(transaction, stars, review, offer);
            }

            @Override
            public void onSkip() {
                // User chose to skip rating
            }
        });
        ratingDialog.show();
    }

    private void submitRating(Transaction transaction, int stars, String review, Offer offer) {
        String currentUserId = firebaseManager.getCurrentUserId();
        String currentUserName = CurrentUser.getUser() != null ? CurrentUser.getUser().getUsername() : "Anonymous";
        String ratedUserId = isSellerView ? offer.getBuyerId() : product.getSellerId();
        String ratedUserName = isSellerView ? offer.getBuyerName() : product.getSellerName();
        String userType = isSellerView ? "SELLER" : "BUYER";

        Rating rating = new Rating(
            transaction.getId(),
            currentUserId,
            currentUserName,
            ratedUserId,
            ratedUserName,
            stars,
            review,
            userType
        );

        firebaseManager.submitRating(rating, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Rating submitted successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to submit rating", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
