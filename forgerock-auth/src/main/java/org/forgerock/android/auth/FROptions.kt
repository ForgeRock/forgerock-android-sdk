/*
 * Copyright (c) 2022 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.forgerock.android.auth.OkHttpClientProvider.getInstance
import org.forgerock.android.auth.exception.ApiException
import org.forgerock.android.auth.storage.Storage
import org.json.JSONObject
import java.net.URL

/**
 * Manages SDK configuration information
 */
data class FROptions(val server: Server,
                     val oauth: OAuth,
                     val service: Service,
                     val urlPath: UrlPath,
                     val sslPinning: SSLPinning,
                     val logger: Log,
                     val store: Store) {

    companion object {
        @JvmStatic
        fun equals(old: FROptions?, new: FROptions?): Boolean {
            // do the referential check first
            if (old === new) {
                return true
            }
            // if there is a change in reference then check the value
            return old?.oauth == new?.oauth
                    && old?.server == new?.server
                    && old?.sslPinning == new?.sslPinning
                    && old?.service == new?.service
                    && old?.urlPath == new?.urlPath
                    && old?.logger == new?.logger
                    && old?.store == new?.store
        }
    }

    @Throws(IllegalArgumentException::class)
    @JvmName("validateConfig")
    internal fun validateConfig() {
        require(server.url.isNotBlank()) { "Server URL cannot be blank" }
        require(server.realm.isNotBlank()) { "Realm cannot be blank" }
        require(server.cookieName.isNotBlank()) { "cookieName cannot be blank" }
    }

    private fun getOkHttpClient(url: URL): OkHttpClient {
        val networkConfig = NetworkConfig.networkBuilder()
            .host(url.authority)
            .interceptorSupplier {
                listOf<Interceptor>(
                    OkHttpRequestInterceptor()
                )
            }
            .build()
        // Obtain instance of OkHttp client
        val httpClient = getInstance().lookup(networkConfig)
        return httpClient
    }

    /**
     * Discover the OpenID Configuration
     *
     * @param discoverUrl The OpenID Configuration URL
     * @return The SDK configuration information
     */
    suspend fun discover(discoverUrl: String): FROptions {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(discoverUrl)
                val httpClient = getOkHttpClient(url)
                val request = Request.Builder()
                    .url(discoverUrl)
                    .get().build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful.not()) {
                        throw ApiException(
                            response.code,
                            response.message,
                            response.body?.string()
                        )
                    }

                    val openIdConfiguration = response.body?.string()?.let {
                        JSONObject(it)
                    }

                    val issuer = openIdConfiguration?.getString("issuer") ?: ""
                    val server =
                        this@FROptions.server.copy(url = (server.url.isNotEmpty() then server.url)
                            ?: issuer)
                    val urlPath = this@FROptions.urlPath.copy(
                        authorizeEndpoint = openIdConfiguration?.getString("authorization_endpoint"),
                        tokenEndpoint = openIdConfiguration?.getString("token_endpoint"),
                        userinfoEndpoint = openIdConfiguration?.getString("userinfo_endpoint"),
                        revokeEndpoint = openIdConfiguration?.getString("revocation_endpoint"),
                        endSessionEndpoint = if (oauth.oauthSignOutRedirectUri.isEmpty()) {
                            openIdConfiguration?.optString("ping_end_idp_session_endpoint",
                                openIdConfiguration.getString("end_session_endpoint"))
                        } else {
                            openIdConfiguration?.getString("end_session_endpoint")
                        }
                    )
                    this@FROptions.copy(urlPath = urlPath, server = server)

                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

}

/**
 * Option builder to build the SDK configuration information
 */
class FROptionsBuilder {

    private var server: Server = Server("", "root", 30, "iPlanetDirectoryPro", 0)
    private var oauth: OAuth = OAuth()
    private var service: Service = Service()
    private var urlPath: UrlPath = UrlPath()
    private var sslPinning: SSLPinning = SSLPinning()
    private var logger: Log = Log()
    private var store: Store = Store()

    companion object {
        @JvmStatic
        fun build(block: FROptionsBuilder.() -> Unit): FROptions =
            FROptionsBuilder().apply(block).build()
    }

    fun store(block: Store.() -> Unit) {
        store = Store().apply(block)
    }

    /**
     * Build the server configurations
     *
     * @param block  takes the  closure to set the values for server configuration.
     */
    fun server(block: ServerBuilder.() -> Unit) {
        server = ServerBuilder().apply(block).build()
    }

    /**
     * Build the oauth configurations
     *
     * @param block takes the  closure to set the values for oauth configuration.
     */
    fun oauth(block: OAuthBuilder.() -> Unit) {
        oauth = OAuthBuilder().apply(block).build()
    }

    /**
     * Build the service configurations
     *
     * @param block takes the closure to set the values for service configuration.
     */
    fun service(block: ServiceBuilder.() -> Unit) {
        service = ServiceBuilder().apply(block).build()
    }

