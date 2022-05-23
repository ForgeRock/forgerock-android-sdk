/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth

import okhttp3.logging.HttpLoggingInterceptor

internal class InterceptorProvider @JvmOverloads constructor(private val customLogger: FRLogger? = null,
                                                             private val isDebugEnabled: Boolean = false) {
    fun getInterceptor(): HttpLoggingInterceptor? {
        val httpLogger = object: HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                customLogger?.network("OKHttpClient..", message, "")
            }
        }
        if(customLogger != null) {
            return  HttpLoggingInterceptor(httpLogger).apply { this.level = HttpLoggingInterceptor.Level.BODY }
        }
        if(isDebugEnabled) {
            return  HttpLoggingInterceptor().apply { this.level = HttpLoggingInterceptor.Level.BODY }
        }
        return null
    }
}