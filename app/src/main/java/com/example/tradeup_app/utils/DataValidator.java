package com.example.tradeup_app.utils;

import com.example.tradeup_app.models.Product;
import com.example.tradeup_app.models.Message;
import com.example.tradeup_app.models.Offer;
import com.example.tradeup_app.models.Rating;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Data validation utility to ensure data consistency across the application
 */
public class DataValidator {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");

    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^[0-9]{10,11}$");

    public static class ValidationResult {
        private boolean isValid;
        private String errorMessage;

        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() { return isValid; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Validate product data before saving to Firebase
     */
    public static ValidationResult validateProduct(Product product) {
        if (product == null) {
            return new ValidationResult(false, "Sản phẩm không được để trống");
        }

        // Title validation
        if (isEmpty(product.getTitle())) {
            return new ValidationResult(false, "Tên sản phẩm không được để trống");
        }
        if (product.getTitle().length() < Constants.MIN_PRODUCT_TITLE_LENGTH) {
            return new ValidationResult(false, "Tên sản phẩm phải có ít nhất " + Constants.MIN_PRODUCT_TITLE_LENGTH + " ký tự");
        }
        if (product.getTitle().length() > Constants.MAX_PRODUCT_TITLE_LENGTH) {
            return new ValidationResult(false, "Tên sản phẩm không được vượt quá " + Constants.MAX_PRODUCT_TITLE_LENGTH + " ký tự");
        }

        // Description validation
        if (isEmpty(product.getDescription())) {
            return new ValidationResult(false, "Mô tả sản phẩm không được để trống");
        }
        if (product.getDescription().length() < Constants.MIN_PRODUCT_DESCRIPTION_LENGTH) {
            return new ValidationResult(false, "Mô tả sản phẩm phải có ít nhất " + Constants.MIN_PRODUCT_DESCRIPTION_LENGTH + " ký tự");
        }
        if (product.getDescription().length() > Constants.MAX_PRODUCT_DESCRIPTION_LENGTH) {
            return new ValidationResult(false, "Mô tả sản phẩm không được vượt quá " + Constants.MAX_PRODUCT_DESCRIPTION_LENGTH + " ký tự");
        }

        // Price validation
        if (product.getPrice() < Constants.MIN_PRODUCT_PRICE) {
            return new ValidationResult(false, "Giá sản phẩm phải lớn hơn " + Constants.MIN_PRODUCT_PRICE);
        }
        if (product.getPrice() > Constants.MAX_PRODUCT_PRICE) {
            return new ValidationResult(false, "Giá sản phẩm không được vượt quá " + Constants.MAX_PRODUCT_PRICE);
        }

        // Category validation
        if (isEmpty(product.getCategory())) {
            return new ValidationResult(false, "Danh mục sản phẩm không được để trống");
        }
        if (!isValidCategory(product.getCategory())) {
            return new ValidationResult(false, "Danh mục sản phẩm không hợp lệ");
        }

        // Condition validation
        if (isEmpty(product.getCondition())) {
            return new ValidationResult(false, "Tình trạng sản phẩm không được để trống");
        }
        if (!isValidCondition(product.getCondition())) {
            return new ValidationResult(false, "Tình trạng sản phẩm không hợp lệ");
        }

        // Location validation
        if (isEmpty(product.getLocation())) {
            return new ValidationResult(false, "Địa điểm không được để trống");
        }

        // Images validation
        if (product.getImageUrls() == null || product.getImageUrls().isEmpty()) {
            return new ValidationResult(false, "Sản phẩm phải có ít nhất 1 hình ảnh");
        }
        if (product.getImageUrls().size() > Constants.MAX_IMAGES_PER_PRODUCT) {
            return new ValidationResult(false, "Sản phẩm không được có quá " + Constants.MAX_IMAGES_PER_PRODUCT + " hình ảnh");
        }

        return new ValidationResult(true, null);
    }

    /**
     * Validate message data before sending
     */
    public static ValidationResult validateMessage(Message message) {
        if (message == null) {
            return new ValidationResult(false, "Tin nhắn không được để trống");
        }

        if (isEmpty(message.getContent()) && !Constants.MESSAGE_TYPE_IMAGE.equals(message.getMessageType())) {
            return new ValidationResult(false, "Nội dung tin nhắn không được để trống");
        }

        if (isEmpty(message.getSenderId())) {
            return new ValidationResult(false, "ID người gửi không được để trống");
        }

        if (isEmpty(message.getReceiverId())) {
            return new ValidationResult(false, "ID người nhận không được để trống");
        }

        if (isEmpty(message.getConversationId())) {
            return new ValidationResult(false, "ID cuộc hội thoại không được để trống");
        }

        return new ValidationResult(true, null);
    }

    /**
     * Validate offer data before submitting
     */
    public static ValidationResult validateOffer(Offer offer) {
        if (offer == null) {
            return new ValidationResult(false, "Đề nghị không được để trống");
        }

        if (isEmpty(offer.getProductId())) {
            return new ValidationResult(false, "ID sản phẩm không được để trống");
        }

        if (isEmpty(offer.getBuyerId())) {
            return new ValidationResult(false, "ID người mua không được để trống");
        }

        if (isEmpty(offer.getSellerId())) {
            return new ValidationResult(false, "ID người bán không được để trống");
        }

        if (offer.getOfferPrice() <= 0) {
            return new ValidationResult(false, "Giá đề nghị phải lớn hơn 0");
        }

        if (offer.getOfferPrice() > offer.getOriginalPrice() * 2) {
            return new ValidationResult(false, "Giá đề nghị không được vượt quá 2 lần giá gốc");
        }

        return new ValidationResult(true, null);
    }

    /**
     * Validate rating data before submitting
     */
    public static ValidationResult validateRating(Rating rating) {
        if (rating == null) {
            return new ValidationResult(false, "Đánh giá không được để trống");
        }

        if (isEmpty(rating.getRaterId())) {
            return new ValidationResult(false, "ID người đánh giá không được để trống");
        }

        if (isEmpty(rating.getRatedUserId())) {
            return new ValidationResult(false, "ID người được đánh giá không được để trống");
        }

        if (rating.getStars() < Constants.MIN_RATING || rating.getStars() > Constants.MAX_RATING) {
            return new ValidationResult(false, "Số sao phải từ " + Constants.MIN_RATING + " đến " + Constants.MAX_RATING);
        }

        if (isEmpty(rating.getReview())) {
            return new ValidationResult(false, "Nội dung đánh giá không được để trống");
        }

        if (rating.getReview().length() < 5) {
            return new ValidationResult(false, "Nội dung đánh giá phải có ít nhất 5 ký tự");
        }

        return new ValidationResult(true, null);
    }

    /**
     * Validate email format
     */
    public static ValidationResult validateEmail(String email) {
        if (isEmpty(email)) {
            return new ValidationResult(false, "Email không được để trống");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return new ValidationResult(false, "Định dạng email không hợp lệ");
        }

        return new ValidationResult(true, null);
    }

    /**
     * Validate phone number format
     */
    public static ValidationResult validatePhoneNumber(String phone) {
        if (isEmpty(phone)) {
            return new ValidationResult(false, "Số điện thoại không được để trống");
        }

        // Remove spaces and dashes
        String cleanPhone = phone.replaceAll("[\\s-]", "");

        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            return new ValidationResult(false, "Số điện thoại phải có 10-11 chữ số");
        }

        return new ValidationResult(true, null);
    }

