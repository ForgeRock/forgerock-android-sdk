/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
}
apply<AndroidBuildGradlePlugin>()

android {
    namespace = "org.forgerock.android.protect"

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":forgerock-auth"))
    implementation(libs.com.pingidentity.signals)
    implementation(libs.org.jetbrains.kotlinx)

    testImplementation(libs.org.robolectric.robolectric)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.runner)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.espresso.core)

    // Mockk
    testImplementation(libs.io.mockk)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
