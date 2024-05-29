/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import org.forgerock.android.auth.ConfigHelper.Companion.load
import org.forgerock.android.auth.Logger.Companion.set
import org.forgerock.android.auth.Logger.Companion.setCustomLogger
import org.forgerock.android.auth.SecureCookieJar.Companion.builder
import java.util.UUID

/**
 * Provide SDK Configuration, most components in the SDK has its default setting, this class allow developer to
 * override the default configuration.
 */
@SuppressLint("StaticFieldLeak")
object Config {

    var context: Context? = null
        private set

    //OAuth2
    var clientId: String? = null
        private set
    var redirectUri: String? = null
        private set
    var scope: String? = null
        private set
    var oauthCacheMillis: Long? = null
        private set
    var oauthThreshold: Long? = null
        private set
    var cookieCacheMillis: Long? = null
        private set

    //Server
    private var identifier: String? = null

    @set:VisibleForTesting
    var url: String? = null
    var realm: String? = null
        private set
    var timeout: Int = 0
        private set
    var pins: List<String>? = null
        private set
    private var buildSteps: List<BuildStep<OkHttpClient.Builder>>? = null
    private var cookieJar: CookieJar? = null
    var cookieName: String? = null
        private set

    private var authenticateEndpoint: String? = null
    private var authorizeEndpoint: String? = null
    private var tokenEndpoint: String? = null
    private var revokeEndpoint: String? = null
    private var userinfoEndpoint: String? = null
    private var sessionEndpoint: String? = null
    private var endSessionEndpoint: String? = null

    //service
    var authServiceName: String? = null
        private set
    var registrationServiceName: String? = null
        private set

    //For testing to avoid using Android KeyStore Encryption
    @set:VisibleForTesting
    var ssoSharedPreferences: SharedPreferences? = null

    //For testing to avoid using Android KeyStore Encryption
    //Token Manager
    @set:VisibleForTesting
    var sharedPreferences: SharedPreferences? = null

    //KeyStoreManager
    @set:VisibleForTesting
    var keyStoreManager: KeyStoreManager? = null
        get() = if (field == null) {
            KeyStoreManager.builder().context(context).build()
        } else {
            field
        }

    //BroadcastModel
    private var ssoBroadcastModel: SSOBroadcastModel? = null

    private val mutex = Mutex()

    /**
     * Load all the SDK configuration from the strings.xml
     *
     * @param appContext The Application Context
     */
    suspend fun init(appContext: Context) = mutex.withLock {
        val option = load(appContext, null)
        init(appContext, option)
    }

    /**
     * Load all the SDK configuration from the FROptions
     *
     * @param context   The Application Context
     * @param frOptions FrOptions is nullable it loads the config from strings.xml, if not use the passed value.
     */
    suspend fun init(context: Context, frOptions: FROptions?) = mutex.withLock {
        this.context = context.applicationContext
        val options = if ((frOptions == null)) load(context, null) else frOptions
        clientId = options.oauth.oauthClientId
        redirectUri = options.oauth.oauthRedirectUri
        scope = options.oauth.oauthScope
        oauthCacheMillis = options.oauth.oauthCacheSeconds * 1000
        oauthThreshold = options.oauth.oauthThresholdSeconds
        cookieJar = null // We cannot initialize default cookie jar here
        url = options.server.url
        realm = options.server.realm
        timeout = options.server.timeout
        cookieName = options.server.cookieName
        cookieCacheMillis = options.server.cookieCacheSeconds * 1000
        registrationServiceName = options.service.registrationServiceName
        authServiceName = options.service.authServiceName
        pins = options.sslPinning.pins
        buildSteps = options.sslPinning.buildSteps

        //TODO if options.oauth.wellKnown is not empty use wellKnown to set the endpoints
        //TODO Use OkHttpClientProvider.getInstance().lookup()

        authenticateEndpoint = options.urlPath.authenticateEndpoint
        authorizeEndpoint = options.urlPath.authorizeEndpoint
        tokenEndpoint = options.urlPath.tokenEndpoint
        revokeEndpoint = options.urlPath.revokeEndpoint
        userinfoEndpoint = options.urlPath.userinfoEndpoint
        sessionEndpoint = options.urlPath.sessionEndpoint
        if (StringUtils.isEmpty(sessionEndpoint)) {
            sessionEndpoint = context.getString(R.string.forgerock_logout_endpoint)
        }
        endSessionEndpoint = options.urlPath.endSessionEndpoint
        identifier = UUID.randomUUID().toString()
        val customLogger = options.logger.customLogger
        if (customLogger != null) {
            setCustomLogger(customLogger)
        }
        val logLevel = options.logger.logLevel
        if (logLevel != null) {
            set(logLevel)
        }
    }

