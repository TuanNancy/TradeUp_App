# Hướng dẫn thiết lập Backend Server cho Stripe Payment

## Vấn đề hiện tại
Ứng dụng Android đang sử dụng mock data thay vì kết nối thực tế với Stripe API. Để Stripe hiển thị dữ liệu thực, bạn cần:

1. **Backend server** để tạo PaymentIntent an toàn
2. **Secret Key** của Stripe (không được đặt trong app Android)
3. **Webhook** để xử lý kết quả thanh toán

## Giải pháp nhanh (Development)

### Option 1: Sử dụng Node.js Backend

Tạo file `server.js`:

```javascript
const express = require('express');
const stripe = require('stripe')('sk_test_YOUR_SECRET_KEY_HERE'); // Thay bằng secret key thật
const app = express();

app.use(express.json());

// CORS middleware
app.use((req, res, next) => {
    res.header('Access-Control-Allow-Origin', '*');
    res.header('Access-Control-Allow-Headers', 'Content-Type');
    next();
});

app.post('/create-payment-intent', async (req, res) => {
    try {
        const { amount, currency, product_id, buyer_id, seller_id, metadata } = req.body;

        const paymentIntent = await stripe.paymentIntents.create({
            amount: amount,
            currency: currency || 'vnd',
            metadata: metadata,
            automatic_payment_methods: {
                enabled: true,
            },
        });

        res.json({
            client_secret: paymentIntent.client_secret,
            id: paymentIntent.id
        });
    } catch (error) {
        res.status(400).json({ error: error.message });
    }
});

app.listen(3000, () => {
    console.log('Server running on port 3000');
});
```

Chạy server:
```bash
npm init -y
npm install express stripe
node server.js
```

### Option 2: Sử dụng ngrok để expose local server

```bash
# Cài đặt ngrok
npm install -g ngrok

# Chạy server local trên port 3000
node server.js

# Trong terminal khác, expose server ra internet
ngrok http 3000
```

Sau đó update `BACKEND_URL` trong `StripePaymentService.java`:
```java
private static final String BACKEND_URL = "https://your-ngrok-url.ngrok.io";
```

## Cách kiểm tra và debug

### 1. Kiểm tra logs trong Android Studio
```
adb logcat | grep "PaymentActivity\|StripePaymentService"
```

### 2. Test với thẻ test của Stripe
- Visa: `4242424242424242`
- Visa (debit): `4000056655665556`
- Mastercard: `5555555555554444`
- CVV: `123`
- Expiry: Bất kỳ ngày trong tương lai (vd: `12/25`)

### 3. Kiểm tra Stripe Dashboard
- Đăng nhập vào https://dashboard.stripe.com
- Vào mục "Payments" để xem các giao dịch test

## Ghi chú quan trọng

1. **Secret Key**: Không bao giờ đặt secret key trong app Android
2. **Test Mode**: Chỉ sử dụng test keys cho development
3. **Production**: Cần server backend thật với HTTPS
4. **VND Currency**: Cần verify Stripe có hỗ trợ VND hay không
5. **Webhook**: Để xử lý trạng thái thanh toán cuối cùng

## Troubleshooting

### Nếu vẫn không hiển thị dữ liệu:
1. Kiểm tra internet connection
2. Kiểm tra Stripe publishable key có đúng không
3. Kiểm tra backend server có chạy không
4. Kiểm tra logs để tìm lỗi cụ thể

### Error phổ biến:
- `Network error`: Backend server không accessible
- `Invalid publishable key`: Key không đúng hoặc expired
- `Currency not supported`: VND có thể không được hỗ trợ

## Tình trạng hiện tại

Sau khi cập nhật code:
- ✅ Đã thêm Stripe Android SDK
- ✅ Đã cập nhật StripePaymentService để sử dụng Stripe thực
- ✅ Đã cải thiện PaymentActivity với logging chi tiết
- ⚠️ Cần thiết lập backend server để hoàn toàn loại bỏ mock data
- ⚠️ Cần kiểm tra currency VND có được Stripe hỗ trợ không

Bây giờ app sẽ thử kết nối với Stripe thực trước, nếu thất bại mới fallback về mock data.
