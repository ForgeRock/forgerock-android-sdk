/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

/**
 * A Cache entry with time to live,
 * the cached data should consider invalid after expired.
 */
internal class CacheEntry<T>(val value: T, ttl: Long) {

    private val expiryTime: Long

    init {
        expiryTime = System.currentTimeMillis() + ttl
    }

    val isExpired: Boolean
        get() = System.currentTimeMillis() >= expiryTime
}
