/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidBuildGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.android().apply {
            compileSdk = 36;
            defaultConfig {
                minSdk = 28
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            buildTypes {
                getByName("release") {
                    isMinifyEnabled = false
                    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),
                        ("proguard-rules.pro"))
                }
            }
            testOptions {
                targetSdk = 35
                unitTests {
                    isIncludeAndroidResources = true
                    isReturnDefaultValues = true
                }
                unitTests.all {
                    it.exclude("**/*TestSuite*")
                }
            }

            buildFeatures {
                buildConfig = true
            }

            useLibrary("android.test.base")
            useLibrary("android.test.mock")

            defaultConfig {
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }

            packaging {
                resources.excludes.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
            }

            ndkVersion = "28.1.13356709"
        }
    }

    /**
     * Extension function.
     */
    private fun Project.android(): LibraryExtension {
        return extensions.getByType(LibraryExtension::class.java)
    }


}