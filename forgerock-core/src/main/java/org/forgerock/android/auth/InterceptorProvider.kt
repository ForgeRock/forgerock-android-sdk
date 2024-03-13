/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth

import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.*
import org.lighthousegames.logging.logging

internal class InterceptorProvider {

    val log = logging()
    @JvmOverloads
    fun getInterceptor(frLogger: FRLogger = Logger.frLogger): HttpLoggingInterceptor? {
        val httpLogger = Logger { message ->
            log.debug("OKHttpClient..") { message }
           // MFLogger.network("OKHttpClient..", message)
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