/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import okhttp3.OkHttpClient

data class FROptions(val server: Server,
                     val oauth: OAuth,
                     val service: Service,
                     val urlPath: UrlPath,
                     val sslPinning: SSLPinning,
                     val logger: Log) {

    companion object {
        @JvmStatic
        fun equals(old: FROptions?, new: FROptions?): Boolean {
            // referential check first
            if(old === new) {
                return true
            }
            // if there is a change in reference then check the value
            return old?.oauth == new?.oauth
                    && old?.server == new?.server
                    && old?.sslPinning == new?.sslPinning
                    && old?.service == new?.service
                    && old?.urlPath == new?.urlPath
                    && old?.logger == new?.logger
        }
    }
}

class FROptionsBuilder {

    private var server: Server = Server()
    private var oauth: OAuth = OAuth()
    private var service: Service = Service()
    private var urlPath: UrlPath = UrlPath()
    private var sslPinning: SSLPinning = SSLPinning()
    private var logger: Log = Log()

    companion object {
        @JvmStatic
        fun build(block: FROptionsBuilder.() -> Unit): FROptions = FROptionsBuilder().apply(block).build()
    }

    fun server(block: ServerBuilder.() -> Unit) {
        server = ServerBuilder().apply(block).build()
    }

    fun oauth(block: OAuthBuilder.() -> Unit) {
        oauth = OAuthBuilder().apply(block).build()
    }

    fun service(block: ServiceBuilder.() -> Unit) {
        service = ServiceBuilder().apply(block).build()
    }

    fun urlPath(block: UrlPathBuilder.() -> Unit) {
        urlPath = UrlPathBuilder().apply(block).build()
    }

    fun sslPinning(block: SSLPinningBuilder.() -> Unit) {
        sslPinning = SSLPinningBuilder().apply(block).build()
    }

    fun logger(block: LoggerBuilder.() -> Unit) {
        logger = LoggerBuilder().apply(block).build()
    }

    private fun build(): FROptions {
        return FROptions(server, oauth, service, urlPath, sslPinning, logger)
    }

}

data class Server(val url: String? = "",
                  val realm: String? = "root",
                  val timeout: Int = 30,
                  val cookieName: String? = "iPlanetDirectoryPro",
                  val oauthUrl: String? = null)

class ServerBuilder {
    var url: String? = ""
    var realm: String? = "root"
    var timeout: Int = 30
    var cookieName: String? = "iPlanetDirectoryPro"
    var oauthUrl: String? = null

    fun build(): Server = Server(url, realm, timeout, cookieName, oauthUrl)
}

data class OAuth(val oauthClientId: String? = "",
                 val oauthRedirectUri: String = "",
                 val oauthScope: String = "",
                 val oauthThresholdSeconds: Long = 30,
                 val oauthCacheSeconds: Long = 0,
                 val cookieCacheSeconds: Long = 0)

class OAuthBuilder {
    var oauthClientId: String? = ""
    var oauthRedirectUri: String = ""
    var oauthScope: String = ""
    var oauthThresholdSeconds: Long = 30
    var oauthCacheSeconds: Long = 0
    var cookieCacheSeconds: Long = 0

    fun build() : OAuth = OAuth(oauthClientId, oauthRedirectUri, oauthScope, oauthThresholdSeconds, oauthCacheSeconds, cookieCacheSeconds)

}

data class Service(val authServiceName: String? = null,
                   val registrationServiceName: String? = null)

class ServiceBuilder {
    var authServiceName: String? = null
    var registrationServiceName: String? = null

    fun build() : Service = Service(authServiceName, registrationServiceName)

}

data class SSLPinning(val buildSteps: List<BuildStep<OkHttpClient.Builder>> = emptyList(),
                      val pins: List<String> = emptyList())

class SSLPinningBuilder {
    var buildSteps: List<BuildStep<OkHttpClient.Builder>> = emptyList()
    var pins: List<String> = emptyList()

    fun build() : SSLPinning = SSLPinning(buildSteps, pins)

}

data class UrlPath(val authenticateEndpoint: String? = null,
                   val revokeEndpoint: String? = null,
                   val logoutEndpoint: String? = null,
                   val tokenEndpoint: String? = null,
                   val userinfoEndpoint: String? = null,
                   val authorizeEndpoint: String? = null,
                   val endSessionEndpoint: String? = null)

class UrlPathBuilder {
    var authenticateEndpoint: String? = null
    var revokeEndpoint: String? = null
    var logoutEndpoint: String? = null
    var tokenEndpoint: String? = null
    var userinfoEndpoint: String? = null
    var authorizeEndpoint: String? = null
    var endSessionEndpoint: String? = null

    fun build() : UrlPath = UrlPath(authenticateEndpoint, revokeEndpoint, logoutEndpoint, tokenEndpoint, userinfoEndpoint, authorizeEndpoint, endSessionEndpoint)

}

data class Log(val logLevel: Logger.Level? = null, val customLogger: FRLogger? = null)

class LoggerBuilder {
    var customLogger: FRLogger? = null
    var logLevel: Logger.Level? = null
    fun build(): Log = Log(logLevel, customLogger)
}
