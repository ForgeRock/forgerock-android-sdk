/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import okhttp3.Cookie

/**
 * Interceptor to intercept Http Request Cookie header
 * and invoke registered [RequestInterceptor]
 */
interface OkHttpCookieInterceptor : CookieInterceptor {

    override fun intercept(cookies: List<Cookie>): List<Cookie> {
        var updatedCookies = cookies
        RequestInterceptorRegistry.getInstance().requestInterceptors?.let { requestInterceptors ->
            requestInterceptors.filterIsInstance<CookieInterceptor>()
                .forEach {
                    updatedCookies = it.intercept(updatedCookies)
                }
        }
        return updatedCookies;
    }
}
