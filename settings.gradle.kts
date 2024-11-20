/*
 * Copyright (c) 2019 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

include(":forgerock-auth")
include(":forgerock-auth-ui")
include(":forgerock-core")
include(":forgerock-authenticator")
include(":ping-protect")
include(":forgerock-integration-tests")

include(":app")
project(":app").projectDir = File("e2e/app")