    /**
     * Build the endpoints
     *
     * @param block takes the  closure to set the values for urls.
     */
    fun urlPath(block: UrlPathBuilder.() -> Unit) {
        urlPath = UrlPathBuilder().apply(block).build()
    }

    /**
     * Build the ssl pinning
     *
     * @param block takes the builder closure to set the values for ssl pinning.
     */
    fun sslPinning(block: SSLPinningBuilder.() -> Unit) {
        sslPinning = SSLPinningBuilder().apply(block).build()
    }

    /**
     * Build the custom logger
     *
     * @param block takes the builder closure to configure custom logger.
     */
    fun logger(block: LoggerBuilder.() -> Unit) {
        logger = LoggerBuilder().apply(block).build()
    }

    private fun build(): FROptions {
        return FROptions(server, oauth, service, urlPath, sslPinning, logger, store)
    }

}

data class Store(var oidcStorage: Storage<AccessToken> = Options.oidcStorage,
                 var ssoTokenStorage: Storage<SSOToken> = Options.ssoTokenStorage,
                 var cookiesStorage: Storage<Collection<String>> = Options.cookieStorage
)

/**
 * Data class for the server configurations
 */
data class Server(val url: String = "",
                  val realm: String = "root",
                  val timeout: Int = 30,
                  val cookieName: String = "iPlanetDirectoryPro",
                  val cookieCacheSeconds: Long = 0)

/**
 * Server builder to build the SDK configuration information specific to server
 */
class ServerBuilder {
    var url: String = ""
    var realm: String = "root"
    var timeout: Int = 30
    var cookieName: String = "iPlanetDirectoryPro"
    var cookieCacheSeconds: Long = 0
    fun build(): Server {
        return Server(url, realm, timeout, cookieName, cookieCacheSeconds)
    }
}

/**
 * Data class for the OAuth configurations
 */
data class OAuth(val oauthClientId: String = "",
                 val oauthRedirectUri: String = "",
                 val oauthSignOutRedirectUri: String = "",
                 val oauthScope: String = "",
                 val oauthThresholdSeconds: Long = 0,
                 val oauthCacheSeconds: Long = 0)

/**
 * Oauth builder to build the SDK configuration information specific to oauth
 */
class OAuthBuilder {
    var oauthClientId: String = ""
    var oauthRedirectUri: String = ""
    var oauthSignOutRedirectUri: String = ""
    var oauthScope: String = ""
    var oauthThresholdSeconds: Long = 0
    var oauthCacheSeconds: Long = 0

    fun build(): OAuth = OAuth(oauthClientId, oauthRedirectUri, oauthSignOutRedirectUri,
        oauthScope, oauthThresholdSeconds, oauthCacheSeconds)

}

/**
 * Data class for the Service configurations
 */
data class Service(val authServiceName: String = "Login",
                   val registrationServiceName: String = "Registration")

/**
 * Service builder to build the SDK configuration information specific to services
 */
class ServiceBuilder {
    var authServiceName: String = "Login"
    var registrationServiceName: String = "Registration"

    fun build(): Service = Service(authServiceName, registrationServiceName)

}

/**
 * Data class for the SSL pinning configurations
 */
data class SSLPinning(val buildSteps: List<BuildStep<OkHttpClient.Builder>> = emptyList(),
                      val pins: List<String> = emptyList())

/**
 * SSL builder to build the SDK configuration information specific to SSL
 */
class SSLPinningBuilder {
    var buildSteps: List<BuildStep<OkHttpClient.Builder>> = emptyList()
    var pins: List<String> = emptyList()

    fun build(): SSLPinning = SSLPinning(buildSteps, pins)

}

/**
 * Data class for the URL configurations
 */
data class UrlPath(val authenticateEndpoint: String? = null,
                   val revokeEndpoint: String? = null,
                   val sessionEndpoint: String? = null,
                   val tokenEndpoint: String? = null,
                   val userinfoEndpoint: String? = null,
                   val authorizeEndpoint: String? = null,
                   val endSessionEndpoint: String? = null)

/**
 * URL Path builder to build the SDK configuration information specific to endpoints
 */
class UrlPathBuilder {
    var authenticateEndpoint: String? = null
    var revokeEndpoint: String? = null
    var sessionEndpoint: String? = null
    var tokenEndpoint: String? = null
    var userinfoEndpoint: String? = null
    var authorizeEndpoint: String? = null
    var endSessionEndpoint: String? = null

    fun build(): UrlPath = UrlPath(authenticateEndpoint,
        revokeEndpoint,
        sessionEndpoint,
        tokenEndpoint,
        userinfoEndpoint,
        authorizeEndpoint,
        endSessionEndpoint)

}

/**
 * Data class to set the log level and custom logger configurations
 */
data class Log(val logLevel: Logger.Level? = null, val customLogger: FRLogger? = null)

/**
 * Logger builder to build the SDK configuration information specific to loggers
 */
class LoggerBuilder {
    var customLogger: FRLogger? = null
    var logLevel: Logger.Level? = null
    fun build(): Log = Log(logLevel, customLogger)
}
