/*
 * Copyright (c) 2024 - 2025 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.storage

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class StorageDelegateTest {

    @Mock
    private lateinit var mockStorage: Storage<String>

    private lateinit var cacheableDelegate: StorageDelegate<String>
    private lateinit var nonCacheableDelegate: StorageDelegate<String>

    @Before
    fun setUp() {
        cacheableDelegate = StorageDelegate(cacheable = true) { mockStorage }
        nonCacheableDelegate = StorageDelegate(cacheable = false) { mockStorage }
    }

    @Test
    fun testSaveWithCache() {
        cacheableDelegate.save("test")
        verify(mockStorage).save("test")
        assertEquals("test", cacheableDelegate.get())
        // Should not call get on delegate after caching
        verify(mockStorage, never()).get()
    }

    @Test
    fun testSaveWithoutCache() {
        nonCacheableDelegate.save("test")
        verify(mockStorage).save("test")

        `when`(mockStorage.get()).thenReturn("test")
        assertEquals("test", nonCacheableDelegate.get())
        // Should call get on delegate when not caching
        verify(mockStorage).get()
    }

    @Test
    fun testGetWithException() {
        `when`(mockStorage.get()).thenThrow(RuntimeException("Storage error"))
        assertNull(nonCacheableDelegate.get())
        // Verify delete was called when get throws exception
        verify(mockStorage).delete()
    }

    @Test
    fun testDeleteWithException() {
        doThrow(RuntimeException("Delete error")).`when`(mockStorage).delete()

        // Should not throw exception
        nonCacheableDelegate.delete()
        verify(mockStorage).delete()
    }

    @Test
    fun testDeleteClearsCache() {
        // Populate the cache
        cacheableDelegate.save("test")
        assertEquals("test", cacheableDelegate.get())
        verify(mockStorage, never()).get()

        // Delete should clear cache
        cacheableDelegate.delete()
        verify(mockStorage).delete()

        // Next get should call the delegate
        `when`(mockStorage.get()).thenReturn("test2")
        assertEquals("test2", cacheableDelegate.get())
        verify(mockStorage).get()
    }
}

