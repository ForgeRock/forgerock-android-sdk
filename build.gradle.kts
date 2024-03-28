/*
 * Copyright (c) 2019 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTask
import java.io.FileInputStream
import java.util.Properties

val customCSSFile = "$projectDir/dokka/fr-backstage-styles.css"
val customLogoFile = "$projectDir/dokka/logo-icon.svg"
val customTemplatesFolder = file("$projectDir/dokka/templates")

buildscript {

    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("com.adarshr:gradle-test-logger-plugin:2.0.0")
        classpath("com.google.gms:google-services:4.3.15")
    }
}

plugins {
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("org.sonatype.gradle.plugins.scan") version "2.4.0"
    id("org.jetbrains.dokka") version "1.9.10"
    id("com.android.application") version "8.2.2" apply false
    id("com.android.library") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}

// Configure all single-project Dokka tasks at the same time,
// such as dokkaHtml, dokkaJavadoc and dokkaGfm.
// Configure all single-project Dokka tasks at the same time,
// such as dokkaHtml, dokkaJavadoc and dokkaGfm.
tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets.configureEach {
        sourceRoots.setFrom(file("$buildDir/src-delomboked"))
    }
}

allprojects {
    configurations.all {

        resolutionStrategy {
            // Due to vulnerability [CVE-2022-40152] from dokka project.
            force("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.5")
            force("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.13.5")
            force("com.fasterxml.jackson.core:jackson-databind:2.13.5")
            // Junit test project
            force("junit:junit:4.13.2")
            //Due to Vulnerability [CVE-2022-2390]: CWE-471 The product does not properly
            // protect an assumed-immutable element from being modified by an attacker.
            // on version < 18.0.1, this library is depended by most of the google libraries.
            // and needs to be reviewed on upgrades
            force("com.google.android.gms:play-services-basement:18.1.0")
            //Due to Vulnerability [CVE-2023-3635] CWE-681: Incorrect Conversion between Numeric Types
            //on version < 3.4.0, this library is depended by okhttp, when okhttp upgrade, this needs
            //to be reviewed
            force("com.squareup.okio:okio:3.4.0")
            //Due to this https://github.com/powermock/powermock/issues/1125, we have to keep using an
            //older version of mockito until mockito release a fix
            force("org.mockito:mockito-core:3.12.4")
            // this is for the mockwebserver
            force("org.bouncycastle:bcprov-jdk15on:1.68")
        }
    }
}

subprojects {

    apply(plugin = "org.jetbrains.dokka")

    tasks.dokkaHtml {
        val map = mutableMapOf<String, String>()
        map["org.jetbrains.dokka.base.DokkaBase"] = """{
                    "customStyleSheets": ["$customCSSFile"],
                    "templatesDir": "$customTemplatesFolder"
                }"""
        pluginsMapConfiguration.set(map)
        moduleVersion.set(project.property("VERSION") as? String)
        outputDirectory.set(file("build/html/${project.name}-dokka"))

        dokkaSourceSets.configureEach {
            documentedVisibilities.set(
                setOf(
                    Visibility.PUBLIC,
                    Visibility.PROTECTED,
                    Visibility.PRIVATE,
                    Visibility.INTERNAL,
                    Visibility.PACKAGE
                )
            )
            perPackageOption {
                matchingRegex.set(".*internal.*")
                suppress.set(true)
            }
        }
    }


    tasks.withType<Test>().configureEach {
        jvmArgs = jvmArgs?.plus("--add-opens=java.base/java.lang=ALL-UNNAMED")
        jvmArgs = jvmArgs?.plus("--add-opens=java.base/java.security=ALL-UNNAMED")
        jvmArgs = jvmArgs?.plus("--add-opens=java.base/java.security.cert=ALL-UNNAMED")
    }

}

afterEvaluate {

    tasks.dokkaHtmlMultiModule {
        moduleName.set("ForgeRock SDK for Android")
        moduleVersion.set(project.property("VERSION") as? String)
        outputDirectory.set(file("build/api-reference/html"))
        val map = mutableMapOf<String, String>()
        map["org.jetbrains.dokka.base.DokkaBase"] = """{
                    "customStyleSheets": ["$customCSSFile"],
                    "templatesDir": "$customTemplatesFolder"
                }"""
        pluginsMapConfiguration.set(map)
    }


    tasks.dokkaJavadocCollector {
        moduleName.set("ForgeRock SDK for Android Javadoc")
        moduleVersion.set(project.property("VERSION") as? String)
        outputDirectory.set(file("build/api-reference/javadoc"))
    }

}


ossIndexAudit {
    username = System.getProperty("username")
    password = System.getProperty("password")
    excludeVulnerabilityIds = setOf("CVE-2020-15250")
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}

// Need to be removed this in future
project.ext.set("versionName", project.property("VERSION") as? String ?: "")
project.ext.set("versionCode", project.property("VERSION_CODE") as? Int ?: 1)
project.ext["signing.keyId"] = ""
project.ext["signing.password"] = ""
project.ext["signing.secretKeyRingFile"] = ""
project.ext["ossrhUsername"] = ""
project.ext["ossrhPassword"] = ""

val secretPropsFile: File = project.rootProject.file("local.properties")
if (secretPropsFile.exists()) {
    val p = Properties()
    p.load(FileInputStream(secretPropsFile))
    p.forEach { name, value ->
        ext[(name as? String).toString()] = value
    }
}