plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.tangledline.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tangledline.game"
        minSdk = 24
        targetSdk = 36
        val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0
        val baseVersionCode = 7
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: if (runNumber > 0) (baseVersionCode + runNumber) else baseVersionCode
        versionName = System.getenv("VERSION_NAME") ?: if (runNumber > 0) "1.6.${runNumber}" else "1.6"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                val keystoreFile = if (file(keystorePath).exists()) {
                    file(keystorePath)
                } else {
                    rootProject.file(keystorePath)
                }
                if (keystoreFile.exists()) {
                    storeFile = keystoreFile
                    storePassword = System.getenv("KEYSTORE_PASSWORD")
                    keyAlias = System.getenv("KEY_ALIAS")
                    keyPassword = System.getenv("KEY_PASSWORD")
                }
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // AdMob test IDs for debug
            buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-3940256099942544~3347511713\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
        }
        release {
            isMinifyEnabled = true
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-1811294933992844~8458523252\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-1811294933992844/6513848787\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"ca-app-pub-1811294933992844/9848355496\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-1811294933992844/2518763451\"")
            manifestPlaceholders["admobAppId"] = "ca-app-pub-1811294933992844~8458523252"
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.webkit:webkit:1.9.0")
    implementation("com.google.android.gms:play-services-ads:23.1.0")
}
