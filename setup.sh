#!/bin/bash

# TradeUp App Setup Script
# Chạy script này để setup dự án tự động

echo "🚀 Setting up TradeUp App..."

# Check Node.js
if ! command -v node &> /dev/null; then
    echo "❌ Node.js chưa được cài đặt. Vui lòng cài đặt Node.js v16+ trước."
    exit 1
fi

echo "✅ Node.js version: $(node --version)"

# Check npm
if ! command -v npm &> /dev/null; then
    echo "❌ npm chưa được cài đặt."
    exit 1
fi

echo "✅ npm version: $(npm --version)"

# Install Node.js dependencies
echo "📦 Installing Node.js dependencies..."
npm install

if [ $? -eq 0 ]; then
    echo "✅ Node.js dependencies installed successfully"
else
    echo "❌ Failed to install Node.js dependencies"
    exit 1
fi

# Check if google-services.json exists
if [ ! -f "app/google-services.json" ]; then
    echo "⚠️  WARNING: app/google-services.json not found!"
    echo "   Vui lòng tải file này từ Firebase Console và đặt vào app/"
    echo "   Xem hướng dẫn trong README-SETUP.md"
fi

# Check if .env exists
if [ ! -f ".env" ]; then
    echo "⚠️  WARNING: .env file not found!"
    echo "   Copy .env.example to .env và cập nhật Stripe API keys"
    echo "   cp .env.example .env"
fi

# Check Gradle wrapper
if [ -f "./gradlew" ]; then
    echo "🔧 Setting up Android project..."
    chmod +x ./gradlew
    echo "✅ Gradle wrapper ready"
else
    echo "❌ gradlew not found. This might not be an Android project root."
fi

echo ""
echo "🎉 Setup completed!"
echo ""
echo "📋 Next steps:"
echo "1. Cấu hình Firebase: Đặt google-services.json vào app/"
echo "2. Cấu hình Stripe: Copy .env.example to .env và cập nhật API keys"
echo "3. Chạy server: npm start hoặc node server.js"
echo "4. Mở Android Studio và import project"
echo ""
echo "📖 Xem chi tiết trong README-SETUP.md"
