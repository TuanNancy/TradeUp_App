package com.example.tradeup_app.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup_app.R;
import com.example.tradeup_app.adapters.MessageAdapter;
import com.example.tradeup_app.firebase.FirebaseManager;
import com.example.tradeup_app.models.Message;
import com.example.tradeup_app.services.MessagingService;
import com.example.tradeup_app.utils.ImageUploadManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final int REQUEST_STORAGE_PERMISSION = 1002;

    // UI Components
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private ImageButton buttonAttach;
    private ImageButton buttonEmoji;
    private ImageButton buttonOffer; // NEW: Offer button
    private TextView textViewTyping;
    private Toolbar toolbar;

    // Data
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private MessagingService messagingService;

    // Intent extras
    private String conversationId;
    private String receiverId;
    private String receiverName;
    private String productTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get data from intent
        getIntentData();

        // Initialize messaging service FIRST before UI initialization
        messagingService = new MessagingService(this); // Pass context for notifications

        // Initialize UI
        initializeUI();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup listeners
        setupListeners();

        // Load messages
        loadMessages();

        // Check if user is blocked when opening chat
        checkIfUserIsBlocked();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        conversationId = intent.getStringExtra("conversationId");
        receiverId = intent.getStringExtra("receiverId");
        receiverName = intent.getStringExtra("receiverName");
        productTitle = intent.getStringExtra("productTitle");
    }

    private void initializeUI() {
        // Setup toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set initial title, load actual name if needed
        if (receiverName != null && !receiverName.equals("Unknown")) {
            getSupportActionBar().setTitle(receiverName);
        } else {
            getSupportActionBar().setTitle("Loading...");
            // Load receiver name from Firebase if not provided
            loadReceiverName();
        }

        if (productTitle != null) {
            getSupportActionBar().setSubtitle("About: " + productTitle);
        }

        // Initialize views
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        buttonAttach = findViewById(R.id.buttonAttach);
        buttonEmoji = findViewById(R.id.buttonEmoji);
        buttonOffer = findViewById(R.id.buttonOffer); // Initialize offer button
        textViewTyping = findViewById(R.id.textViewTyping);

        textViewTyping.setVisibility(View.GONE);
    }

    private void loadReceiverName() {
        if (receiverId != null && messagingService != null) {
            // Load user profile from Firebase
            messagingService.getUserProfile(receiverId, new MessagingService.UserProfileCallback() {
                @Override
                public void onSuccess(String userName, String userAvatar) {
                    runOnUiThread(() -> {
                        receiverName = userName;
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle(userName);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle("Chat");
                        }
                    });
                }
            });
        }
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from bottom

        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setAdapter(messageAdapter);
    }

    private void setupListeners() {
        // Send button click
        buttonSend.setOnClickListener(v -> sendTextMessage());

        // Attach button click
        buttonAttach.setOnClickListener(v -> showAttachmentOptions());

        // Emoji button click
        buttonEmoji.setOnClickListener(v -> showEmojiPicker());

        // Offer button click - NEW
        buttonOffer.setOnClickListener(v -> makeOffer());

        // Enable send button only when text is entered
        editTextMessage.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                buttonSend.setEnabled(!TextUtils.isEmpty(s.toString().trim()));
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void sendTextMessage() {
        String messageText = editTextMessage.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        editTextMessage.setText("");
        buttonSend.setEnabled(false);

        messagingService.sendTextMessage(conversationId, receiverId, messageText,
            new MessagingService.MessageCallback() {
                @Override
                public void onMessagesLoaded(List<Message> messages) {}

                @Override
                public void onMessageSent(String messageId) {
                    runOnUiThread(() -> {
                        Toast.makeText(ChatActivity.this, "Message sent", Toast.LENGTH_SHORT).show();
                        buttonSend.setEnabled(true); // Re-enable send button
                        // Message will be updated via real-time listener
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(ChatActivity.this, "Failed to send: " + error, Toast.LENGTH_LONG).show();
                        editTextMessage.setText(messageText); // Restore text
                        buttonSend.setEnabled(true); // Re-enable send button
                    });
                }
            });
    }

    private void showAttachmentOptions() {
        Log.d("ChatActivity", "showAttachmentOptions called");
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_attachments, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        bottomSheetView.findViewById(R.id.layoutCamera).setOnClickListener(v -> {
            Log.d("ChatActivity", "Camera option clicked");
            bottomSheetDialog.dismiss();
            openImagePicker();
        });

        bottomSheetView.findViewById(R.id.layoutGallery).setOnClickListener(v -> {
            Log.d("ChatActivity", "Gallery option clicked");
            bottomSheetDialog.dismiss();
            openImagePicker();
        });

        bottomSheetDialog.show();
        Log.d("ChatActivity", "BottomSheet shown");
    }

    private void openImagePicker() {
        Log.d("ChatActivity", "openImagePicker called");

        // Check permissions based on Android version
        boolean hasPermission = false;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) - use READ_MEDIA_IMAGES
            hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;

            if (!hasPermission) {
                Log.d("ChatActivity", "READ_MEDIA_IMAGES permission not granted, requesting permission");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_STORAGE_PERMISSION);
                return;
            }
        } else {
            // Below Android 13 - use READ_EXTERNAL_STORAGE
            hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;

            if (!hasPermission) {
                Log.d("ChatActivity", "READ_EXTERNAL_STORAGE permission not granted, requesting permission");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
                return;
            }
        }

        Log.d("ChatActivity", "Storage permission granted, starting image picker");
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void showEmojiPicker() {
        // Simple emoji picker - you can enhance this with a proper emoji library
        String[] emojis = {"😀", "😃", "😄", "😁", "😆", "😂", "🤣", "😊", "😇", "🙂",
                          "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚", "😋", "😛",
                          "👍", "👎", "👌", "✌️", "🤞", "🤟", "🤘", "🤙", "👈", "👉",
                          "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Emoji");
        builder.setItems(emojis, (dialog, which) -> {
            String currentText = editTextMessage.getText().toString();
            editTextMessage.setText(currentText + emojis[which]);
            editTextMessage.setSelection(editTextMessage.getText().length());
        });
        builder.show();
    }

    private void loadMessages() {
        messagingService.listenForMessages(conversationId, new MessagingService.MessageCallback() {
            @Override
            public void onMessagesLoaded(List<Message> messages) {
                runOnUiThread(() -> {
                    messageList.clear();
                    messageList.addAll(messages);
                    messageAdapter.notifyDataSetChanged();

                    // Scroll to bottom
                    if (!messageList.isEmpty()) {
                        recyclerViewMessages.scrollToPosition(messageList.size() - 1);
                    }
                });
            }

            @Override
            public void onMessageSent(String messageId) {}

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "Error loading messages: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                sendImageMessage(imageUri);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission denied to access storage", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendImageMessage(Uri imageUri) {
        Log.d("ChatActivity", "sendImageMessage called with URI: " + imageUri);
        // Show progress
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();

        Log.d("ChatActivity", "Calling messagingService.sendImageMessage");
        messagingService.sendImageMessage(conversationId, receiverId, imageUri,
            new MessagingService.ImageUploadCallback() {
                @Override
                public void onImageUploaded(String imageUrl) {
                    Log.d("ChatActivity", "Image uploaded successfully: " + imageUrl);
                    runOnUiThread(() -> {
                        Toast.makeText(ChatActivity.this, "Image sent", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onUploadProgress(int progress) {
                    Log.d("ChatActivity", "Upload progress: " + progress + "%");
                    // Update progress if needed
                }

                @Override
                public void onError(String error) {
                    Log.e("ChatActivity", "Image upload failed: " + error);
                    runOnUiThread(() -> {
                        Toast.makeText(ChatActivity.this, "Failed to send image: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_blocked_users) {
            Intent intent = new Intent(this, BlockedUsersActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_block_user) {
            showBlockUserDialog();
            return true;
        } else if (id == R.id.action_report_conversation) {
            showReportConversationDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showBlockUserDialog() {
        if (receiverName == null) receiverName = "this user";

        new AlertDialog.Builder(this)
            .setTitle("Block User")
            .setMessage("Are you sure you want to block " + receiverName + "? You will no longer receive messages from this user.")
            .setPositiveButton("Block", (dialog, which) -> {
                blockCurrentUser();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showReportConversationDialog() {
        // Use the new comprehensive reporting system
        com.example.tradeup_app.utils.ReportUtils.reportConversation(
            this,
            conversationId,
            receiverId,
            receiverName != null ? receiverName : "Unknown User"
        );
    }

    private void blockCurrentUser() {
        if (conversationId == null || receiverId == null) {
            Toast.makeText(this, "Cannot block user: missing conversation data", Toast.LENGTH_SHORT).show();
            return;
        }

        messagingService.blockUser(conversationId, receiverId, new MessagingService.BlockCallback() {
            @Override
            public void onUserBlocked(boolean success) {
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(ChatActivity.this, receiverName + " has been blocked", Toast.LENGTH_SHORT).show();
                        // Update UI to show blocked state
                        updateUIForBlockedUser(true);
                    } else {
                        Toast.makeText(ChatActivity.this, "Failed to block user", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onUserUnblocked(boolean success) {}

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "Failed to block user: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void updateUIForBlockedUser(boolean isBlocked) {
        if (isBlocked) {
            // Disable message input
            editTextMessage.setEnabled(false);
            editTextMessage.setHint("User is blocked");
            buttonSend.setEnabled(false);
            buttonAttach.setEnabled(false);

            // Change toolbar subtitle to show blocked status
            getSupportActionBar().setSubtitle("🚫 BLOCKED - " + (productTitle != null ? "About: " + productTitle : ""));

            // Show prominent unblock option
            showBlockedUserBar();
        } else {
            // Re-enable message input
            editTextMessage.setEnabled(true);
            editTextMessage.setHint("Type a message...");
            buttonSend.setEnabled(true);
            buttonAttach.setEnabled(true);

            // Restore normal subtitle
            if (productTitle != null) {
                getSupportActionBar().setSubtitle("About: " + productTitle);
            } else {
                getSupportActionBar().setSubtitle(null);
            }

            // Hide blocked user bar
            hideBlockedUserBar();
        }
    }

    private void showBlockedUserBar() {
        // Create a prominent bar at top showing block status with easy unblock button
        View blockedBar = findViewById(R.id.blockedUserBar);
        if (blockedBar == null) {
            // If the bar doesn't exist in layout, create it dynamically
            android.widget.LinearLayout mainLayout = findViewById(R.id.mainChatLayout);
            if (mainLayout != null) {
                View barView = getLayoutInflater().inflate(R.layout.blocked_user_bar, mainLayout, false);
                mainLayout.addView(barView, 1); // Add after toolbar

                // Set up unblock button
                android.widget.Button unblockBtn = barView.findViewById(R.id.btnQuickUnblock);
                unblockBtn.setOnClickListener(v -> showQuickUnblockDialog());
            }
        } else {
            blockedBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideBlockedUserBar() {
        View blockedBar = findViewById(R.id.blockedUserBar);
        if (blockedBar != null) {
            blockedBar.setVisibility(View.GONE);
        }
    }

    private void showQuickUnblockDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Unblock User")
            .setMessage("Unblock " + receiverName + "? You will be able to send and receive messages again.")
            .setPositiveButton("Unblock", (dialog, which) -> {
                quickUnblockUser();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void quickUnblockUser() {
        if (conversationId == null || receiverId == null) {
            Toast.makeText(this, "Cannot unblock user: missing data", Toast.LENGTH_SHORT).show();
            return;
        }

        messagingService.unblockUser(conversationId, receiverId, new MessagingService.BlockCallback() {
            @Override
            public void onUserBlocked(boolean success) {}

            @Override
            public void onUserUnblocked(boolean success) {
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(ChatActivity.this, receiverName + " has been unblocked", Toast.LENGTH_SHORT).show();
                        updateUIForBlockedUser(false);
                    } else {
                        Toast.makeText(ChatActivity.this, "Failed to unblock user", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "Failed to unblock user: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void reportCurrentConversation(String reason) {
        if (conversationId == null) {
            Toast.makeText(this, "Cannot report: missing conversation data", Toast.LENGTH_SHORT).show();
            return;
        }

        messagingService.reportConversation(conversationId, reason, task -> {
            runOnUiThread(() -> {
                if (task.isSuccessful()) {
                    Toast.makeText(ChatActivity.this, "Conversation reported successfully. Thank you for keeping our community safe.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(ChatActivity.this, "Failed to report conversation", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listeners if needed
        if (messagingService != null) {
            messagingService.cleanup();
        }
    }

    // Check if user is blocked when opening chat
    private void checkIfUserIsBlocked() {
        if (receiverId == null) return;

        messagingService.checkIfUserBlocked(
            FirebaseManager.getInstance().getCurrentUserId(),
            receiverId,
            isBlocked -> runOnUiThread(() -> updateUIForBlockedUser(isBlocked))
        );
    }

    private void makeOffer() {
        Log.d(TAG, "makeOffer called");

        // Check if product information is available
        if (productTitle == null) {
            Toast.makeText(this, "Product information not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get product details for the offer
        String productId = getIntent().getStringExtra("productId");
        if (productId == null) {
            Toast.makeText(this, "Product ID not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load product details using existing method
        FirebaseManager.getInstance().getDatabase()
            .getReference("products")
            .child(productId)
            .get()
            .addOnSuccessListener(dataSnapshot -> {
                com.example.tradeup_app.models.Product product =
                    dataSnapshot.getValue(com.example.tradeup_app.models.Product.class);
                if (product != null) {
                    product.setId(dataSnapshot.getKey());
                    runOnUiThread(() -> showChatOfferDialog(product));
                } else {
                    runOnUiThread(() ->
                        Toast.makeText(ChatActivity.this, "Product not found", Toast.LENGTH_SHORT).show());
                }
            })
            .addOnFailureListener(e ->
                runOnUiThread(() ->
                    Toast.makeText(ChatActivity.this, "Error loading product: " + e.getMessage(),
                                 Toast.LENGTH_SHORT).show())
            );
    }

    private void showChatOfferDialog(com.example.tradeup_app.models.Product product) {
        Log.d(TAG, "Showing ChatOfferDialog for product: " + product.getTitle());

        // Create and show offer dialog
        com.example.tradeup_app.dialogs.ChatOfferDialog dialog =
            new com.example.tradeup_app.dialogs.ChatOfferDialog(this, product,
                (offerPrice, message) -> {
                    Log.d(TAG, "Offer submitted: " + offerPrice + " for " + product.getTitle());
                    submitChatOffer(product, offerPrice, message);
                });
        dialog.show();
    }

    private void submitChatOffer(com.example.tradeup_app.models.Product product,
                                double offerPrice, String message) {
        Log.d(TAG, "Submitting chat offer: " + offerPrice + " for " + product.getTitle());

        // Get current user info
        String currentUserId = FirebaseManager.getInstance().getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "Please login to make an offer", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current user name
        String currentUserName = getCurrentUsername();

        // Show progress
        Toast.makeText(this, "Sending offer...", Toast.LENGTH_SHORT).show();

        // Use ChatOfferService to send offer
        com.example.tradeup_app.services.ChatOfferService chatOfferService =
            new com.example.tradeup_app.services.ChatOfferService();

        chatOfferService.sendOfferInChat(
            conversationId,
            product.getId(),
            product.getTitle(),
            currentUserId,
            currentUserName,
            receiverId,
            product.getPrice(),
            offerPrice,
            message,
            new com.example.tradeup_app.services.ChatOfferService.ChatOfferCallback() {
                @Override
                public void onOfferSent(com.example.tradeup_app.models.ChatOffer chatOffer) {
                    runOnUiThread(() -> {
                        Log.d(TAG, "Offer sent successfully: " + chatOffer.getId());
                        Toast.makeText(ChatActivity.this, "💰 Offer sent!", Toast.LENGTH_SHORT).show();
                        // The offer message will appear in chat via real-time listener
                    });
                }

                @Override
                public void onOfferResponded(com.example.tradeup_app.models.ChatOffer chatOffer, String response) {
                    // This callback is for responses, not initial sends
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "Failed to send offer: " + error);
                        Toast.makeText(ChatActivity.this, "Failed to send offer: " + error,
                                     Toast.LENGTH_LONG).show();
                    });
                }
            }
        );
    }

    private String getCurrentUsername() {
        try {
            // Try to get username from CurrentUser helper
            com.example.tradeup_app.auth.Domain.UserModel userModel =
                com.example.tradeup_app.auth.Helper.CurrentUser.getUser();
            if (userModel != null && userModel.getName() != null) {
                return userModel.getName();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get username from CurrentUser", e);
        }

        // Fallback: use Firebase Auth display name
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null && auth.getCurrentUser().getDisplayName() != null) {
            return auth.getCurrentUser().getDisplayName();
        }

        // Final fallback
        return "User";
    }
}
