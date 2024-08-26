/*
 * Copyright (c) 2019 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import okhttp3.Response
import org.forgerock.android.auth.Logger.Companion.debug
import org.forgerock.android.auth.exception.ApiException
import org.forgerock.android.auth.exception.AuthenticationException
import org.forgerock.android.auth.exception.AuthenticationTimeoutException
import org.forgerock.android.auth.exception.SuspendedAuthSessionException
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection

/**
 * Implementation for handling [AuthService] response, and provide feedback to the registered [NodeListener]
 */
internal class AuthServiceResponseHandler(private val authService: AuthService,
                                          listener: NodeListener<SSOToken?>) : ResponseHandler {
    private val listener: NodeListener<SSOToken?>? = listener

    /**
     * Handle [AuthService] APIs response and trigger registered [NodeListener]
     *
     * @param response The response from [AuthService]
     */
    fun handleResponse(response: Response) {
        try {
            if (response.isSuccessful) {
                //Proceed to next Node in the tree
                response.body?.let {
                    val jsonObject = JSONObject(it.string())
                    if (jsonObject.has(Node.AUTH_ID)) {
                        debug(TAG, "Journey callback(s) received.")
                        if (listener != null) {
                            val node =
                                listener.onCallbackReceived(authService.authServiceId, jsonObject)
                            listener.onCallbackReceived(node)
                        }
                    } else {
                        //The Auth Tree is consider finished if auth id not from the response
                        authService.done()
                        debug(TAG, "Journey finished with Success outcome.")
                        if (jsonObject.has(TOKEN_ID)) {
                            debug(TAG, "SSO Token received.")
                            Listener.onSuccess(listener, SSOToken(jsonObject.getString(TOKEN_ID),
                                jsonObject.optString("successUrl"),
                                jsonObject.optString("realm")))
                        } else {
                            Listener.onSuccess(listener, null)
                        }
                    }
                } ?: throw IllegalStateException("Response body is null")
            } else {
                handleError(response, listener)
            }
        } catch (e: Exception) {
            handleError(e)
        }
    }

    override fun handleError(response: Response, listener: FRListener<*>?) {
        when (response.code) {
            HttpURLConnection.HTTP_UNAUTHORIZED -> {
                val body = getBody(response)
                var responseBody: JSONObject? = null
                try {
                    responseBody = JSONObject(body)
                } catch (e: JSONException) {
                    //should not happened
                    handleError(AuthenticationException(response.code, response.message, body))
                    return
                }
                when (getError(responseBody)) {
                    "110" -> {
                        authService.done()
                        handleError(AuthenticationTimeoutException(response.code,
                            response.message,
                            body))
                        return
                    }

                    SUSPENDED_AUTH_SESSION_EXCEPTION -> {
                        authService.done()
                        handleError(SuspendedAuthSessionException(response.code,
                            response.message,
                            body))
                        return
                    }

                    else -> {
                        handleError(AuthenticationException(response.code,
                            response.message,
                            body))
                        return
                    }
                }
            }

            else -> handleError(ApiException(response.code,
                response.message,
                getBody(response)))
        }
    }


    private fun getError(body: JSONObject): String {
        val detail = body.optJSONObject("detail")
        if (detail != null) {
            return detail.optString("errorCode", "-1")
        }
        val message = body.optString("message", "")
        if (message.contains(SUSPENDED_AUTH_SESSION_EXCEPTION)) {
            return SUSPENDED_AUTH_SESSION_EXCEPTION
        }
        return "-1"
    }

    fun handleError(e: Exception) {
        debug(TAG, "Journey finished with failed result %s", e.message)
        Listener.onException(listener, e)
    }

    companion object {
        private val TAG: String = AuthServiceResponseHandler::class.java.simpleName
        private const val TOKEN_ID = "tokenId"
        const val SUSPENDED_AUTH_SESSION_EXCEPTION: String =
            "org.forgerock.openam.auth.nodes.framework.token.SuspendedAuthSessionException"
    }
}

