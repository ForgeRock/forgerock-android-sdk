/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth

import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.*

internal class InterceptorProvider {
    @JvmOverloads
    fun getInterceptor(frLogger: FRLogger = Logger.frLogger): HttpLoggingInterceptor? {
        val httpLogger = object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                frLogger.network("OKHttpClient..", message, "")
            }
        }
        if (frLogger.isNetworkEnabled()) {
            val logger = when(frLogger) {
                is DefaultLogger -> HttpLoggingInterceptor.Logger.DEFAULT
                else -> httpLogger
            }
            return HttpLoggingInterceptor(logger).apply { this.level = Level.BODY }
         }
        return null
    }
}