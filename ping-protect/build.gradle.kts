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
    id("org.jetbrains.dokka")
}

apply<AndroidBuildGradlePlugin>()

android {
    namespace = "org.forgerock.android.protect"
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

apply("../config/logger.gradle")
apply("../config/publish.gradle")

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