/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.internal.toImmutableList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class SecureCookieJar(context: Context,
                      singleSignOnManager: SingleSignOnManager?,
                      cacheIntervalMillis: Long?) : CookieJar, OkHttpCookieInterceptor {

    private val singleSignOnManager: SingleSignOnManager
    private val cacheRef = AtomicReference<MutableSet<Cookie>?>()
    private val cacheIntervalMillis: Long
    private val cookieMarshaller = CookieMarshaller()

    init {
        this.singleSignOnManager = singleSignOnManager ?: Config.getInstance().singleSignOnManager
        this.cacheIntervalMillis = cacheIntervalMillis
            ?: (context.resources.getInteger(R.integer.forgerock_cookie_cache) * 1000).toLong()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        var cookies = cacheRef.get()
        if (cookies == null) {
            cookies = HashSet()
            val storedCookies = singleSignOnManager.cookies
            if (!storedCookies.isEmpty()) {
                val updatedCookies: MutableSet<String> = HashSet(storedCookies)
                val iterator = updatedCookies.iterator()
                while (iterator.hasNext()) {
                    val cookie = cookieMarshaller.unmarshal(iterator.next())
                    if (cookie != null) {
                        if (!isExpired(cookie)) {
                            cookies.add(cookie)
                        } else {
                            //Remove expired cookies
                            iterator.remove()
                        }
                    } else {
                        //Failed to parse it
                        iterator.remove()
                    }
                }

                // Some cookies are expired, or failed to parse, remove it
                if (storedCookies.size != updatedCookies.size) {
                    cache(cookies)
                    persist(updatedCookies)
                }
            }
        }
        return filter(url, cookies)
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val cookiesContainer: MutableSet<Cookie> = HashSet()
        for (c in singleSignOnManager.cookies) {
            val cookie = cookieMarshaller.unmarshal(c)
            //Remove the same stored cookies
            if (cookie != null && !contains(cookie, cookies)) {
                cookiesContainer.add(cookie)
            }
        }
        for (cookie in cookies) {
            if (!isExpired(cookie)) {
                cookiesContainer.add(cookie)
            }
        }
        cache(cookiesContainer)
        val updatedCookies: MutableSet<String> = HashSet()
        for (cookie in cookiesContainer) {
            updatedCookies.add(cookieMarshaller.marshal(cookie))
        }
        persist(updatedCookies)
    }

    private fun contains(input: Cookie, cookies: Collection<Cookie>): Boolean {
        for (cookie in cookies) {
            if (cookie.name == input.name && cookie.domain == input.domain && cookie.path == input.path) {
                return true
            }
        }
        return false
    }

    private fun filter(url: HttpUrl, cookies: Set<Cookie>): List<Cookie> {
        val result = mutableListOf<Cookie>()
        cookies.filter { !isExpired(it) }
            .filter { it.matches(url) }
            .toCollection(result)

        return intercept(result.toImmutableList())
    }

    private fun isExpired(cookie: Cookie): Boolean {
        return cookie.expiresAt < System.currentTimeMillis()
    }

    private fun cache(cookies: MutableSet<Cookie>) {
        if (cacheIntervalMillis > 0) {
            cacheRef.set(cookies)
            worker.schedule({ cacheRef.set(null) }, cacheIntervalMillis, TimeUnit.MILLISECONDS)
        }
    }

    private fun persist(cookies: Collection<String>) {
        singleSignOnManager.persist(cookies)
        FRLifecycle.dispatchCookiesUpdated(cookies)
    }

    class SecureCookieJarBuilder internal constructor() {
        private lateinit var context: Context
        private var singleSignOnManager: SingleSignOnManager? = null
        private var cacheIntervalMillis: Long? = null
        fun context(context: Context): SecureCookieJarBuilder {
            this.context = context
            return this
        }

        fun singleSignOnManager(singleSignOnManager: SingleSignOnManager?): SecureCookieJarBuilder {
            this.singleSignOnManager = singleSignOnManager
            return this
        }

        fun cacheIntervalMillis(cacheIntervalMillis: Long?): SecureCookieJarBuilder {
            this.cacheIntervalMillis = cacheIntervalMillis
            return this
        }

        fun build(): SecureCookieJar {
            return SecureCookieJar(context, singleSignOnManager, cacheIntervalMillis)
        }

        override fun toString(): String {
            return "SecureCookieJar.SecureCookieJarBuilder(context=$context, " +
                    "singleSignOnManager=$singleSignOnManager, cacheIntervalMillis=$cacheIntervalMillis)"
        }
    }

    companion object {
        private val worker = Executors.newSingleThreadScheduledExecutor()

        @JvmStatic
        fun builder(): SecureCookieJarBuilder {
            return SecureCookieJarBuilder()
        }
    }
}
