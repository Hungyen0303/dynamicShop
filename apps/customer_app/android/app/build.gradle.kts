plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

// Stage 1 — Firebase Crashlytics (docs/70-stages.md). CHƯA có `google-services.json` thật cho
// flavor nào (dev/staging/prod) — xem missing_config.md mục 6. Hai plugin dưới đây BẮT BUỘC có
// google-services.json mới apply được (không thì Gradle fail build ngay ở bước config), nên chỉ
// apply khi thấy file thật — thiếu file thì build chạy y hệt trước khi thêm Firebase, không vỡ
// gì. `google-services` plugin cũng tự tìm file theo flavor (`src/<flavor>/google-services.json`)
// nên chỉ cần check có ÍT NHẤT một file ở gốc app/ hoặc trong bất kỳ thư mục flavor nào.
val hasGoogleServicesConfig =
    file("google-services.json").exists() ||
        file("src/dev/google-services.json").exists() ||
        file("src/staging/google-services.json").exists() ||
        file("src/prod/google-services.json").exists()

if (hasGoogleServicesConfig) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

android {
    namespace = "vn.dynamicshop.customer_app"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    defaultConfig {
        applicationId = "vn.dynamicshop.customer_app"
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    buildTypes {
        release {
            // TODO: Add your own signing config cho release thật khi lên Stage 1+.
            // Ký bằng debug key để `flutter run --release` chạy được ngay ở Stage 0.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    // Chỉ 3 flavor MÔI TRƯỜNG (docs/70-stages.md, docs/10-customer-app.md mục "Flavor").
    // Flavor chỉ chứa DANH TÍNH (app_name, applicationIdSuffix) — KHÔNG chứa menu/giá/theme,
    // những thứ đó luôn tải runtime. Flavor theo từng shop là Stage 2+.
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "DynamicShop Dev")
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            resValue("string", "app_name", "DynamicShop Staging")
        }
        create("prod") {
            dimension = "environment"
            resValue("string", "app_name", "DynamicShop")
        }
    }
}

flutter {
    source = "../.."
}
