plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// ── Release signing: CI (বা লোকাল) এনভায়রনমেন্ট ভ্যারিয়েবল থেকে আসে ──
// KEYSTORE_PATH না থাকলে release বিল্ড debug key দিয়ে সাইন হবে (ইনস্টল করা যাবে, কিন্তু প্রোডাকশনের জন্য নয়)
val keystorePath: String? = System.getenv("KEYSTORE_PATH")?.takeIf { it.isNotBlank() }

android {
    namespace = "com.example.timerblock"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.timerblock"
        minSdk = 26
        targetSdk = 34
        // CI থেকে -PversionCode / -PversionName দিয়ে ওভাররাইড হয়
        versionCode = (project.findProperty("versionCode") as? String)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("versionName") as? String) ?: "1.0"
    }

    signingConfigs {
        keystorePath?.let { path ->
            create("release") {
                storeFile = file(path)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (keystorePath != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    lint {
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