    val serverConfig: ServerConfig
        get() = ServerConfig.builder()
            .context(context)
            .identifier(identifier)
            .url(url)
            .realm(realm)
            .timeout(timeout)
            .cookieJarSupplier { this.getCookieJar() }
            .cookieName(cookieName)
            .pins(pins)
            .buildSteps(buildSteps)
            .authenticateEndpoint(authenticateEndpoint)
            .authorizeEndpoint(authorizeEndpoint)
            .tokenEndpoint(tokenEndpoint)
            .revokeEndpoint(revokeEndpoint)
            .userInfoEndpoint(userinfoEndpoint)
            .sessionEndpoint(sessionEndpoint)
            .endSessionEndpoint(endSessionEndpoint)
            .build()

    val oAuth2Client: OAuth2Client
        get() = OAuth2Client.builder()
            .clientId(clientId)
            .scope(scope)
            .redirectUri(redirectUri)
            .serverConfig(serverConfig)
            .build()

    @get:VisibleForTesting
    val tokenManager: TokenManager
        get() = DefaultTokenManager.builder()
            .context(context)
            .sharedPreferences(sharedPreferences)
            .oAuth2Client(oAuth2Client)
            .cacheIntervalMillis(oauthCacheMillis)
            .threshold(oauthThreshold)
            .build()

    val singleSignOnManager: SingleSignOnManager
        get() = DefaultSingleSignOnManager.builder()
            .sharedPreferences(ssoSharedPreferences)
            .serverConfig(serverConfig)
            .context(context)
            .ssoBroadcastModel(sSOBroadcastModel)
            .build()

    val sessionManager: SessionManager
        /**
         * Retrieve the Session Manager that manage the user session.
         *
         * @return The SessionManager
         */
        get() = SessionManager.builder()
            .tokenManager(tokenManager)
            .singleSignOnManager(singleSignOnManager)
            .build()

    private fun getCookieJar(): CookieJar {
        if (cookieJar == null) {
            cookieJar = builder()
                .singleSignOnManager(singleSignOnManager)
                .context(context!!).cacheIntervalMillis(cookieCacheMillis).build()
        }
        return cookieJar!!
    }


    @set:VisibleForTesting
    var sSOBroadcastModel: SSOBroadcastModel?
        get() = if (ssoBroadcastModel == null) {
            SSOBroadcastModel(context).also { ssoBroadcastModel = it }
        } else {
            ssoBroadcastModel
        }
        set(ssoModel) {
            this.ssoBroadcastModel = ssoModel
        }


    @VisibleForTesting
    fun setCookieJar(cookieJar: CookieJar?) {
        this.cookieJar = cookieJar
    }

    fun getBuildSteps(): List<BuildStep<OkHttpClient.Builder>>? {
        return this.buildSteps
    }

    fun setBuildSteps(buildSteps: List<BuildStep<OkHttpClient.Builder>>?) {
        this.buildSteps = buildSteps
    }

    @JvmStatic
    var instance = this

    @JvmStatic
    @VisibleForTesting
    fun reset() {
        context = null
        clientId = null
        redirectUri = null
        scope = null
        oauthCacheMillis = null
        oauthThreshold = null
        cookieCacheMillis = null
        url = null
        realm = null
        timeout = 0
        pins = null
        buildSteps = null
        cookieJar = null
        cookieName = null
        authenticateEndpoint = null
        authorizeEndpoint = null
        tokenEndpoint = null
        revokeEndpoint = null
        userinfoEndpoint = null
        sessionEndpoint = null
        endSessionEndpoint = null
        authServiceName = null
        registrationServiceName = null
        ssoSharedPreferences = null
        sharedPreferences = null
        keyStoreManager = null
        ssoBroadcastModel = null
    }
}
