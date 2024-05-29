/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import lombok.Singular
import org.forgerock.android.auth.Config.instance
import org.forgerock.android.auth.ConfigHelper.Companion.getPersistedConfig
import org.forgerock.android.auth.ConfigHelper.Companion.isConfigDifferentFromPersistedValue
import org.forgerock.android.auth.ConfigHelper.Companion.load
import org.forgerock.android.auth.ConfigHelper.Companion.persist
import org.forgerock.android.auth.FROptions.Companion.equals
import java.util.Collections

/**
 * Model of an FRAuth.
 *
 *
 * Dispatches requests to [AuthService] and [OAuth2Client], performs user authentication and manage
 * user session.
 *
 *
 *
 * To create a new FRAuth object use the static [FRAuth.builder].
 */
class FRAuth private constructor(context: Context,
                                 serviceName: String?,
                                 advice: PolicyAdvice?,
                                 resumeURI: Uri?,
                                 serverConfig: ServerConfig?,
                                 sessionManager: SessionManager?,
                                 @Singular interceptors: List<Interceptor<*>>) {
    private val authService: AuthService
    private val sessionManager: SessionManager

    init {
        this.sessionManager = sessionManager ?: instance.sessionManager

        val builder = AuthService.builder()
            .name(serviceName)
            .advice(advice)
            .resumeURI(resumeURI)
            .serverConfig(serverConfig ?: instance.serverConfig)
            .interceptor(SingleSignOnInterceptor(
                this.sessionManager))

        for (interceptor in interceptors) {
            builder.interceptor(interceptor)
        }

        authService = builder.build()
    }

    /**
     * Trigger the Authentication Tree flow process.
     *
     * @param context  The Application Context
     * @param listener Listener for receiving [FRAuth] related changes
     */
    fun next(context: Context?, listener: NodeListener<*>?) {
        authService.next(context, listener)
    }

    class FRAuthBuilder {
        private var context: Context? = null
        private var serviceName: String? = null
        private var advice: PolicyAdvice? = null
        private var resumeURI: Uri? = null
        private var serverConfig: ServerConfig? = null
        private var sessionManager: SessionManager? = null
        private var interceptors: ArrayList<Interceptor<*>>? = null
        fun build(): FRAuth {
            val interceptors = when (if (this.interceptors == null) 0 else interceptors!!.size) {
                0 -> emptyList()
                1 -> listOf(
                    interceptors!![0])

                else -> Collections.unmodifiableList(ArrayList(this.interceptors))
            }
            return FRAuth(context!!,
                serviceName,
                advice,
                resumeURI,
                serverConfig,
                sessionManager,
                interceptors)
        }

        fun context(context: Context?): FRAuthBuilder {
            this.context = context
            return this
        }

        fun serviceName(serviceName: String?): FRAuthBuilder {
            this.serviceName = serviceName
            return this
        }

        fun advice(advice: PolicyAdvice?): FRAuthBuilder {
            this.advice = advice
            return this
        }

        fun resumeURI(resumeURI: Uri?): FRAuthBuilder {
            this.resumeURI = resumeURI
            return this
        }

        fun serverConfig(serverConfig: ServerConfig?): FRAuthBuilder {
            this.serverConfig = serverConfig
            return this
        }

        fun sessionManager(sessionManager: SessionManager?): FRAuthBuilder {
            this.sessionManager = sessionManager
            return this
        }

        fun interceptor(interceptor: Interceptor<*>): FRAuthBuilder {
            if (this.interceptors == null) this.interceptors = ArrayList()
            interceptors!!.add(interceptor)
            return this
        }

        override fun toString(): String {
            return "FRAuth.FRAuthBuilder(context=" + this.context + ", serviceName=" + this.serviceName + ", advice=" + this.advice + ", resumeURI=" + this.resumeURI + ", serverConfig=" + this.serverConfig + ", sessionManager=" + this.sessionManager + ", interceptors=" + this.interceptors + ")"
        }
    }

    companion object {
        private var started = false

        private var cachedOptions: FROptions? = null
        private val lock = Mutex()

        /**
         * Start the SDK
         *
         * @param context The Application Context
         * @param options The FROptions is a nullable field which takes either a null or config. If the caller passes null it fetches the default values from strings.xml .
         * @Deprecated Use [init] instead
         */
        @JvmStatic
        @Deprecated("Use [init] instead", ReplaceWith("init(context, options)"))
        fun start(context: Context?, options: FROptions?) {
            runBlocking(Dispatchers.Default) {
                init(context!!, options)
            }
        }

        suspend fun init(context: Context, options: FROptions? = null) = lock.withLock {
            if (!started || !equals(cachedOptions, options)) {
                started = true
                val currentOptions = load(context, options)
                //Validate (AM URL, Realm, CookieName) is not Empty. If its empty will throw IllegalArgumentException.
                //TODO We may need to remove this validation to support DaVinci, we may not require that if well-known is provided
                currentOptions.validateConfig()
                if (isConfigDifferentFromPersistedValue(context, currentOptions)) {
                    val sessionManager = getPersistedConfig(
                        context, cachedOptions).sessionManager
                    sessionManager.close()
                }
                Config.init(context, currentOptions)
                persist(context, currentOptions)
                cachedOptions = options
            }
        }

        @JvmStatic
        @Deprecated("Use [init] instead", ReplaceWith("init(context, options)"))
        fun start(context: Context?) {
            start(context, null)
        }

        @JvmStatic
        fun builder(): FRAuthBuilder {
            return FRAuthBuilder()
        }
    }
}