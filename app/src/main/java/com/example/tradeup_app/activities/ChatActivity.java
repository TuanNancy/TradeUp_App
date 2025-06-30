package com.example.tradeup_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup_app.R;
import com.example.tradeup_app.adapters.MessageAdapter;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Conversation;
import com.example.tradeup_app.models.Message;
import com.example.tradeup_app.models.Product;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView messagesRecyclerView;
    private EditText messageEditText;
    private Button sendButton, makeOfferButton;
    private ImageView productImage, backButton;
    private TextView productTitle, productPrice, sellerName;

    private MessageAdapter messageAdapter;
    private List<Message> messages;
    private FirebaseManager firebaseManager;
    private ValueEventListener messageListener;

    private String conversationId;
    private String productId;
    private String sellerId;
    private String buyerId;
    private Product currentProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initViews();
        getIntentData();
        setupRecyclerView();
        setupButtons();
        loadProductInfo();
        loadMessages();
    }

    private void initViews() {
        messagesRecyclerView = findViewById(R.id.messages_recycler_view);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_button);
        makeOfferButton = findViewById(R.id.make_offer_button);
        productImage = findViewById(R.id.product_image);
        productTitle = findViewById(R.id.product_title);
        productPrice = findViewById(R.id.product_price);
        sellerName = findViewById(R.id.seller_name);
        backButton = findViewById(R.id.back_button);

        firebaseManager = FirebaseManager.getInstance();
        messages = new ArrayList<>();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        conversationId = intent.getStringExtra("conversationId");
        productId = intent.getStringExtra("productId");
        sellerId = intent.getStringExtra("sellerId");
        buyerId = firebaseManager.getCurrentUserId();

        // If no conversation ID, create new conversation
        if (conversationId == null) {
            conversationId = productId + "_" + buyerId + "_" + sellerId;
        }
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(this, messages, firebaseManager.getCurrentUserId());
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(messageAdapter);
    }

    private void setupButtons() {
        backButton.setOnClickListener(v -> finish());

        sendButton.setOnClickListener(v -> sendMessage());

        makeOfferButton.setOnClickListener(v -> showOfferDialog());

        // Enable/disable send button based on text input
        messageEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendButton.setEnabled(!TextUtils.isEmpty(s.toString().trim()));
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void loadProductInfo() {
        DatabaseReference ref = firebaseManager.getDatabase().getReference();
        ref.child(FirebaseManager.PRODUCTS_NODE)
           .child(productId)
           .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        currentProduct = snapshot.getValue(Product.class);
                        if (currentProduct != null) {
                            currentProduct.setId(snapshot.getKey());
                            displayProductInfo();
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Toast.makeText(ChatActivity.this, "Lỗi tải thông tin sản phẩm", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void displayProductInfo() {
        productTitle.setText(currentProduct.getTitle());
        productPrice.setText(String.format("%.0f VNĐ", currentProduct.getPrice()));
        sellerName.setText(currentProduct.getSellerName());

        if (currentProduct.getImageUrls() != null && !currentProduct.getImageUrls().isEmpty()) {
            Glide.with(this)
                    .load(currentProduct.getImageUrls().get(0))
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .centerCrop()
                    .into(productImage);
        }
    }

    private void loadMessages() {
        DatabaseReference messagesRef = firebaseManager.getDatabase()
                .getReference(FirebaseManager.MESSAGES_NODE);

        messageListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                messages.clear();
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    Message message = messageSnapshot.getValue(Message.class);
                    if (message != null && conversationId.equals(message.getConversationId())) {
                        message.setId(messageSnapshot.getKey());
                        messages.add(message);
                    }
                }
                messageAdapter.notifyDataSetChanged();
                scrollToBottom();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Lỗi tải tin nhắn", Toast.LENGTH_SHORT).show();
            }
        };

        messagesRef.addValueEventListener(messageListener);
    }

    private void sendMessage() {
        String content = messageEditText.getText().toString().trim();
        if (TextUtils.isEmpty(content)) return;

        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(firebaseManager.getCurrentUserId());
        message.setReceiverId(sellerId);
        message.setContent(content);
        message.setTimestamp(System.currentTimeMillis()); // Fixed: use long timestamp instead of Date
        message.setMessageType("text");

        firebaseManager.sendMessage(message, task -> {
            if (task.isSuccessful()) {
                messageEditText.setText("");
                // Message will be added through the listener
            } else {
                Toast.makeText(this, "Lỗi gửi tin nhắn", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showOfferDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Đưa ra đề xuất giá");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Nhập giá đề xuất");
        builder.setView(input);

        builder.setPositiveButton("Gửi đề xuất", (dialog, which) -> {
            String offerText = input.getText().toString().trim();
            if (!TextUtils.isEmpty(offerText)) {
                try {
                    double offerAmount = Double.parseDouble(offerText);
                    sendOfferMessage(offerAmount);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Giá không hợp lệ", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void sendOfferMessage(double offerAmount) {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(firebaseManager.getCurrentUserId());
        message.setReceiverId(sellerId);
        message.setContent("Đề xuất giá: " + String.format("%.0f VNĐ", offerAmount));
        message.setTimestamp(System.currentTimeMillis()); // Fixed: use long timestamp instead of Date
        message.setMessageType("offer");
        message.setProductId(productId);
        message.setOfferAmount(offerAmount);

        firebaseManager.sendMessage(message, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Đã gửi đề xuất giá", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Lỗi gửi đề xuất", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void scrollToBottom() {
        if (messageAdapter.getItemCount() > 0) {
            messagesRecyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            firebaseManager.getDatabase()
                    .getReference(FirebaseManager.MESSAGES_NODE)
                    .removeEventListener(messageListener);
        }
    }
}
