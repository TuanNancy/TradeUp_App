package com.example.tradeup_app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup_app.R;
import com.example.tradeup_app.models.Offer;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OfferAdapter extends RecyclerView.Adapter<OfferAdapter.OfferViewHolder> {

    public interface OnOfferActionListener {
        void onAcceptOffer(Offer offer);
        void onRejectOffer(Offer offer);
        void onCounterOffer(Offer offer);
        void onViewOffer(Offer offer);
    }

    private Context context;
    private List<Offer> offers;
    private OnOfferActionListener listener;
    private boolean isSellerView; // true if seller viewing offers, false if buyer viewing

    public OfferAdapter(Context context, List<Offer> offers, boolean isSellerView) {
        this.context = context;
        this.offers = offers;
        this.isSellerView = isSellerView;
    }

    public void setOnOfferActionListener(OnOfferActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public OfferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_offer, parent, false);
        return new OfferViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OfferViewHolder holder, int position) {
        Offer offer = offers.get(position);
        holder.bind(offer);
    }

    @Override
    public int getItemCount() {
        return offers.size();
    }

    public void updateOffers(List<Offer> newOffers) {
        this.offers = newOffers;
        notifyDataSetChanged();
    }

    class OfferViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardView;
        private TextView userNameText;
        private TextView offerPriceText;
        private TextView originalPriceText;
        private TextView messageText;
        private TextView statusText;
        private TextView dateText;
        private MaterialButton acceptButton;
        private MaterialButton rejectButton;
        private MaterialButton counterButton;
        private View actionButtonsLayout;

        public OfferViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.offerCardView);
            userNameText = itemView.findViewById(R.id.userNameText);
            offerPriceText = itemView.findViewById(R.id.offerPriceText);
            originalPriceText = itemView.findViewById(R.id.originalPriceText);
            messageText = itemView.findViewById(R.id.messageText);
            statusText = itemView.findViewById(R.id.statusText);
            dateText = itemView.findViewById(R.id.dateText);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
            counterButton = itemView.findViewById(R.id.counterButton);
            actionButtonsLayout = itemView.findViewById(R.id.actionButtonsLayout);
        }

        public void bind(Offer offer) {
            // Set user name based on view type
            if (isSellerView) {
                userNameText.setText("Offer from " + offer.getBuyerName());
            } else {
                userNameText.setText("Offer to seller");
            }

            // Set prices in VND format instead of USD
            offerPriceText.setText(formatVNDPrice(offer.getOfferPrice()));
            originalPriceText.setText("Original: " + formatVNDPrice(offer.getOriginalPrice()));

            // Set message
            if (offer.getMessage() != null && !offer.getMessage().isEmpty()) {
                messageText.setText(offer.getMessage());
                messageText.setVisibility(View.VISIBLE);
            } else {
                messageText.setVisibility(View.GONE);
            }

            // Set status
            statusText.setText(offer.getStatus());
            setStatusColor(offer.getStatus());

            // Set date
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            dateText.setText(sdf.format(new Date(offer.getCreatedAt())));

            // Show/hide action buttons based on status and view type
            if (isSellerView && "PENDING".equals(offer.getStatus())) {
                actionButtonsLayout.setVisibility(View.VISIBLE);
                setupActionButtons(offer);
                android.util.Log.d("OfferAdapter", "Showing action buttons for PENDING offer from: " + offer.getBuyerName());
            } else {
                actionButtonsLayout.setVisibility(View.GONE);
                android.util.Log.d("OfferAdapter", "Hiding action buttons - isSellerView: " + isSellerView + ", status: " + offer.getStatus());
            }

            // Set click listener for the card
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewOffer(offer);
                }
            });
        }

        // Add method to format VND price
        private String formatVNDPrice(double price) {
            java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###");
            return formatter.format(price) + " VND";
        }

        private void setStatusColor(String status) {
            int colorRes;
            switch (status) {
                case "ACCEPTED":
                    colorRes = R.color.offer_accepted;
                    break;
                case "REJECTED":
                    colorRes = R.color.offer_rejected;
                    break;
                case "COUNTERED":
                    colorRes = R.color.offer_countered;
                    break;
                case "PENDING":
                default:
                    colorRes = R.color.offer_pending;
                    break;
            }
            statusText.setTextColor(ContextCompat.getColor(context, colorRes));
        }

        private void setupActionButtons(Offer offer) {
            acceptButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAcceptOffer(offer);
                }
            });

            rejectButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRejectOffer(offer);
                }
            });

            counterButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCounterOffer(offer);
                }
            });
        }
    }
}
