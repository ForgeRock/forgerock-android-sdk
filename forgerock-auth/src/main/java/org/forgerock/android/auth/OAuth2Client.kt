/*
 * Copyright (c) 2019 - 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.net.Uri
import android.util.Base64
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.forgerock.android.auth.Logger.Companion.debug
import org.forgerock.android.auth.exception.AuthorizeException
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom

/**
 * Class to handle OAuth2 related endpoint
 */
class OAuth2Client(
    /**
     * The registered client identifier
     */
    val clientId: String,
    val scope: String,
    val redirectUri: String,
    val signOutRedirectUri: String,
    val serverConfig: ServerConfig) {

    val responseType: String = OAuth2.CODE

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClientProvider.lookup(serverConfig)
    }

    /**
     * Sends an authorization request to the authorization service.
     *
     * @param token                The SSO Token received with the result of [AuthService]
     * @param additionalParameters Additional parameters for inclusion in the authorization endpoint
     * request
     * @param listener             Listener that listens to changes resulting from OAuth endpoints .
     */
    fun exchangeToken(token: SSOToken,
                      additionalParameters: Map<String, String>,
                      listener: FRListener<AccessToken?>) {
        debug(TAG, "Exchanging Access Token with SSO Token.")
        val handler = OAuth2ResponseHandler()
        try {
            val builder = FormBody.Builder()

            builder.add(OAuth2.SCOPE, scope)

            val pkce = generateCodeChallenge()
            val state = generateState()

            debug(TAG, "Exchanging Authorization Code with SSO Token.")

            val request = Request.Builder()
                .url(getAuthorizeUrl(token, pkce, state, additionalParameters))
                .get()
                .header(ServerConfig.ACCEPT_API_VERSION, ServerConfig.API_VERSION_2_1)
                .header(serverConfig.cookieName, token.value)
                .tag(AUTHORIZE)
                .build()

            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    debug(TAG, "Failed to exchange for Authorization Code: %s", e.message)
                    listener.onException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    handler.handleAuthorizeResponse(response, state, object : FRListener<String> {
                        override fun onException(e: Exception) {
                            debug(TAG, "Failed to exchange for Authorization Code: %s", e.message)
                            listener.onException(AuthorizeException("Failed to exchange authorization code with sso token",
                                e))
                        }

                        override fun onSuccess(result: String) {
                            debug(TAG, "Authorization Code received.")
                            token(token, result, pkce, additionalParameters, handler, listener)
                        }
                    })
                }
            })
        } catch (e: IOException) {
            listener.onException(e)
        }
    }

    /**
     * Refresh the Access Token with the provided Refresh Token
     *
     * @param sessionToken The Session Token that bind to existing AccessToken
     * @param refreshToken The Refresh Token that use to refresh the Access Token
     * @param listener     Listen for endpoint event
     */
    fun refresh(sessionToken: SSOToken?, refreshToken: String, listener: FRListener<AccessToken?>) {
        debug(TAG, "Refreshing Access Token")

        val handler = OAuth2ResponseHandler()
        try {
            val builder = FormBody.Builder()

            builder.add(OAuth2.SCOPE, scope)

            val body: RequestBody = builder.add(OAuth2.CLIENT_ID, clientId)
                .add(OAuth2.GRANT_TYPE, OAuth2.REFRESH_TOKEN)
                .add(OAuth2.RESPONSE_TYPE, responseType)
                .add(OAuth2.REFRESH_TOKEN, refreshToken)
                .build()

            val request: Request = Request.Builder()
                .url(tokenUrl)
                .post(body)
                .header(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
                .header(ServerConfig.ACCEPT_API_VERSION, ServerConfig.API_VERSION_2_1)
                .tag(REFRESH_TOKEN)
                .build()


            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    listener.onException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    handler.handleTokenResponse(sessionToken, response, refreshToken, listener)
                }
            })
        } catch (e: IOException) {
            listener.onException(e)
        }
    }

    /**
     * Revoke the AccessToken, to revoke the access token, first look for refresh token to revoke, if
     * not provided, will revoke with the access token.
     *
     * @param accessToken The AccessToken to be revoked
     * @param listener    Listener to listen for revoke event
     */
    fun revoke(accessToken: AccessToken, listener: FRListener<Void?>?) {
        revoke(accessToken, true, listener)
    }

    /**
     * Revoke the AccessToken, to revoke the access token, first look for refresh token to revoke, if
     * not provided or useRefreshToken = false, will revoke with the access token.
     *
     * @param accessToken     The AccessToken to be revoked
     * @param useRefreshToken If true, revoke with refresh token, otherwise revoke access token
     * @param listener        Listener to listen for revoke event
     */
    fun revoke(accessToken: AccessToken, useRefreshToken: Boolean, listener: FRListener<Void?>?) {
        debug(TAG, "Revoking Access Token & Refresh Token")
        val handler = OAuth2ResponseHandler()
        try {
            val builder = FormBody.Builder()

            val token = if (accessToken.refreshToken == null || !useRefreshToken
            ) accessToken.value else accessToken.refreshToken

            val body: RequestBody = builder
                .add(OAuth2.CLIENT_ID, clientId)
                .add(OAuth2.TOKEN, token)
                .build()

            val request: Request = Request.Builder()
                .url(revokeUrl)
                .post(body)
                .header(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
                .header(ServerConfig.ACCEPT_API_VERSION, ServerConfig.API_VERSION_2_1)
                .tag(REVOKE_TOKEN)
                .build()


            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Listener.onException(listener, e)
                }

                override fun onResponse(call: Call, response: Response) {
                    handler.handleRevokeResponse(response, listener)
                }
            })
        } catch (e: IOException) {
            Listener.onException(listener, e)
        }
    }

    /**
     * End the user session with end session endpoint.
     *
     * @param idToken  The ID_TOKEN which associated with the user session.
     * @param listener Listener to listen for end session event.
     */
    fun endSession(idToken: String, listener: FRListener<Void?>?) {
        val request: Request?
        try {
            request = Request.Builder()
                .url(getEndSessionUrl(clientId, idToken))
                .get()
                .tag(END_SESSION)
                .build()
        } catch (e: MalformedURLException) {
            Listener.onException(listener, e)
            return
        }

        val handler = OAuth2ResponseHandler()
        debug(TAG, "End session with id token")
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                debug(TAG, "Revoke session with id token failed: %s", e.message)
                Listener.onException(listener, e)
            }

            override fun onResponse(call: Call, response: Response) {
                handler.handleRevokeResponse(response, listener)
            }
        })
    }

    /**
     * Sends an token request to the authorization service.
     *
     * @param sessionToken         The Session Token
     * @param code                 The Authorization code.
     * @param pkce                 The Proof Key for Code Exchange
     * @param additionalParameters Additional parameters for inclusion in the token endpoint
     * request
     * @param handler              Handle changes resulting from OAuth endpoints.
     */
    fun token(sessionToken: SSOToken?,
              code: String,
              pkce: PKCE,
              additionalParameters: Map<String, String>,
              handler: OAuth2ResponseHandler,
              listener: FRListener<AccessToken?>) {
        debug(TAG, "Exchange Access Token with Authorization Code")
        try {
            val builder = FormBody.Builder()

            for ((key, value) in additionalParameters) {
                builder.add(key, value)
            }

            val body: RequestBody = builder
                .add(OAuth2.CLIENT_ID, clientId)
                .add(OAuth2.CODE, code)
                .add(OAuth2.REDIRECT_URI, redirectUri)
                .add(OAuth2.GRANT_TYPE, OAuth2.AUTHORIZATION_CODE)
                .add(OAuth2.CODE_VERIFIER, pkce.codeVerifier)
                .build()

            val request: Request = Request.Builder()
                .url(tokenUrl)
                .post(body)
                .header(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
                .header(ServerConfig.ACCEPT_API_VERSION, ServerConfig.API_VERSION_2_1)
                .tag(EXCHANGE_TOKEN)
                .build()

            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    debug(TAG,
                        "Exchange Access Token with Authorization Code failed: %s",
                        e.message)
                    listener.onException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    handler.handleTokenResponse(sessionToken, response, null, listener)
                }
            })
        } catch (e: IOException) {
            listener.onException(e)
        }
    }

    @Throws(MalformedURLException::class, UnsupportedEncodingException::class)
    private fun getAuthorizeUrl(token: Token,
                                pkce: PKCE,
                                state: String,
                                additionalParameters: Map<String, String>): URL {
        val builder = Uri.parse(authorizeUrl.toString()).buildUpon()
        for ((key, value) in additionalParameters) {
            builder.appendQueryParameter(key, value)
        }
        return URL(builder
            .appendQueryParameter(OAuth2.CLIENT_ID, clientId)
            .appendQueryParameter(OAuth2.SCOPE, scope)
            .appendQueryParameter(OAuth2.RESPONSE_TYPE, responseType)
            .appendQueryParameter(OAuth2.REDIRECT_URI, redirectUri)
            .appendQueryParameter(OAuth2.CODE_CHALLENGE, pkce.codeChallenge)
            .appendQueryParameter(OAuth2.CODE_CHALLENGE_METHOD, pkce.codeChallengeMethod)
            .appendQueryParameter(OAuth2.STATE, state)
            .build().toString())
    }

    @get:Throws(MalformedURLException::class)
    val authorizeUrl: URL
        get() {
            val builder = Uri.parse(serverConfig.url).buildUpon()
            if (StringUtils.isNotEmpty(serverConfig.authorizeEndpoint)) {
                builder.appendEncodedPath(serverConfig.authorizeEndpoint)
            } else {
                builder.appendPath("oauth2")
                    .appendPath("realms")
                    .appendPath(serverConfig.realm)
                    .appendPath("authorize")
            }
            return URL(builder.build().toString())
        }

    @get:Throws(MalformedURLException::class)
    val tokenUrl: URL
        get() {
            val builder = Uri.parse(serverConfig.url).buildUpon()
            if (StringUtils.isNotEmpty(serverConfig.tokenEndpoint)) {
                builder.appendEncodedPath(serverConfig.tokenEndpoint)
            } else {
                builder.appendPath("oauth2")
                    .appendPath("realms")
                    .appendPath(serverConfig.realm)
                    .appendPath("access_token")
            }
            return URL(builder.build().toString())
        }

    @get:Throws(MalformedURLException::class)
    val revokeUrl: URL
        get() {
            val builder = Uri.parse(serverConfig.url).buildUpon()
            if (StringUtils.isNotEmpty(serverConfig.revokeEndpoint)) {
                builder.appendEncodedPath(serverConfig.revokeEndpoint)
            } else {
                builder.appendPath("oauth2")
                    .appendPath("realms")
                    .appendPath(serverConfig.realm)
                    .appendPath("token")
                    .appendPath("revoke")
            }
            return URL(builder.build().toString())
        }

    @get:Throws(MalformedURLException::class)
    val endSessionUrl: URL
        get() {
            val builder = Uri.parse(serverConfig.url).buildUpon()
            if (StringUtils.isNotEmpty(serverConfig.endSessionEndpoint)) {
                builder.appendEncodedPath(serverConfig.endSessionEndpoint)
            } else {
                builder.appendPath("oauth2")
                    .appendPath("realms")
                    .appendPath(serverConfig.realm)
                    .appendPath("connect")
                    .appendPath("endSession")
            }
            return URL(builder.build().toString())
        }


    @Throws(MalformedURLException::class)
    fun getEndSessionUrl(clientId: String?, idToken: String?): URL {
        val builder = Uri.parse(endSessionUrl.toString()).buildUpon()
        builder.appendQueryParameter("id_token_hint", idToken)
        builder.appendQueryParameter("client_id", clientId)
        return URL(builder.build().toString())
    }


    @Throws(UnsupportedEncodingException::class)
    private fun generateCodeChallenge(): PKCE {
        val encodeFlags = Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE
        val randomBytes = ByteArray(64)
        SecureRandom().nextBytes(randomBytes)
        val codeVerifier = Base64.encodeToString(randomBytes, encodeFlags)
        try {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            messageDigest.update(codeVerifier.toByteArray(StandardCharsets.ISO_8859_1))
            val digestBytes = messageDigest.digest()
            return PKCE(Base64.encodeToString(digestBytes, encodeFlags), "S256", codeVerifier)
        } catch (e: NoSuchAlgorithmException) {
            return PKCE("plain", codeVerifier, codeVerifier)
        }
    }

    class OAuth2ClientBuilder internal constructor() {
        private var clientId: String = ""
        private var scope: String = ""
        private var redirectUri: String = ""
        private var signOutRedirectUri: String = ""
        private var serverConfig: ServerConfig? = null
        fun clientId(clientId: String): OAuth2ClientBuilder {
            this.clientId = clientId
            return this
        }

        fun scope(scope: String): OAuth2ClientBuilder {
            this.scope = scope
            return this
        }

        fun redirectUri(redirectUri: String): OAuth2ClientBuilder {
            this.redirectUri = redirectUri
            return this
        }

        fun signOutRedirectUri(signOutRedirectUri: String): OAuth2ClientBuilder {
            this.signOutRedirectUri = signOutRedirectUri
            return this
        }

        fun serverConfig(serverConfig: ServerConfig): OAuth2ClientBuilder {
            this.serverConfig = serverConfig
            return this
        }

        fun build(): OAuth2Client {
            return OAuth2Client(clientId,
                scope,
                redirectUri,
                signOutRedirectUri,
                serverConfig!!)
        }

        override fun toString(): String {
            return "OAuth2Client.OAuth2ClientBuilder(clientId=" + this.clientId + ", scope=" + this.scope + ", redirectUri=" + this.redirectUri + ", signOutRedirectUri=" + this.signOutRedirectUri + ", serverConfig=" + this.serverConfig + ")"
        }
    }

    companion object {
        private const val TAG = "OAuth2Client"
        private const val CONTENT_TYPE = "Content-Type"
        private const val APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded"
        private val AUTHORIZE = Action(Action.AUTHORIZE)
        private val EXCHANGE_TOKEN = Action(Action.EXCHANGE_TOKEN)
        private val REFRESH_TOKEN = Action(Action.REFRESH_TOKEN)
        private val REVOKE_TOKEN = Action(Action.REVOKE_TOKEN)
        private val END_SESSION = Action(Action.END_SESSION)
        private const val STATE_LENGTH = 16

        @JvmStatic
        fun builder(): OAuth2ClientBuilder {
            return OAuth2ClientBuilder()
        }

        fun generateState(): String {
            val secureRandom = SecureRandom()
            val random = ByteArray(STATE_LENGTH)
            secureRandom.nextBytes(random)
            return Base64.encodeToString(random,
                Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE)
        }
    }
}
