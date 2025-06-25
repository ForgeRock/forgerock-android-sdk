/*
 * Copyright (c) 2025 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth

import java.io.IOException
import java.util.Properties

object TestConfig {
    private const val CONFIG_FILE_NAME = "test_config.properties"
    private val properties = Properties()

    var serverUrl: String = ""
        private set
    var realm: String = ""
        private set
    var clientId: String = ""
        private set
    var scope: String = ""
        private set
    var username: String = ""
        private set
    var password: String = ""
        private set
    var email: String = ""
        private set
    var redirectUri: String = ""
        private set
    var recaptchaSiteKey: String = ""
        private set
    var cookieName: String = ""
        private set


    init {
        try {
            val inputStream = ContextProvider.context.assets.open(CONFIG_FILE_NAME)
            properties.load(inputStream)

            serverUrl = properties.getProperty("serverUrl", "")
            realm = properties.getProperty("realm", "")
            clientId = properties.getProperty("clientId", "")
            scope = properties.getProperty("scope", "")
            username = properties.getProperty("username", "")
            password = properties.getProperty("password", "")
            email = properties.getProperty("email", "")
            redirectUri = properties.getProperty("redirectUri", "")
            recaptchaSiteKey = properties.getProperty("recaptchaSiteKey", "")
            cookieName = properties.getProperty("cookieName", "")

            inputStream.close()
        } catch (e: IOException) {
            // Handle the exception appropriately, e.g., log an error
            android.util.Log.e("AppConfig", "Error loading properties file: ${e.message}")
        }
    }
}