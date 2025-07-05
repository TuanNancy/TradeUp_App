package com.example.tradeup_app.config;

/**
 * Configuration template for Stripe API Keys
 *
 * IMPORTANT:
 * 1. Copy this file to StripeConfig.java
 * 2. Update with your actual Stripe keys
 * 3. Make sure StripeConfig.java is added to .gitignore
 */
public class StripeConfigTemplate {

    // Replace with your actual Stripe Publishable Key
    public static final String PUBLISHABLE_KEY = "pk_test_YOUR_PUBLISHABLE_KEY_HERE";

    // Backend URL for different environments
    public static final String BACKEND_URL_EMULATOR = "http://10.0.2.2:3000";
    public static final String BACKEND_URL_DEVICE = "http://YOUR_LOCAL_IP:3000";

    // Current backend URL (change based on your testing environment)
    public static final String BACKEND_URL = BACKEND_URL_EMULATOR;

    /**
     * Instructions:
     *
     * 1. Get your Stripe keys from: https://dashboard.stripe.com/test/apikeys
     * 2. Replace PUBLISHABLE_KEY with your actual key
     * 3. For device testing, replace YOUR_LOCAL_IP with your computer's IP
     * 4. Save this file as StripeConfig.java (remove Template from name)
     */
}
