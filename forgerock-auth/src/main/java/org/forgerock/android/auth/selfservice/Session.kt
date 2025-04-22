/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.selfservice

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.Request
import okhttp3.internal.EMPTY_REQUEST
import org.forgerock.android.auth.FRSession
import org.forgerock.android.auth.OkHttpClientProvider
import org.forgerock.android.auth.SSOToken
import org.forgerock.android.auth.ServerConfig
import org.forgerock.android.auth.exception.ApiException
import org.forgerock.android.auth.json
import java.net.URL

/**
 * Retrieves the current SSO token.
 *
 * @return The current [SSOToken] or an empty token if no session is available.
 */
internal fun ssoToken(): SSOToken {
    return FRSession.getCurrentSession()?.sessionToken
        ?: SSOToken("")
}

/**
 * Extension function for [ServerConfig] to build the AM URL.
 *
 * @return A [Uri.Builder] for the AM URL.
 * @throws IllegalArgumentException if the URL is not set.
 */
fun ServerConfig.am(): Uri.Builder {
    url?.let {
        return Uri.parse(url).buildUpon()
    } ?: throw IllegalArgumentException("URL is not set")
}

/**
 * Retrieves the session information from the server.
 *
 * @param server The [ServerConfig] containing server details.
 * @param ssoTokenBlock A suspend function to retrieve the SSO token.
 * @return The [Session] information.
 * @throws ApiException if the session information retrieval fails.
 */
internal suspend fun session(server: ServerConfig, ssoTokenBlock: suspend () -> SSOToken): Session {
    val httpClient = OkHttpClientProvider.lookup(server)
    val uri = server.am().apply {
        appendPath("json")
        appendPath("realms")
        appendPath(server.realm)
        appendPath("sessions")
        appendQueryParameter("_action", "getSessionInfo")
    }
    val request: Request = Request.Builder()
        .url(URL(uri.toString()))
        .post(EMPTY_REQUEST)
        .header("Content-Type", "application/json")
        .header(server.cookieName, ssoTokenBlock().value)
        .header(ServerConfig.ACCEPT_API_VERSION, ServerConfig.API_VERSION_2_1)
        .build()
    return withContext(Dispatchers.IO) {
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw ApiException(response.code,
                    response.message,
                    response.body?.string() ?: "Failed to retrieve user")
            }
            val resp = response.body?.string()
            resp?.let {
                json.decodeFromString(it)
            } ?: throw ApiException(response.code, response.message, "Failed to retrieve session info")
        }
    }
}

/**
 * Data class representing a user session.
 *
 * @property username The username of the session.
 * @property universalId The universal ID of the session.
 * @property realm The realm of the session.
 * @property latestAccessTime The latest access time of the session.
 * @property maxIdleExpirationTime The maximum idle expiration time of the session.
 * @property maxSessionExpirationTime The maximum session expiration time.
 */
@Serializable
data class Session(
    val username: String,
    val universalId: String,
    val realm: String,
    val latestAccessTime: String,
    val maxIdleExpirationTime: String,
    val maxSessionExpirationTime: String)