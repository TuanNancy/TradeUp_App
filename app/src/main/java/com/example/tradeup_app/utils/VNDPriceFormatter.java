package com.example.tradeup_app.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

public class VNDPriceFormatter {

    // Format VND với dấu phẩy phân cách nghìn
    public static String formatVND(double price) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
        symbols.setGroupingSeparator(','); // Sử dụng dấu phẩy cho phân cách nghìn
        formatter.setDecimalFormatSymbols(symbols);
        return formatter.format(price) + " VNĐ";
    }

    // Format VND không có đơn vị (chỉ số)
    public static String formatVNDNumber(double price) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
        symbols.setGroupingSeparator(',');
        formatter.setDecimalFormatSymbols(symbols);
        return formatter.format(price);
    }

    // Parse VND từ string có dấu phẩy thành double
    public static double parseVND(String priceText) {
        if (priceText == null || priceText.trim().isEmpty()) {
            return 0.0;
        }

        // Loại bỏ VNĐ, dấu phẩy và khoảng trắng
        String cleanText = priceText.replace("VNĐ", "")
                                   .replace(",", "")
                                   .replace(".", "")
                                   .trim();

        try {
            return Double.parseDouble(cleanText);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // TextWatcher để format giá VNĐ tự động khi nhập
    public static class VNDTextWatcher implements TextWatcher {
        private EditText editText;
        private boolean isFormatting = false;

        public VNDTextWatcher(EditText editText) {
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (isFormatting) return;

            isFormatting = true;

            String text = s.toString();

            // Loại bỏ tất cả ký tự không phải số
            String digitsOnly = text.replaceAll("[^0-9]", "");

            if (!digitsOnly.isEmpty()) {
                try {
                    long value = Long.parseLong(digitsOnly);
                    String formatted = formatVNDNumber(value);

                    s.replace(0, s.length(), formatted);

                    // Đặt cursor ở cuối
                    editText.setSelection(formatted.length());
                } catch (NumberFormatException e) {
                    // Nếu số quá lớn, giữ nguyên text
                }
            }

            isFormatting = false;
        }
    }

    // Validate giá VNĐ
    public static boolean isValidVNDPrice(String priceText) {
        double price = parseVND(priceText);
        return price > 0 && price <= 999_999_999_999L; // Tối đa 999 tỷ VNĐ
    }

    // Format giá cho hiển thị ngắn gọn (1M, 1B, etc.)
    public static String formatVNDShort(double price) {
        if (price >= 1_000_000_000) {
            return String.format("%.1f tỷ", price / 1_000_000_000);
        } else if (price >= 1_000_000) {
            return String.format("%.1f triệu", price / 1_000_000);
        } else if (price >= 1_000) {
            return String.format("%.0f nghìn", price / 1_000);
        } else {
            return formatVNDNumber(price);
        }
    }
}
