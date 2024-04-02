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

include(":auth")
project(":auth").projectDir = File("samples/auth")

include(":authenticator")
project(":authenticator").projectDir = File("samples/authenticator")

/*
include ':pebblebank'
project(':pebblebank').projectDir = new File('demo/pebblebank')
*/

include(":quickstart")
project(":quickstart").projectDir = File("samples/quickstart")

include(":kotlin")
project(":kotlin").projectDir = File("samples/kotlin")

include(":app")
project(":app").projectDir = File("samples/app")

include(":forgerock-integration-tests")
