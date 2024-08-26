/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.storage

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class SecureSharedPreferencesStorageTest {
    private val context: Context by lazy { ApplicationProvider.getApplicationContext<Application>() }
    private lateinit var storage: Storage<Data>

    @Before
    fun setUp() {
        storage = StorageDelegate {
            SecureSharedPreferencesStorage(context,
                filename = "dummy",
                keyAlias = "dummy-key",
                key = "mykey",
                serializer = Json.serializersModule.serializer())
        }
    }

    @After
    fun tearDown() {
        storage.delete()
    }


    @Test
    fun testDataStore() {
        storage.save(Data(1, "test"))
        val storedData = storage.get()
        assertEquals(1, storedData!!.a)
        assertEquals("test", storedData.b)
    }

    @Test
    fun testMultipleData() {
        val storage = StorageDelegate<List<Data>> {
            SecureSharedPreferencesStorage(context,
                filename = "dummy",
                keyAlias = "dummy-key",
                key = "mykey",
                serializer = Json.serializersModule.serializer())
        }
        val dataList = listOf(Data(1, "test1"), Data(2, "test2"))
        storage.save(dataList)
        val storedData = storage.get()
        assertEquals(dataList, storedData)
    }

    @Test
    fun testDeleteData() {
        val data = Data(1, "test")
        storage.save(data)
        storage.delete()
        val storedData = storage.get()
        assertEquals(null, storedData)
    }

    @Test
    fun testDifferentDataObjectsWithSameStorage() {
        val storageData = storage
        val storageData2 = StorageDelegate<Data2> {
            SecureSharedPreferencesStorage(context,
                filename = "dummy", // Same filename
                keyAlias = "dummy-key", // Same keyAlias
                key = "mykey2", // Different key
                serializer = Json.serializersModule.serializer())
        }

        val data = Data(1, "test")
        val data2 = Data2(2, "test1")

        storageData.save(data)
        storageData2.save(data2)

        val storedData = storageData.get()
        val storedData2 = storageData2.get()

        assertEquals(data, storedData)
        assertEquals(data2, storedData2)

        storageData.delete()
        assertNull(storageData.get())
        assertNotNull(storageData2.get())
    }
}