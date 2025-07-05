const express = require('express');
const stripe = require('stripe')('sk_test_your_secret_key_here');
const app = express();

// Add request logging middleware FIRST
app.use((req, res, next) => {
    console.log(`📨 ${new Date().toISOString()} - ${req.method} ${req.path} from ${req.ip}`);
    next();
});

app.use(express.json());

// CORS middleware để cho phép Android app kết nối
app.use((req, res, next) => {
    res.header('Access-Control-Allow-Origin', '*');
    res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept, Authorization');

    if (req.method === 'OPTIONS') {
        res.sendStatus(200);
    } else {
        next();
    }
});

// Health check endpoint
app.get('/', (req, res) => {
    res.json({
        message: 'TradeUp Stripe Payment Server is running!',
        timestamp: new Date().toISOString()
    });
});

// Create PaymentIntent endpoint
app.post('/create-payment-intent', async (req, res) => {
    try {
        const { amount, currency, product_id, buyer_id, seller_id, metadata } = req.body;

        console.log('📦 Creating PaymentIntent for:', {
            amount,
            currency,
            product_id,
            buyer_id,
            seller_id
        });

        // Validate required fields
        if (!amount || !product_id || !buyer_id || !seller_id) {
            console.log('❌ Missing required fields');
            return res.status(400).json({
                error: 'Missing required fields: amount, product_id, buyer_id, seller_id'
            });
        }

        // Create PaymentIntent with Stripe
        const paymentIntent = await stripe.paymentIntents.create({
            amount: amount,
            currency: currency || 'usd',
            metadata: {
                product_id,
                buyer_id,
                seller_id,
                ...metadata
            },
            automatic_payment_methods: {
                enabled: true,
                allow_redirects: 'never' // Prevent redirect-based payment methods
            },
        });

        console.log('✅ PaymentIntent created successfully:', paymentIntent.id);

        res.json({
            client_secret: paymentIntent.client_secret,
            id: paymentIntent.id,
            amount: paymentIntent.amount,
            currency: paymentIntent.currency,
            status: paymentIntent.status
        });

    } catch (error) {
        console.error('❌ Error creating PaymentIntent:', error.message);
        res.status(400).json({
            error: error.message,
            type: error.type || 'api_error'
        });
    }
});

// Confirm PaymentIntent endpoint
app.post('/confirm-payment-intent', async (req, res) => {
    try {
        const { payment_intent_id } = req.body;

        console.log('🔄 Confirming PaymentIntent:', payment_intent_id);
        console.log('📋 Request body:', JSON.stringify(req.body, null, 2));
        console.log('📋 Request headers:', JSON.stringify(req.headers, null, 2));
        console.log('📋 Content-Type:', req.headers['content-type']);
        console.log('📋 Raw body type:', typeof req.body);

        // Validate required fields
        if (!payment_intent_id) {
            console.log('❌ Missing payment_intent_id in request body');
            console.log('❌ Available keys in body:', Object.keys(req.body));
            return res.status(400).json({
                error: 'Missing required field: payment_intent_id',
                received_body: req.body,
                available_keys: Object.keys(req.body)
            });
        }

        // Validate payment_intent_id format
        if (typeof payment_intent_id !== 'string' || !payment_intent_id.startsWith('pi_')) {
            console.log('❌ Invalid payment_intent_id format:', payment_intent_id);
            return res.status(400).json({
                error: 'Invalid payment_intent_id format. Must be a string starting with "pi_"',
                received: payment_intent_id,
                type: typeof payment_intent_id
            });
        }

        // First, retrieve the PaymentIntent to check its status
        console.log('🔍 Retrieving PaymentIntent details...');
        const existingPaymentIntent = await stripe.paymentIntents.retrieve(payment_intent_id);
        console.log('📊 PaymentIntent status:', existingPaymentIntent.status);
        console.log('📊 PaymentIntent amount:', existingPaymentIntent.amount);
        console.log('📊 PaymentIntent currency:', existingPaymentIntent.currency);

        // Check if PaymentIntent is already confirmed
        if (existingPaymentIntent.status === 'succeeded') {
            console.log('✅ PaymentIntent already confirmed');
            return res.json({
                id: existingPaymentIntent.id,
                status: existingPaymentIntent.status,
                amount: existingPaymentIntent.amount,
                currency: existingPaymentIntent.currency,
                message: 'Payment already confirmed'
            });
        }

        // For testing purposes, create a test payment method and confirm
        console.log('💳 Creating test payment method for confirmation...');

        try {
            // Create a test payment method
            const paymentMethod = await stripe.paymentMethods.create({
                type: 'card',
                card: {
                    token: 'tok_visa', // Stripe test token
                },
            });

            console.log('💳 Test payment method created:', paymentMethod.id);

            // Confirm PaymentIntent with the created payment method
            console.log('🔄 Confirming PaymentIntent with payment method...');
            const paymentIntent = await stripe.paymentIntents.confirm(payment_intent_id, {
                payment_method: paymentMethod.id,
                return_url: 'https://your-website.com/return', // Required for some payment methods
            });

            console.log('🎉 PaymentIntent confirmed successfully:', paymentIntent.id, 'Status:', paymentIntent.status);

            res.json({
                id: paymentIntent.id,
                status: paymentIntent.status,
                amount: paymentIntent.amount,
                currency: paymentIntent.currency,
                payment_method_id: paymentMethod.id
            });

        } catch (paymentError) {
            console.error('💳 Error with payment method creation/confirmation:', paymentError.message);

            // Fallback: Try to confirm with a direct test payment method
            console.log('🔄 Fallback: Trying direct confirmation with test payment method...');
            try {
                const paymentIntent = await stripe.paymentIntents.confirm(payment_intent_id, {
                    payment_method: 'pm_card_visa',
                });

                res.json({
                    id: paymentIntent.id,
                    status: paymentIntent.status,
                    amount: paymentIntent.amount,
                    currency: paymentIntent.currency,
                    payment_method_id: 'pm_card_visa'
                });

            } catch (fallbackError) {
                console.error('❌ Fallback confirmation also failed:', fallbackError.message);
                throw fallbackError; // Re-throw to be caught by outer catch block
            }
        }
    } catch (error) {
        console.error('❌ Error confirming PaymentIntent:', error.message);
        console.error('❌ Error type:', error.type);
        console.error('❌ Error code:', error.code);
        console.error('❌ Error param:', error.param);
        console.error('❌ Error decline_code:', error.decline_code);
        console.error('❌ Full error object:', JSON.stringify(error, null, 2));

        res.status(400).json({
            error: error.message,
            type: error.type || 'api_error',
            code: error.code,
            param: error.param,
            decline_code: error.decline_code
        });
    }
});

// Webhook endpoint for Stripe events (optional but recommended)
app.post('/webhook', express.raw({type: 'application/json'}), (req, res) => {
    const sig = req.headers['stripe-signature'];

    try {
        console.log('🔔 Webhook received:', req.body.toString());
        res.json({received: true});
    } catch (err) {
        console.error('❌ Webhook error:', err.message);
        res.status(400).send(`Webhook Error: ${err.message}`);
    }
});

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
    console.log(`🚀 TradeUp Payment Server running on port ${PORT}`);
    console.log(`📱 Ready to accept payments from Android app`);
    console.log(`🔗 Health check: http://localhost:${PORT}`);
    console.log(`🔗 Emulator should connect via: http://10.0.2.2:${PORT}`);
    console.log(`📊 Current time: ${new Date().toISOString()}`);
});

module.exports = app;
