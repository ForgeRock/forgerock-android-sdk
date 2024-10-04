/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.storage

/**
 * Interface to persist and retrieve objects.
 *
 * @param T The type of object to be stored.
 */
interface Storage<T : Any> {

    /**
     * Save an item to the storage.
     *
     * @param item The item to be saved.
     */
    fun save(item: T)

    /**
     * Retrieve an item from the storage.
     *
     * @return The retrieved item, or null if no item is found.
     */
    fun get(): T?

    /**
     * Delete an item from the storage.
     */
    fun delete()
}