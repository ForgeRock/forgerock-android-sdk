/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.storage

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
     *
     * @return The retrieved item, or null if no item is found.
     */
    override fun get(): T? {
        synchronized(lock) {
            return cached ?: delegate.get()
        }
    }

    /**
     * Delete an item from the storage and clear the cache if enabled.
     */
    override fun delete() {
        synchronized(lock) {
            delegate.delete()
            if (cacheable) {
                cached = null
            }
        }
    }
}