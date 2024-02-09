@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "org.forgerock.android.integration.tests"
    compileSdk = 34

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    api(project(":forgerock-auth"))
    api(project(":ping-protect"))
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.assertj.core)
    //For Application Pin
    androidTestImplementation("com.madgag.spongycastle:bcpkix-jdk15on:1.58.0.0")
    androidTestImplementation("androidx.security:security-crypto:1.1.0-alpha06")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("com.squareup.okhttp:mockwebserver:2.7.5")
    androidTestImplementation("commons-io:commons-io:2.6")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("com.google.android.gms:play-services-location:21.0.1")
    //Do not update to the latest library, Only 2.x compatible with Android M and below.
    androidTestImplementation("com.google.android.gms:play-services-fido:20.0.1")
    androidTestImplementation("androidx.biometric:biometric-ktx:1.2.0-alpha05")
    androidTestImplementation("com.nimbusds:nimbus-jose-jwt:9.25")
    //For Application Pin
    androidTestImplementation("com.madgag.spongycastle:bcpkix-jdk15on:1.58.0.0")
    androidTestImplementation("androidx.security:security-crypto:1.1.0-alpha06")
    //App Integrity
    androidTestImplementation("com.google.android.play:integrity:1.3.0")
    androidTestImplementation("org.assertj:assertj-core:2.9.1")


    androidTestImplementation("com.squareup.okhttp3:okhttp:4.11.0")
    androidTestImplementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
}