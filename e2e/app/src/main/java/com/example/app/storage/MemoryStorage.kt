/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.example.app.storage

import org.forgerock.android.auth.storage.Storage
import org.forgerock.android.auth.storage.StorageDelegate

/**
 * A simple in-memory storage implementation for storing and retrieving objects.
 *
 * @param T The type of object to be stored.
 */
class Memory<T : Any> : Storage<T> {
    private var data: T? = null

    /**
     * Save an item to the in-memory storage.
     *
     * @param item The item to be saved.
     */
    override fun save(item: T) {
        data = item
    }

    /**
     * Retrieve an item from the in-memory storage.
     *
     * @return The retrieved item, or null if no item is found.
     */
    override fun get(): T? = data

    /**
     * Delete an item from the in-memory storage.
     */
    override fun delete() {
        data = null
    }
}

/**
 * Factory function to create a `MemoryStorage` instance.
 *
 * @param T The type of object to be stored.
 * @return A `StorageDelegate` for `MemoryStorage`.
 */
inline fun <reified T : Any> MemoryStorage(): StorageDelegate<T> = StorageDelegate {
    Memory()
}