    /**
     * Validate password strength
     */
    public static ValidationResult validatePassword(String password) {
        if (isEmpty(password)) {
            return new ValidationResult(false, "Mật khẩu không được để trống");
        }

        if (password.length() < 6) {
            return new ValidationResult(false, "Mật khẩu phải có ít nhất 6 ký tự");
        }

        if (password.length() > 50) {
            return new ValidationResult(false, "Mật khẩu không được vượt quá 50 ký tự");
        }

        // Check for at least one letter and one number
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasNumber = password.matches(".*[0-9].*");

        if (!hasLetter || !hasNumber) {
            return new ValidationResult(false, "Mật khẩu phải chứa ít nhất 1 chữ cái và 1 chữ số");
        }

        return new ValidationResult(true, null);
    }

    // Helper methods
    private static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private static boolean isValidCategory(String category) {
        for (String validCategory : Constants.PRODUCT_CATEGORIES) {
            if (validCategory.equals(category)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidCondition(String condition) {
        for (String validCondition : Constants.PRODUCT_CONDITIONS) {
            if (validCondition.equals(condition)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sanitize user input to prevent XSS and injection attacks
     */
    public static String sanitizeInput(String input) {
        if (input == null) return null;

        return input.trim()
                   .replaceAll("<script[^>]*>.*?</script>", "")
                   .replaceAll("<[^>]+>", "")
                   .replaceAll("javascript:", "")
                   .replaceAll("vbscript:", "")
                   .replaceAll("onload", "")
                   .replaceAll("onerror", "")
                   .replaceAll("onclick", "");
    }

    /**
     * Check if a list contains only valid URLs
     */
    public static boolean areValidImageUrls(List<String> urls) {
        if (urls == null) return false;

        for (String url : urls) {
            if (!isValidImageUrl(url)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidImageUrl(String url) {
        if (isEmpty(url)) return false;

        return url.startsWith("https://") &&
               (url.contains("cloudinary.com") || url.contains("firebasestorage.googleapis.com"));
    }
}
