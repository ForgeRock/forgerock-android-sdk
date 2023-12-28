/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth

import okhttp3.Cookie

/**
 * Observes, modifies outgoing request cookie header from the SDK.
 * Interceptors can be used to add, remove, or transform cookie headers on the request.
 */
interface CookieInterceptor {

    /**
     * Intercepts outgoing request cookie header from the SDK.
     *
     * @param cookies The original outgoing cookies
     * @return The Updated Cookies
     */
    fun intercept(cookies: List<Cookie>): List<Cookie>
}