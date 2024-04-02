/*
 * Copyright (c) 2023 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "com.example.app"
    compileSdk = 34
    defaultConfig {
        minSdk = 23
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                ("proguard-rules.pro"),
            )
        }
    }
    signingConfigs {
        getByName("debug") {
            storeFile = file("../debug.jks")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {

    val composeBom = platform("androidx.compose:compose-bom:2022.10.00")
    implementation(composeBom)

    // SDK
    implementation(project(":forgerock-auth"))
    implementation(project(":ping-protect"))

    // implementation 'org.forgerock:forgerock-auth:4.2.0'
    // Device Binding + JWT + Application Pin
    implementation(libs.bcpkix.jdk15on) // Application Pin
    implementation(libs.androidx.security.crypto)
    implementation(libs.nimbus.jose.jwt)
    implementation(libs.androidx.biometric.ktx)

    // WebAuthn
    implementation(libs.play.services.fido)

    // Centralize Login
    implementation(libs.appauth)

    // Captcha
    implementation(libs.play.services.safetynet)

    // Social Login
    implementation(libs.play.services.auth)
    implementation(libs.facebook.login)

    // For App integrity
    implementation(libs.integrity)

    // Capture Location for Device Profile
    implementation(libs.play.services.location)

    // For IG, invoke endpoint using okHttp
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // End of SDK

    // Keep the sample application specific library out of the toml
    // Material Design 3
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Android Studio Preview support
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.activity:activity-compose:1.8.2")

    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material:material-icons-extended:1.6.2")

    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
}