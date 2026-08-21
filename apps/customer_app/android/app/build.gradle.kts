plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
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
