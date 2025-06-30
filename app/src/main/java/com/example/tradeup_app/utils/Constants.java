package com.example.tradeup_app.utils;

/**
 * Constants file to ensure data synchronization across the application
 */
public class Constants {
    
    // Firebase Node Names - MUST match across all components
    public static final String PRODUCTS_NODE = "products";
    public static final String MESSAGES_NODE = "messages";
    public static final String CONVERSATIONS_NODE = "conversations";
    public static final String OFFERS_NODE = "offers";
    public static final String TRANSACTIONS_NODE = "transactions";
    public static final String RATINGS_NODE = "ratings";
    public static final String REPORTS_NODE = "reports";
    public static final String USERS_NODE = "Users";
    public static final String PRODUCT_LIKES_NODE = "product_likes";
    
    // Product Status
    public static final String PRODUCT_STATUS_AVAILABLE = "Available";
    public static final String PRODUCT_STATUS_SOLD = "Sold";
    public static final String PRODUCT_STATUS_PAUSED = "Paused";
    public static final String PRODUCT_STATUS_DELETED = "Deleted";
    
    // Offer Status
    public static final String OFFER_STATUS_PENDING = "PENDING";
    public static final String OFFER_STATUS_ACCEPTED = "ACCEPTED";
    public static final String OFFER_STATUS_REJECTED = "REJECTED";
    public static final String OFFER_STATUS_COUNTERED = "COUNTERED";
    public static final String OFFER_STATUS_EXPIRED = "EXPIRED";
    
    // Transaction Status
    public static final String TRANSACTION_STATUS_PENDING = "PENDING";
    public static final String TRANSACTION_STATUS_COMPLETED = "COMPLETED";
    public static final String TRANSACTION_STATUS_CANCELLED = "CANCELLED";
    
    // Message Types
    public static final String MESSAGE_TYPE_TEXT = "text";
    public static final String MESSAGE_TYPE_IMAGE = "image";
    public static final String MESSAGE_TYPE_OFFER = "offer";
    public static final String MESSAGE_TYPE_SYSTEM = "system";
    
    // Report Types
    public static final String REPORT_TYPE_USER = "USER";
    public static final String REPORT_TYPE_PRODUCT = "PRODUCT";
    public static final String REPORT_TYPE_CONVERSATION = "CONVERSATION";
    
    // Report Reasons
    public static final String REPORT_REASON_SCAM = "SCAM";
    public static final String REPORT_REASON_INAPPROPRIATE = "INAPPROPRIATE_CONTENT";
    public static final String REPORT_REASON_SPAM = "SPAM";
    public static final String REPORT_REASON_HARASSMENT = "HARASSMENT";
    public static final String REPORT_REASON_FAKE_LISTING = "FAKE_LISTING";
    
    // Report Status
    public static final String REPORT_STATUS_PENDING = "PENDING";
    public static final String REPORT_STATUS_REVIEWED = "REVIEWED";
    public static final String REPORT_STATUS_RESOLVED = "RESOLVED";
    public static final String REPORT_STATUS_DISMISSED = "DISMISSED";
    
    // User Types for Rating
    public static final String USER_TYPE_BUYER = "BUYER";
    public static final String USER_TYPE_SELLER = "SELLER";
    
    // Product Categories
    public static final String[] PRODUCT_CATEGORIES = {
        "Tất cả", "Điện tử", "Thời trang", "Nhà cửa & Đời sống", 
        "Xe cộ", "Thể thao & Du lịch", "Sách & Văn phòng phẩm", 
        "Mẹ & Bé", "Khác"
    };
    
    // Product Conditions
    public static final String[] PRODUCT_CONDITIONS = {
        "Tất cả", "Mới", "Như mới", "Tốt", "Khá tốt", "Cũ"
    };
    
    // Sort Options
    public static final String SORT_BY_DATE = "date";
    public static final String SORT_BY_PRICE_ASC = "price_asc";
    public static final String SORT_BY_PRICE_DESC = "price_desc";
    public static final String SORT_BY_POPULARITY = "popularity";
    
    // Rating Constants
    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;
    
    // Validation Constants
    public static final int MIN_PRODUCT_TITLE_LENGTH = 3;
    public static final int MAX_PRODUCT_TITLE_LENGTH = 100;
    public static final int MIN_PRODUCT_DESCRIPTION_LENGTH = 10;
    public static final int MAX_PRODUCT_DESCRIPTION_LENGTH = 1000;
    public static final double MIN_PRODUCT_PRICE = 0.01;
    public static final double MAX_PRODUCT_PRICE = 999999999.99;

    // Image Constants
    public static final int MAX_IMAGES_PER_PRODUCT = 5;
    public static final int IMAGE_QUALITY = 80; // 0-100
    public static final int MAX_IMAGE_SIZE_MB = 5;

    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 50;

    // Timeouts (in milliseconds)
    public static final long NETWORK_TIMEOUT = 30000; // 30 seconds
    public static final long IMAGE_UPLOAD_TIMEOUT = 60000; // 60 seconds

    // Cache Duration (in milliseconds)
    public static final long PRODUCT_CACHE_DURATION = 300000; // 5 minutes
    public static final long USER_CACHE_DURATION = 600000; // 10 minutes

    // Notification Types
    public static final String NOTIFICATION_TYPE_NEW_MESSAGE = "new_message";
    public static final String NOTIFICATION_TYPE_NEW_OFFER = "new_offer";
    public static final String NOTIFICATION_TYPE_OFFER_ACCEPTED = "offer_accepted";
    public static final String NOTIFICATION_TYPE_OFFER_REJECTED = "offer_rejected";
    public static final String NOTIFICATION_TYPE_TRANSACTION_COMPLETED = "transaction_completed";

    // Intent Extras
    public static final String EXTRA_PRODUCT_ID = "product_id";
    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_OFFER_ID = "offer_id";
    public static final String EXTRA_TRANSACTION_ID = "transaction_id";

    // Shared Preferences Keys
    public static final String PREFS_NAME = "TradeUpPrefs";
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_USER_NAME = "user_name";
    public static final String PREF_USER_EMAIL = "user_email";
    public static final String PREF_FIRST_LAUNCH = "first_launch";
    public static final String PREF_NOTIFICATION_ENABLED = "notifications_enabled";

    // Error Messages
    public static final String ERROR_NETWORK = "Lỗi kết nối mạng";
    public static final String ERROR_AUTHENTICATION = "Lỗi xác thực người dùng";
    public static final String ERROR_PERMISSION_DENIED = "Không có quyền truy cập";
    public static final String ERROR_FILE_UPLOAD = "Lỗi tải lên hình ảnh";
    public static final String ERROR_DATA_NOT_FOUND = "Không tìm thấy dữ liệu";
    public static final String ERROR_INVALID_INPUT = "Dữ liệu nhập không hợp lệ";

    // Success Messages
    public static final String SUCCESS_PRODUCT_ADDED = "Đã thêm sản phẩm thành công";
    public static final String SUCCESS_OFFER_SENT = "Đã gửi đề nghị thành công";
    public static final String SUCCESS_TRANSACTION_COMPLETED = "Giao dịch hoàn thành";
    public static final String SUCCESS_RATING_SUBMITTED = "Đã gửi đánh giá thành công";

    // Private constructor to prevent instantiation
    private Constants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}
