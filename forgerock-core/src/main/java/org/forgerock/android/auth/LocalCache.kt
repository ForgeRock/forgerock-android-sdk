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
class LocalCache<T> public constructor() {

    companion object {

        @Volatile
        private var instance: LocalCache<*>? = null

        @JvmStatic
        fun <T> getInstance(): LocalCache<T> {
            synchronized(this) {
                if (instance == null) {
                    instance = LocalCache<T>()
                }
                @Suppress("UNCHECKED_CAST")
                return instance as LocalCache<T>
            }
        }
    }

    private var data: T? = null

    fun getData(): T? {
        synchronized(this) {
            return data
        }
    }

    fun setData(value: T) {
        synchronized(this) {
            data = value
        }
    }
}