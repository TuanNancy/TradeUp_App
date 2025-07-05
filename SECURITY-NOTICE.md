# 🚨 SECURITY ALERT: API Keys Detected by GitHub

## Vấn đề
GitHub Secret Scanning đã phát hiện Stripe API keys trong repository và chặn việc push code.

## Giải pháp đã thực hiện

### 1. ✅ Loại bỏ API keys khỏi code
- **server.js**: Xóa hardcoded API key, chỉ sử dụng environment variables
- **.env.example**: Thay thế API keys thực bằng placeholders

### 2. 🔧 Cách setup cho team members

1. **Copy template environment file**:
   ```bash
   cp .env.example .env
   ```

2. **Cập nhật .env với API keys thực của bạn**:
   ```env
   STRIPE_SECRET_KEY=sk_test_YOUR_ACTUAL_KEY_HERE
   STRIPE_PUBLISHABLE_KEY=pk_test_YOUR_ACTUAL_KEY_HERE
   ```

3. **File .env sẽ KHÔNG được push lên GitHub** (đã có trong .gitignore)

### 3. 🚀 Chạy server
```bash
npm start
```

Nếu thiếu .env file, server sẽ hiển thị lỗi và hướng dẫn cách fix.

## ⚠️ Quan trọng cho Team

- **KHÔNG BAO GIỜ** commit API keys thực vào Git
- **LUÔN LUÔN** sử dụng .env file cho sensitive data  
- **KIỂM TRA** .gitignore đã loại trừ .env file
- **SỬ DỤNG** .env.example làm template

## 🔍 Kiểm tra trước khi commit
```bash
git status
git diff --cached
```

Đảm bảo không có API keys nào trong những file sẽ được commit.
