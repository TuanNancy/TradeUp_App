# TradeUp App - Setup Guide for Team Members

## 🚀 Hướng dẫn thiết lập dự án cho thành viên nhóm

### 📋 Yêu cầu hệ thống

1. **Android Studio** (phiên bản mới nhất)
2. **Node.js** (v16 trở lên)
3. **Java JDK** (v11 trở lên)
4. **Git**

### 🔧 Bước 1: Clone và cài đặt dependencies

```bash
# Clone repository
git clone [YOUR_GITHUB_REPO_URL]
cd TradeUp_App

# Cài đặt Node.js dependencies cho backend
npm install

# Sync Android project
./gradlew clean
./gradlew build
```

### 🔑 Bước 2: Cấu hình Firebase

1. **Tải file `google-services.json`** từ Firebase Console
2. **Đặt file vào**: `app/google-services.json`
3. **Cập nhật Firebase Database Rules** (sử dụng file `firebase_database_rules.json`)

### 💳 Bước 3: Cấu hình Stripe (cho Payment)

1. **Tạo tài khoản Stripe Test**: https://dashboard.stripe.com/test
2. **Lấy API Keys**:
   - Publishable Key: `pk_test_...`
   - Secret Key: `sk_test_...`

3. **Cập nhật trong code**:
   - File `app/src/main/java/.../services/StripePaymentService.java`:
     ```java
     private static final String PUBLISHABLE_KEY = "pk_test_YOUR_KEY_HERE";
     ```
   - File `server.js`:
     ```javascript
     const stripe = require('stripe')('sk_test_YOUR_SECRET_KEY_HERE');
     ```

### 🌐 Bước 4: Chạy Backend Server

```bash
# Chạy server trên port 3000
node server.js
```

Server sẽ chạy tại: `http://localhost:3000`

### 📱 Bước 5: Chạy Android App

1. **Mở Android Studio**
2. **Import project** từ thư mục đã clone
3. **Sync project với Gradle files**
4. **Chạy app trên emulator hoặc device**

### 🔍 Kiểm tra kết nối

1. **Test Backend**: Truy cập `http://localhost:3000` - sẽ thấy message "TradeUp Stripe Payment Server is running!"
2. **Test Android**: Mở app và thử chức năng payment

### 🐛 Troubleshooting

#### Lỗi Firebase
```
Could not find google-services.json
```
**Giải pháp**: Đảm bảo file `google-services.json` đã được đặt đúng vị trí trong `app/`

#### Lỗi Payment
```
Backend confirm payment returned error: 400
```
**Giải pháp**: Kiểm tra lại Stripe API keys và đảm bảo server đang chạy

#### Lỗi Network
```
Failed to connect to backend server
```
**Giải pháp**: 
- Đảm bảo server đang chạy (`node server.js`)
- Với emulator: sử dụng `http://10.0.2.2:3000`
- Với device thật: sử dụng IP máy của bạn

### 📞 Liên hệ

Nếu gặp vấn đề, liên hệ với team leader hoặc tạo issue trên GitHub repo.

---

## 🔄 Git Workflow

1. **Pull latest changes**: `git pull origin main`
2. **Create feature branch**: `git checkout -b feature/your-feature`
3. **Make changes and commit**: `git add . && git commit -m "Your message"`
4. **Push and create PR**: `git push origin feature/your-feature`

---

**Happy Coding! 🚀**
