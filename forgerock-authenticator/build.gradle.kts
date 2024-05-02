/*
 * Copyright (c) 2020 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

plugins {
    id("com.android.library")
    id("com.adarshr.test-logger")
    id("maven-publish")
    id("signing")
    id("kotlin-android")
// We cannot use kdoc for this project due to Lombak ,so need to add this dokka plugin here.
    id("org.jetbrains.dokka")
}

apply<AndroidBuildGradlePlugin>()

android {
    namespace = "org.forgerock.android.authenticator"
}

tasks.dokkaHtml.configure {
    outputDirectory.set(file("$buildDir/html"))
}

tasks.dokkaJavadoc.configure {
    outputDirectory.set(file("$buildDir/javadoc"))
}

/**
 * JCenter Dependency Manager
 */
tasks {
    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(android.sourceSets.getByName("main").java.srcDirs)
    }

    val javadocJar by creating(Jar::class) {
        dependsOn.add(dokkaHtml)
        archiveClassifier.set("javadoc")
        from(File("$buildDir/generated-javadoc"))
    }

    artifacts {
        archives(sourcesJar)
        archives(javadocJar)
    }
}

apply("../config/jacoco.gradle")
apply("../config/logger.gradle")
apply("../config/publish.gradle")


/**
 * Dependencies
 */
dependencies {
    api(project(":forgerock-core"))

    // JWT
    implementation(libs.nimbus.jose.jwt)

    // Common
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)

    // FCM Notifications, make it optional for developer
    compileOnly(libs.firebase.messaging)

    // Networking
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Biometric
    implementation(libs.androidx.biometric.ktx)


    // Testing
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.test.runner)
    testImplementation(libs.junit)
    testImplementation(libs.org.robolectric.robolectric)
    testImplementation(libs.okhttp3.mockwebserver)
    testImplementation(libs.firebase.messaging)
    testImplementation(libs.mockito.core)

    androidTestImplementation(libs.androidx.test.ext.junit)
    
}

