package com.example.tradeup_app.services;

import android.content.Context;
import android.util.Log;

import com.example.tradeup_app.models.Product;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.Stripe;
import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.PaymentMethodCreateParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class StripePaymentService {
    private static final String TAG = "StripePaymentService";
    private static final String PUBLISHABLE_KEY = "pk_test_51RhYnJQ2YtUuH1yG0IWDmejxJ9vx91a81pAj4IGkMi8ZexobRXApwBg7NQOx4MzleRSuHbBXR6bDeVzkW7KNaXbP00ugcWqR5B";

    // Backend server URL - Sử dụng 10.0.2.2 cho Android Emulator kết nối với localhost
    // 10.0.2.2 là địa chỉ đặc biệt trong Android Emulator để truy cập localhost của máy host
    private static final String BACKEND_URL = "http://10.0.2.2:3000";

    private final OkHttpClient httpClient;
    private final Stripe stripe;

    public StripePaymentService(Context context) {
        // Initialize Stripe with publishable key
        PaymentConfiguration.init(context, PUBLISHABLE_KEY);
        this.stripe = new Stripe(context, PUBLISHABLE_KEY);

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public interface PaymentIntentCallback {
        void onSuccess(String clientSecret, String paymentIntentId);
        void onError(String error);
    }

    public interface PaymentConfirmationCallback {
        void onSuccess(PaymentIntent paymentIntent);
        void onError(String error);
    }

    public void createPaymentIntent(Product product, String buyerId, PaymentIntentCallback callback) {
        // Convert price to cents (Stripe uses smallest currency unit)
        // For VND prices, we need to convert to USD first to avoid huge amounts
        double priceInUSD = product.getPrice() / 25000; // 1 USD ≈ 25,000 VND
        long amountInCents = Math.round(priceInUSD * 100);

        // Ensure amount is within Stripe limits (max ~$999,999)
        if (amountInCents > 99999999) { // 999,999.99 USD in cents
            amountInCents = 99999999;
        }
        if (amountInCents < 50) { // Minimum 50 cents
            amountInCents = 50;
        }

        JSONObject json = new JSONObject();
        try {
            json.put("amount", amountInCents);
            json.put("currency", "usd");
            json.put("product_id", product.getId());
            json.put("buyer_id", buyerId);
            json.put("seller_id", product.getSellerId());

            JSONObject metadata = new JSONObject();
            metadata.put("product_title", product.getTitle());
            metadata.put("seller_id", product.getSellerId());
            metadata.put("buyer_id", buyerId);
            metadata.put("original_price_vnd", String.valueOf(product.getPrice()));
            json.put("metadata", metadata);

            Log.d(TAG, "PaymentIntent request - Original VND: " + product.getPrice() +
                  ", USD equivalent: $" + String.format("%.2f", priceInUSD) +
                  ", Amount in cents: " + amountInCents);

        } catch (JSONException e) {
            callback.onError("Error creating payment data: " + e.getMessage());
            return;
        }

        // Call backend to create PaymentIntent
        createPaymentIntentOnBackend(json, callback);
    }

    private void createPaymentIntentOnBackend(JSONObject paymentData, PaymentIntentCallback callback) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(paymentData.toString(), JSON);

        Log.d(TAG, "Attempting to connect to backend: " + BACKEND_URL + "/create-payment-intent");
        Log.d(TAG, "Request payload: " + paymentData.toString());

        Request request = new Request.Builder()
                .url(BACKEND_URL + "/create-payment-intent")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to connect to backend server: " + BACKEND_URL, e);
                Log.e(TAG, "Error details: " + e.getMessage());
                // Fallback to mock data for development
                simulateBackendPaymentIntent(callback);
            }

            @Override
            public void onResponse(Call call, Response response) {
                Log.d(TAG, "Backend response code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "Backend response: " + responseBody);
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        String clientSecret = jsonResponse.getString("client_secret");
                        String paymentIntentId = jsonResponse.getString("id");

                        Log.d(TAG, "Real PaymentIntent created: " + paymentIntentId);
                        callback.onSuccess(clientSecret, paymentIntentId);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing payment intent response", e);
                        callback.onError("Error parsing server response");
                    }
                } else {
                    Log.e(TAG, "Backend returned error: " + response.code());
                    // Fallback to mock data for development
                    simulateBackendPaymentIntent(callback);
                }
            }
        });
    }

    private void simulateBackendPaymentIntent(PaymentIntentCallback callback) {
        // This simulates what your backend would do - FOR DEVELOPMENT ONLY
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Simulate network delay

                String mockPaymentIntentId = "pi_mock_" + System.currentTimeMillis();
                String mockClientSecret = mockPaymentIntentId + "_secret_mock";

                Log.d(TAG, "Using mock PaymentIntent: " + mockPaymentIntentId);
                callback.onSuccess(mockClientSecret, mockPaymentIntentId);

            } catch (InterruptedException e) {
                callback.onError("Payment intent creation failed");
            }
        }).start();
    }

    public void confirmPayment(String clientSecret, PaymentMethodCreateParams paymentMethodParams,
                              PaymentConfirmationCallback callback) {

        if (clientSecret.contains("mock")) {
            // Handle mock payment for development
            handleMockPayment(clientSecret, callback);
            return;
        }

        // Extract PaymentIntent ID from client secret
        String paymentIntentId = clientSecret.split("_secret_")[0];
        Log.d(TAG, "Confirming payment with backend API: " + paymentIntentId);

        // Call backend to confirm PaymentIntent
        confirmPaymentOnBackend(paymentIntentId, callback);
    }

    private void confirmPaymentOnBackend(String paymentIntentId, PaymentConfirmationCallback callback) {
        Log.d(TAG, "=== PAYMENT CONFIRMATION DEBUG ===");
        Log.d(TAG, "PaymentIntent ID to confirm: " + paymentIntentId);
        Log.d(TAG, "PaymentIntent ID length: " + paymentIntentId.length());
        Log.d(TAG, "PaymentIntent ID starts with pi_: " + paymentIntentId.startsWith("pi_"));

        JSONObject json = new JSONObject();
        try {
            json.put("payment_intent_id", paymentIntentId);
            Log.d(TAG, "Request JSON created: " + json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON request body", e);
            callback.onError("Error creating confirm payment data: " + e.getMessage());
            return;
        }

        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(json.toString(), JSON);

        Request request = new Request.Builder()
                .url(BACKEND_URL + "/confirm-payment-intent")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();

        Log.d(TAG, "Making request to: " + BACKEND_URL + "/confirm-payment-intent");
        Log.d(TAG, "Request body: " + json.toString());

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network failure during payment confirmation", e);
                callback.onError("Network error during payment confirmation: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                String responseBody = "";
                try {
                    if (response.body() != null) {
                        responseBody = response.body().string();
                    }

                    Log.d(TAG, "=== BACKEND RESPONSE DEBUG ===");
                    Log.d(TAG, "Response code: " + response.code());
                    Log.d(TAG, "Response message: " + response.message());
                    Log.d(TAG, "Response headers: " + response.headers().toString());
                    Log.d(TAG, "Response body: " + responseBody);

                    if (response.isSuccessful()) {
                        Log.d(TAG, "Payment confirmation successful");

                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String status = jsonResponse.getString("status");
                        String id = jsonResponse.getString("id");

                        Log.d(TAG, "Payment confirmed successfully. ID: " + id + ", Status: " + status);
                        callback.onSuccess(null);

                    } else {
                        Log.e(TAG, "Backend confirm payment returned error: " + response.code());
                        Log.e(TAG, "Error response body: " + responseBody);

                        // Try to parse error details
                        String errorMessage = "Payment confirmation failed with code: " + response.code();
                        try {
                            JSONObject errorJson = new JSONObject(responseBody);
                            if (errorJson.has("error")) {
                                errorMessage += " - " + errorJson.getString("error");
                            }
                            if (errorJson.has("received_body")) {
                                Log.e(TAG, "Backend received body: " + errorJson.get("received_body"));
                            }
                            if (errorJson.has("available_keys")) {
                                Log.e(TAG, "Available keys in backend: " + errorJson.get("available_keys"));
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Could not parse error response JSON", e);
                        }

                        callback.onError(errorMessage + "\nResponse: " + responseBody);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing confirm payment response", e);
                    callback.onError("Error processing payment confirmation response: " + e.getMessage());
                } finally {
                    // Fix connection leak warning - always close response body
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    private void handleMockPayment(String clientSecret, PaymentConfirmationCallback callback) {
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Simulate payment processing

                Log.d(TAG, "Processing mock payment: " + clientSecret);

                // For development, we'll create a success response
                callback.onSuccess(null); // Passing null since we can't create real PaymentIntent

            } catch (InterruptedException e) {
                callback.onError("Mock payment processing failed");
            }
        }).start();
    }

    public PaymentMethodCreateParams createCardPaymentMethod(String cardNumber, int expMonth,
                                                           int expYear, String cvc) {
        PaymentMethodCreateParams.Card.Builder cardBuilder = new PaymentMethodCreateParams.Card.Builder()
                .setNumber(cardNumber.replaceAll("\\s+", ""))
                .setExpiryMonth(expMonth)
                .setExpiryYear(expYear)
                .setCvc(cvc);

        return PaymentMethodCreateParams.create(cardBuilder.build());
    }

    // Payment validation utilities
    public static class PaymentValidation {
        public static boolean isValidCardNumber(String cardNumber) {
            if (cardNumber == null || cardNumber.trim().isEmpty()) {
                return false;
            }

            String cleanNumber = cardNumber.replaceAll("\\s+", "");

            // Basic length and format validation
            if (cleanNumber.length() < 13 || cleanNumber.length() > 19 || !cleanNumber.matches("\\d+")) {
                return false;
            }

            // Luhn algorithm validation
            return isValidLuhn(cleanNumber);
        }

        private static boolean isValidLuhn(String cardNumber) {
            int sum = 0;
            boolean alternate = false;

            for (int i = cardNumber.length() - 1; i >= 0; i--) {
                int digit = Integer.parseInt(cardNumber.substring(i, i + 1));

                if (alternate) {
                    digit *= 2;
                    if (digit > 9) {
                        digit = (digit % 10) + 1;
                    }
                }

                sum += digit;
                alternate = !alternate;
            }

            return (sum % 10 == 0);
        }

        public static boolean isValidExpiryMonth(int month) {
            return month >= 1 && month <= 12;
        }

        public static boolean isValidExpiryYear(int year) {
            int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
            return year >= currentYear && year <= currentYear + 20;
        }

        public static boolean isValidCvc(String cvc) {
            return cvc != null && cvc.length() >= 3 && cvc.length() <= 4 &&
                   cvc.matches("\\d+");
        }
    }
}
