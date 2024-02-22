/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

plugins {
    id("com.android.library")
    id("com.adarshr.test-logger")
    id("maven-publish")
    id("signing")
    id("kotlin-android")
}

apply<AndroidBuildGradlePlugin>()

android {
    namespace = "org.forgerock.android.core"

    /**
     * Comment this to debug instrument Test,
     * There is an issue for AS to run NDK with instrument Test
     */
    externalNativeBuild {
        ndkBuild {
            path("src/main/jni/Android.mk")
        }
    }

    val VERSION: String by project
    defaultConfig {
        buildConfigField("String", "VERSION_NAME", "\"$VERSION\"")
    }

    packaging {
        jniLibs {
            pickFirsts.add("**/*.so")
        }
    }

    kotlinOptions {
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }

}

apply("../config/logger.gradle")
apply("../config/kdoc.gradle")
apply("../config/publish.gradle")

val delombok by configurations.creating {
    extendsFrom(configurations.compileOnly.get())
}

/**
 * Dependencies
 */
dependencies {

    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.security.crypto)

    // Biometric
    implementation(libs.androidx.biometric.ktx)

    //Application Pin
    compileOnly(libs.bcpkix.jdk15on)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.runner)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.mockwebserver)
    androidTestImplementation(libs.commons.io)
    androidTestImplementation(libs.rules)
    //Do not update to the latest library, Only 2.x compatible with Android M and below.
    androidTestImplementation(libs.assertj.core)

    //For Application Pin
    androidTestImplementation(libs.bcpkix.jdk15on)
    androidTestImplementation(libs.androidx.security.crypto)

    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.junit)
    testImplementation(libs.org.robolectric.robolectric)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.commons.io)
    testImplementation(libs.assertj.core)

    testImplementation(libs.bcpkix.jdk15on)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.powermock.module.junit4)
    testImplementation(libs.powermock.api.mockito2)

    compileOnly(libs.projectlombok.lombok)
    delombok(libs.projectlombok.lombok)
    annotationProcessor(libs.projectlombok.lombok)
}