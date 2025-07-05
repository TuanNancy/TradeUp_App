# Hướng dẫn hoàn thành thiết lập Stripe Payment

## ✅ Đã hoàn thành:

1. **Cập nhật Stripe Keys thực:**
   - Publishable Key: `pk_test_51RhYnJQ2YtUuH1yG0IWDmejxJ9vx91a81pAj4IGkMi8ZexobRXApwBg7NQOx4MzleRSuHbBXR6bDeVzkW7KNaXbP00ugcWqR5B`
   - Secret Key: `sk_test_51RhYnJQ2YtUuH1yG...` (đã được cấu hình trong server.js)

2. **Backend Server đã sẵn sàng:**
   - File `server.js` đã được tạo với Stripe integration thực
   - Dependencies đã được cài đặt
   - Server đang chạy trên port 3000

3. **Ngrok đã được thiết lập:**
   - Ngrok đã được cài đặt và đang chạy
   - Server local đã được expose ra internet

## 🔧 Bước tiếp theo cần thực hiện:

### 1. Lấy ngrok URL và cập nhật Android app:

**Cách 1: Kiểm tra ngrok web interface**
- Mở trình duyệt và truy cập: http://localhost:4040
- Copy URL HTTPS từ ngrok dashboard (dạng: `https://xxxxx.ngrok.io`)

**Cách 2: Kiểm tra terminal ngrok**
- Tìm dòng có chữ "Forwarding" trong terminal ngrok
- Copy URL HTTPS (không phải HTTP)

### 2. Cập nhật URL trong StripePaymentService.java:

Thay đổi dòng này trong file `StripePaymentService.java`:
```java
private static final String BACKEND_URL = "http://localhost:3000";
```

Thành:
```java
private static final String BACKEND_URL = "https://YOUR-NGROK-URL.ngrok.io";
```

Ví dụ:
```java
private static final String BACKEND_URL = "https://abc123.ngrok.io";
```

### 3. Test thanh toán:

**Thẻ test Stripe:**
- Số thẻ: `4242424242424242`
- CVC: `123` 
- Ngày hết hạn: `12/25` (bất kỳ ngày tương lai)

**Kiểm tra logs:**
```bash
adb logcat | grep "PaymentActivity\|StripePaymentService"
```

### 4. Kiểm tra backend hoạt động:

**Test health check:**
- Truy cập: `https://YOUR-NGROK-URL.ngrok.io` trong trình duyệt
- Sẽ thấy: `{"message": "TradeUp Stripe Payment Server is running!"}`

**Monitor server logs:**
- Backend sẽ log tất cả PaymentIntent requests
- Kiểm tra Stripe Dashboard để thấy payments: https://dashboard.stripe.com

## 🎯 Tóm tắt:

1. ✅ Backend server đang chạy với Stripe keys thực
2. ✅ Ngrok đang expose server ra internet  
3. ⏳ **CẦN LÀM:** Lấy ngrok URL và cập nhật vào `StripePaymentService.java`
4. ⏳ **TEST:** Chạy app và thử thanh toán với thẻ test

Sau khi cập nhật ngrok URL, ứng dụng sẽ kết nối với Stripe thực 100% và hiển thị dữ liệu thanh toán thực tế!
