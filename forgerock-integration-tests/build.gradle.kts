/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinSerialization)
}

apply<AndroidBuildGradlePlugin>()

android {

    namespace = "org.forgerock.android.integration"
    testNamespace = "org.forgerock.android.integration.test"
}

dependencies {
    api(project(":forgerock-authenticator"))
    api(project(":forgerock-auth"))
    api(project(":ping-protect"))

    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.mockwebserver)
    androidTestImplementation(libs.commons.io)
    androidTestImplementation(libs.rules)
    androidTestImplementation(libs.play.services.location)
    //Do not update to the latest library, Only 2.x compatible with Android M and below.
    androidTestImplementation(libs.assertj.core)
    androidTestImplementation(libs.play.services.fido)

    androidTestImplementation(libs.androidx.biometric.ktx)
    androidTestImplementation(libs.nimbus.jose.jwt)
    androidTestImplementation(libs.okhttp)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    //For Application Pin
    androidTestImplementation(libs.bcpkix.jdk15on)
    androidTestImplementation(libs.androidx.security.crypto)

    //App Integrity
    androidTestImplementation(libs.integrity)

    // Captcha
    androidTestImplementation(libs.play.services.safetynet)
    androidTestImplementation(libs.recaptchaEnterprise)
}