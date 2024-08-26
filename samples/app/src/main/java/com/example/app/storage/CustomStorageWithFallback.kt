/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.example.app.storage

import android.content.Context
import kotlinx.serialization.Serializable
import org.forgerock.android.auth.AccessToken
import org.forgerock.android.auth.SSOToken
import org.forgerock.android.auth.storage.CookiesStorage
import org.forgerock.android.auth.storage.SSOTokenStorage
import org.forgerock.android.auth.storage.Storage
import org.forgerock.android.auth.storage.TokenStorage

/**
 * A custom storage implementation that switches to a fallback storage when an error occurs.
 */
class CustomStorageWithFallback<T : @Serializable Any>(
    private val context: Context,
    private val flag: String,
    primary: Storage<T>,
    private val fallback: Storage<T>
) : Storage<T> {

    @Volatile
    private var current: Storage<T> = primary

    /**
     * Save an item to the current storage. If an error occurs, switch to the fallback storage.
     *
     * @param item The item to be saved.
     */
    override fun save(item: T) {
        try {
            // Save the item to the current storage.
            current.save(item)
        } catch (e: Throwable) {
            // If an error occurs, switch to the fallback storage.
            context.getSharedPreferences("storage-control", Context.MODE_PRIVATE).edit()
                .putInt(flag, 1).apply()
            fallback.save(item)
            current = fallback
        }
    }

    /**
     * Retrieve an item from the current storage.
     *
     * @return The retrieved item, or null if no item is found.
     */
    override fun get(): T? {
        return current.get()
    }

    /**
     * Delete an item from the current storage.
     */
    override fun delete() {
        current.delete()
    }
}

/**
 * Load the SSO token storage with a fallback mechanism.
 *
 * @param context The application context.
 * @return The storage instance for SSO tokens.
 */
fun loadSSOTokenStorage(context: Context): Storage<SSOToken> {
    return loadStorage(
        context,
        "ssoStorage",
        { SSOTokenStorage(context) },
        { MemoryStorage() }
    )
}

/**
 * Load the token storage with a fallback mechanism.
 *
 * @param context The application context.
 * @return The storage instance for tokens.
 */
fun loadTokenStorage(context: Context): Storage<AccessToken> {
    return loadStorage(
        context,
        "tokenStorage",
        { TokenStorage(context) },
        { MemoryStorage() }
    )
}

/**
 * Load the cookies storage with a fallback mechanism.
 *
 * @param context The application context.
 * @return The storage instance for cookies.
 */
fun loadCookiesStorage(context: Context): Storage<Collection<String>> {
    return loadStorage(
        context,
        "cookiesStorage",
        { CookiesStorage(context) },
        { MemoryStorage() }
    )
}

/**
 * Load a storage instance with a fallback mechanism.
 *
 * @param T The type of object to be stored.
 * @param context The application context.
 * @param flag A flag used to control the storage type.
 * @param primary A function to initialize the primary storage.
 * @param fallback A function to initialize the fallback storage.
 * @return The storage instance.
 */
inline fun <reified T : Any> loadStorage(
    context: Context,
    flag: String,
    primary: () -> Storage<T>,
    fallback: () -> Storage<T>
): Storage<T> {
    val control = context.getSharedPreferences("storage-control", Context.MODE_PRIVATE)
    // Get the storage type from the control flag. 0: primary, 1: fallback.
    val storageType = control.getInt(flag, 0)
    return when (storageType) {
        // Use the primary storage.
        0 -> CustomStorageWithFallback(context,
            flag,
            primary(),
            fallback())

        // Use the fallback storage.
        else -> fallback()
    }
}
