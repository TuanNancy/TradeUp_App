plugins {
    id("com.android.application")
    // Thêm plugin google-services
    id("com.google.gms.google-services")
    // Đảm bảo bạn có plugin kotlin.android nếu đây là project Kotlin
    // alias(libs.plugins.jetbrains.kotlin.android) // Bỏ comment nếu cần
}

android {
    namespace = "com.example.tradeup_app" // Đổi namespace
    compileSdk = 35          // Đổi compileSdk

    defaultConfig {
        applicationId = "com.example.tradeup_app" // Đổi applicationId
        minSdk = 24                 // Đổi minSdk
        targetSdk = 35              // Đổi targetSdk
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        // Giữ nguyên hoặc điều chỉnh Java version nếu cần, ví dụ thành JavaVersion.VERSION_1_8
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // Nếu bạn dùng Kotlin, bạn có thể có kotlinOptions ở đây
    // kotlinOptions {
    //    jvmTarget = "1.8" // Hoặc "11" tùy theo sourceCompatibility
    // }

    // Bật View Binding
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Giữ lại các dependencies hiện có và thêm những cái mới
    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))
    implementation(libs.appcompat) // Phiên bản này sẽ được lấy từ libs.versions.toml
    implementation(libs.material)  // Phiên bản này sẽ được lấy từ libs.versions.toml
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.google.firebase:firebase-analytics")
    // Firebase BoM (Bill of Materials) - Quản lý phiên bản các thư viện Firebase
    // Hãy kiểm tra phiên bản BoM mới nhất và cập nhật nếu cần

    // Firebase Authentication (không cần chỉ định phiên bản khi dùng BoM)
    implementation("com.google.firebase:firebase-auth")

    // Firebase Firestore (không cần chỉ định phiên bản khi dùng BoM)
    implementation("com.google.firebase:firebase-firestore")


    // Firebase Cloud Messaging for push notifications
    implementation("com.google.firebase:firebase-messaging")

    // Add Firebase Storage for image uploads in messaging
    implementation("com.google.firebase:firebase-storage")

    // Google Play Services Auth (kiểm tra phiên bản mới nhất tương thích)
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Google Play Services Maps and Location
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)


    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("com.evernote:android-job:1.4.3")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation ("com.google.firebase:firebase-auth:22.3.0")
    implementation ("com.google.android.gms:play-services-auth:21.1.0")
    implementation ("com.cloudinary:cloudinary-android:2.2.0")
    implementation ("com.google.code.gson:gson:2.10.1")
    // CircleImageView for profile pictures
    implementation ("de.hdodenhof:circleimageview:3.1.0")

    // ViewPager2 for image sliding
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // PhotoView from JitPack
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    // uCrop from JitPack
    implementation("com.github.Yalantis:uCrop:2.2.8-native")

    // Image Compressor
    implementation("id.zelory:compressor:3.0.1")

    // Firebase Realtime Database
    implementation("com.google.firebase:firebase-database-ktx")

    // Image Loading and Processing (chỉ dùng Cloudinary, không dùng Firebase Storage)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // UI Components
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // Add SwipeRefreshLayout for conversations activity
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Stripe Android SDK for payment processing
    implementation("com.stripe:stripe-android:20.37.3")
}