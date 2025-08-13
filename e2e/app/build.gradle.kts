/*
 * Copyright (c) 2023 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.example.app"
    compileSdk = 35
    defaultConfig {
        targetSdk = 35
        minSdk = 23
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

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources.excludes.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
    }

}


kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}


dependencies {

    val composeBom = platform("androidx.compose:compose-bom:2022.10.00")
    implementation(composeBom)

    // SDK
    implementation(project(":forgerock-auth"))
    implementation(project(":ping-protect"))

    //For Custom Storage
    implementation(libs.kotlinx.serialization.json)

    //implementation("org.forgerock:forgerock-auth:4.8.1")
    //implementation("org.forgerock:ping-protect:4.8.1")

    // implementation 'org.forgerock:forgerock-auth:4.2.0'
    // Device Binding + JWT + Application Pin
    implementation(libs.bcpkix.jdk18on) // Application Pin
    implementation(libs.androidx.security.crypto)
    implementation(libs.nimbus.jose.jwt)
    implementation(libs.androidx.biometric.ktx)

    // WebAuthn
    implementation(libs.play.services.fido)

    // Centralize Login
    implementation(libs.appauth)

    // Captcha
    implementation(libs.play.services.safetynet)
    implementation(libs.recaptchaEnterprise)

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

    // Material Design 3
    implementation(libs.material)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.core.splashscreen)

    // Android Studio Preview support
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
}