package org.forgerock.android.auth


class FROptions {

    var server: Server? = null
        private set
    var oauth: OAuth? = null
        private set
    var service: Service? = null
        private set
    var urlPath: UrlPath? = null
        private set

    companion object {
        @JvmStatic
        fun builder(block: FROptions.() -> Unit): FROptions = FROptions().apply(block)
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

}

data class Server(val url: String,
                  val oauthUrl: String,
                  val realm: String,
                  val timeout: String,
                  val cookieName: String,
                  val pins: String?)

class ServerBuilder {
    lateinit var url: String
    lateinit var realm: String
    lateinit var timeout: String
    lateinit var cookieName: String
    lateinit var oauthUrl: String
    var pins: String? = null

    fun build(): Server = Server(url, realm, timeout, cookieName, oauthUrl, pins)

}

data class OAuth(val oauthClientId: String?,
                 val oauthRedirectUri: String?,
                 val oauthScope: String?,
                 val oauthThresholdSeconds: Long?,
                 val oauthCacheSeconds: Long?,
                 val cookieCacheSeconds: Long?)

class OAuthBuilder {
    var oauthClientId: String? = null
    var oauthRedirectUri: String? = null
    var oauthScope: String? = null
    var oauthThresholdSeconds: Long? = null
    var oauthCacheSeconds: Long? = null
    var cookieCacheSeconds: Long? = null

    fun build() : OAuth = OAuth(oauthClientId, oauthRedirectUri, oauthScope, oauthThresholdSeconds, oauthCacheSeconds, cookieCacheSeconds)

}

data class Service(val authServiceName: String?,
                   val registrationServiceName: String?)

class ServiceBuilder {
    var authServiceName: String? = null
    var registrationServiceName: String? = null

    fun build() : Service = Service(authServiceName, registrationServiceName)

}

data class UrlPath(val authenticateEndpoint: String?,
                   val revokeEndpoint: String?,
                   val logoutEndpoint: String?,
                   val tokenEndpoint: String?,
                   val userinfoEndpoint: String?,
                   val authorizeEndpoint: String?)

class UrlPathBuilder {
    var authenticateEndpoint: String? = null
    var revokeEndpoint: String? = null
    var logoutEndpoint: String? = null
    var tokenEndpoint: String? = null
    var userinfoEndpoint: String? = null
    var authorizeEndpoint: String? = null

    fun build() : UrlPath = UrlPath(authenticateEndpoint, revokeEndpoint, logoutEndpoint, tokenEndpoint, userinfoEndpoint, authorizeEndpoint)

}