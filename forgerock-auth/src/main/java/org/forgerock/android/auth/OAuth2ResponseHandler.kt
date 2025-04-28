/*
 * Copyright (c) 2019 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.net.Uri
import okhttp3.Response
import org.forgerock.android.auth.AccessToken.Scope.Companion.parse
import org.forgerock.android.auth.Logger.Companion.debug
import org.forgerock.android.auth.exception.ApiException
import org.json.JSONObject

/**
 * Implementation for handling [OAuth2Client] response, and provide feedback to the registered [FRListener]
 */
class OAuth2ResponseHandler : ResponseHandler {
    /**
     * Handle Authorization response.
     *
     * @param response The response from /authorize endpoint
     * @param state The state parameter included with the /authorize endpoint
     * @param listener Listener for receiving OAuth APIs related changes
     */
    fun handleAuthorizeResponse(response: Response,
                                state: String,
                                listener: FRListener<String>) {
        try {
            if (response.isRedirect) {
                val location = response.header("Location")
                val redirect = Uri.parse(location)
                val code = redirect.getQueryParameter("code")
                val responseState = redirect.getQueryParameter("state")
                if (responseState == null || responseState != state) {
                    Listener.onException(listener, IllegalStateException("OAuth2 state mismatch"))
                    return
                }
                if (code != null) {
                    listener.onSuccess(code)
                } else {
                    val errorDescription = redirect.getQueryParameter("error_description")
                    listener.onException(ApiException(
                        response.code, response.message, errorDescription))
                }
            } else {
                handleError(response, listener)
            }
        } finally {
            close(response)
        }
    }

    /**
     * Handle Token response
     *
     * @param response The response from /token endpoint
     */
    fun handleTokenResponse(sessionToken: SSOToken?,
                            response: Response,
                            origRefreshToken: String?,
                            listener: FRListener<AccessToken>) {
        if (response.isSuccessful) {
            try {
                response.body?.let { responseBody ->
                    val jsonObject = JSONObject(responseBody.string())
                    debug(TAG, "Access Token Received")
                    listener.onSuccess(AccessToken(
                        value = jsonObject.getString(OAuth2.ACCESS_TOKEN),
                        tokenType = jsonObject.optString(OAuth2.TOKEN_TYPE).takeIf { it.isNotEmpty() },
                        scope = parse(jsonObject.optString(OAuth2.SCOPE).takeIf { it.isNotEmpty() } ?: ""),
                        expiresIn = jsonObject.optLong(OAuth2.EXPIRES_IN, 0),
                        refreshToken = jsonObject.optString(OAuth2.REFRESH_TOKEN).takeIf { it.isNotEmpty() } ?: origRefreshToken,
                        idToken = jsonObject.optString(OAuth2.ID_TOKEN).takeIf { it.isNotEmpty() },
                        sessionToken = sessionToken))
                }
           } catch (e: Exception) {
                debug(TAG, "Fail parsing returned Access Token: %s", e.message)
                listener.onException(e)
            }
        } else {
            debug(TAG, "Exchange Access Token with Authorization Code failed.")
            handleError(response, listener)
        }
    }

    /**
     * Handle revoke token response
     *
     * @param response The response from the API
     * @param listener The Listener to listen for events
     */
    fun handleRevokeResponse(response: Response, listener: FRListener<Void?>) {
        if (response.isSuccessful) {
            debug(TAG, "Revoke success")
            listener.onSuccess(null)
            close(response)
        } else {
            debug(TAG, "Revoke failed")
            handleError(response, listener)
        }
    }

    companion object {
        private val TAG: String = OAuth2ResponseHandler::class.java.simpleName
    }
}

