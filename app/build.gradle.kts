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

    // Google Play Services Auth (kiểm tra phiên bản mới nhất tương thích)
    implementation("com.google.android.gms:play-services-auth:20.7.0")


    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}