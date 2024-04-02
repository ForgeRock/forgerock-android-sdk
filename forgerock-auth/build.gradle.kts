/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

plugins {

    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)


    id("com.adarshr.test-logger")
    id("maven-publish")
    id("signing")
    id("kotlin-parcelize")
}

apply<AndroidBuildGradlePlugin>()

android {
    namespace = "org.forgerock.android.auth"
    testNamespace = "org.forgerock.android.auth.androidTest"

    buildFeatures {
        //Do we really need this?
        viewBinding = true
    }

    kotlinOptions {
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }

    unitTestVariants.all {
        this.mergedFlavor.manifestPlaceholders["appAuthRedirectScheme"] = "org.forgerock.demo"
    }

}

apply("../config/logger.gradle")
apply("../config/kdoc.gradle")
apply("../config/publish.gradle")
/**
 * Dependencies
 */

val delombok by configurations.creating {
    extendsFrom(configurations.compileOnly.get())
}

dependencies {
    api(project(":forgerock-core"))

    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.org.jetbrains.kotlinx)
    implementation(libs.jetbrains.kotlinx.coroutines.play.services)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment.ktx)

    //Make it optional for developer
    compileOnly(libs.play.services.location)
    compileOnly(libs.play.services.safetynet)
    // Keeping this version for now, its breaking Apple SignIn for the later versions.
    compileOnly(libs.appauth)
    compileOnly(libs.play.services.fido)

    //For Device Binding
    compileOnly(libs.androidx.biometric.ktx)
    compileOnly(libs.nimbus.jose.jwt)

    //Application Pin
    compileOnly(libs.bcpkix.jdk15on)

    //Social Login
    compileOnly(libs.play.services.auth)
    compileOnly(libs.facebook.login)

    //For App integrity
    compileOnly(libs.integrity)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.mockwebserver)
    androidTestImplementation(libs.commons.io)
    androidTestImplementation(libs.rules)
    androidTestImplementation(libs.play.services.location)
    //Do not update to the latest library, Only 2.x compatible with Android M and below.
    androidTestImplementation(libs.assertj.core)
    androidTestImplementation(libs.play.services.fido)

    androidTestImplementation(libs.androidx.biometric.ktx)
    androidTestImplementation(libs.nimbus.jose.jwt)
    //For Application Pin
    androidTestImplementation(libs.bcpkix.jdk15on)
    androidTestImplementation(libs.androidx.security.crypto)

    //App Integrity
    androidTestImplementation(libs.integrity)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.test.runner)
    testImplementation(libs.androidx.fragment.testing)
    testImplementation(libs.nimbus.jose.jwt)

    testImplementation(libs.junit)
    testImplementation(libs.org.robolectric.robolectric)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.commons.io)
    testImplementation(libs.assertj.core)
    testImplementation(libs.androidx.espresso.intents)
    testImplementation(libs.appauth)
    testImplementation(libs.play.services.fido)
    testImplementation(libs.play.services.auth)
    testImplementation(libs.facebook.login)
    testImplementation(libs.play.services.safetynet)
    testImplementation(libs.easy.random.core)
    testImplementation(libs.nimbus.jose.jwt)
    testImplementation(libs.androidx.biometric.ktx)

    //Application Pin
    testImplementation(libs.bcpkix.jdk15on)
    testImplementation(libs.androidx.security.crypto)

    //App Integrity
    testImplementation(libs.integrity)

    testImplementation(libs.kotlinx.coroutines.test)

    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.powermock.module.junit4)
    testImplementation(libs.powermock.api.mockito2)

    compileOnly(libs.projectlombok.lombok)
    delombok(libs.projectlombok.lombok)
    annotationProcessor(libs.projectlombok.lombok)

}