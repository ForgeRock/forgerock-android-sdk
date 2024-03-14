/*
 * Copyright (c) 2019 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import okhttp3.CertificatePinner
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.ConcurrentHashMap

/**
 * Provider to Cache and provide OKHttpClient
 */
class OkHttpClientProvider private constructor() {
    private val cache: MutableMap<String, OkHttpClient?> = ConcurrentHashMap()
    private val interceptorProvider = InterceptorProvider()

    init {
        CoreEventDispatcher.CLEAR_OKHTTP.addObserver { _, _ -> clear() }
    }

    /**
     * Create or lookup a cached OKHttpClient
     *
     * @param networkConfig The Server configuration
     * @return The OkHttpClient
     */
    fun lookup(networkConfig: NetworkConfig): OkHttpClient {
        var client = cache[networkConfig.identifier]
        if (client != null) {
            return client
        }
        val builder =
            OkHttpClient.Builder()
                .connectTimeout(networkConfig.timeout.toLong(), networkConfig.timeUnit)
                .readTimeout(networkConfig.timeout.toLong(), networkConfig.timeUnit)
                .writeTimeout(networkConfig.timeout.toLong(), networkConfig.timeUnit)
                .followRedirects(false)
        if (networkConfig.cookieJar == null) {
            builder.cookieJar(CookieJar.NO_COOKIES)
        } else {
            builder.cookieJar(networkConfig.cookieJar)
        }
        if (networkConfig.interceptorSupplier != null &&
            networkConfig.interceptorSupplier.get() != null
        ) {
            for (i in networkConfig.interceptorSupplier.get()) {
                builder.addInterceptor(i)
            }
        }

        builder.addInterceptor(
            Interceptor { chain ->
                val request =
                    chain.request().newBuilder()
                        .header(REQUESTED_WITH_KEY, REQUESTED_WITH_VALUE)
                        .header(REQUESTED_PLATFORM_KEY, REQUESTED_PLATFORM_VALUE)
                        .build()
                return@Interceptor chain.proceed(request)
            },
        )

        val networkInterceptor: HttpLoggingInterceptor? = interceptorProvider.getInterceptor()
        networkInterceptor?.let {
            builder.addInterceptor(it)
        }

        if (networkConfig.pins.isNotEmpty()) {
            val cpBuilder = CertificatePinner.Builder()
            for (s in networkConfig.pins) {
                cpBuilder.add(networkConfig.host, "sha256/$s")
            }
            builder.certificatePinner(cpBuilder.build())
        }
        for (buildStep in networkConfig.buildSteps) {
            buildStep?.build(builder)
        }

        client = builder.build()
        cache[networkConfig.identifier] = client
        return client
    }

    /**
     * Clear the cached {[OkHttpClient]}
     */
    fun clear() {
        cache.clear()
    }

    companion object {
        private val providerInstance = OkHttpClientProvider()
        private const val REQUESTED_WITH_KEY = "x-requested-with"
        private const val REQUESTED_WITH_VALUE = "forgerock-sdk"
        private const val REQUESTED_PLATFORM_KEY = "x-requested-platform"
        private const val REQUESTED_PLATFORM_VALUE = "android"

        @JvmStatic
        fun getInstance(): OkHttpClientProvider {
            return providerInstance
        }
    }
}
