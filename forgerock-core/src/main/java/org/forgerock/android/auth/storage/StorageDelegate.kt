/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.storage

import org.forgerock.android.auth.Logger

private val TAG = StorageDelegate::class.simpleName

/**
 * A delegate class for managing storage.
 *
 * @param T The type of object to be stored.
 * @param cacheable Whether the storage should cache the item in memory.
 * @param initializer A function to initialize the underlying storage.
 */
class StorageDelegate<T : Any>(
    private val cacheable: Boolean = false,
    initializer: () -> Storage<T>
) : Storage<T> {
    private val delegate: Storage<T> by lazy(initializer)
    private var lock = Any()
    private var cached: T? = null

    /**
     * Save an item to the storage and optionally cache it.
     *
     * @param item The item to be saved.
     */
    override fun save(item: T) {
        synchronized(lock) {
            delegate.save(item)
            if (cacheable) {
                cached = item
            }
        }
    }

    /**
     * Retrieve an item from the storage, using the cache if available.
     * If an exception occurs during retrieval, the item will be deleted.
     *
     * @return The retrieved item, or null if no item is found or an exception occurs.
     */
    override fun get(): T? {
        synchronized(lock) {
            return cached ?: try {
                delegate.get()
            } catch (e: Exception) {
                Logger.error(TAG, e, "Failed to retrieve item from storage")
                delete()
                null
            }
        }
    }

    /**
     * Delete an item from the storage and clear the cache if enabled.
     */
    override fun delete() {
        synchronized(lock) {
            try {
                delegate.delete()
            } catch (e: Exception) {
                Logger.error(TAG, e, "Failed to delete item from storage", e)
            }

            if (cacheable) {
                cached = null
            }
        }
    }
}