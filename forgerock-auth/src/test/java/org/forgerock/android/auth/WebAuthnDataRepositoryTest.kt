/*
 * Copyright (c) 2022 - 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.*
import org.forgerock.android.auth.webauthn.PublicKeyCredentialSource
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class WebAuthnDataRepositoryTest {

    val context: Context = ApplicationProvider.getApplicationContext()
    private var sharedPreferences = context.getSharedPreferences("Test", Context.MODE_PRIVATE )
    private var repository: WebAuthnDataRepository = object: WebAuthnDataRepository(context, sharedPreferences) {
        override fun getNewSharedPreferences(): SharedPreferences {
            return sharedPreferences
        }
    }
    private lateinit var source1: PublicKeyCredentialSource
    private lateinit var source2: PublicKeyCredentialSource
    private lateinit var source3: PublicKeyCredentialSource
    private lateinit var source4: PublicKeyCredentialSource

    @Before
    fun prepare() {
        source1 = PublicKeyCredentialSource.builder()
            .id("test1".toByteArray())
            .rpid("rpid")
            .otherUI("otherUI1")
            .userHandle("userHandle1".toByteArray()).build()
        source2 = PublicKeyCredentialSource.builder()
            .id("test2".toByteArray())
            .rpid("rpid")
            .otherUI("otherUI2")
            .userHandle("userHandle2".toByteArray()).build()
        source3 = PublicKeyCredentialSource.builder()
            .id("test3".toByteArray())
            .rpid("rpid3")
            .otherUI("otherUI3")
            .userHandle("userHandle3".toByteArray()).build()
        source4 = PublicKeyCredentialSource.builder()
            .id("test4".toByteArray())
            .rpid("rpid")
            .otherUI("otherUI4")
            .userHandle("userHandle4".toByteArray()).build()

    }

    @After
    fun clearupData() {
        sharedPreferences.edit().clear().apply()
    }

    @Test
    fun testPersist(): Unit = runBlocking {
        repository.persist(source1)
        delay(1)
        repository.persist(source2)
        val sources = repository.getPublicKeyCredentialSource(source1.rpid)
        assertThat(sources).hasSize(2)
        assertThat(sources[1].id).isEqualTo(source2.id)
        assertThat(sources[1].rpid).isEqualTo(source2.rpid)
        assertThat(sources[1].otherUI).isEqualTo(source2.otherUI)
        assertThat(sources[1].userHandle).isEqualTo(source2.userHandle)
        assertThat(sources[1].type).isEqualTo(source2.type)
        assertThat(sources[0].id).isEqualTo(source1.id)
        assertThat(sources[0].rpid).isEqualTo(source1.rpid)
        assertThat(sources[0].otherUI).isEqualTo(source1.otherUI)
        assertThat(sources[0].userHandle).isEqualTo(source1.userHandle)
        assertThat(sources[0].type).isEqualTo(source1.type)

    }

    @Test
    fun testPersistWithDifferentRpid() {
        repository.persist(source1)
        repository.persist(source2)
        repository.persist(source3)
        val sources = repository.getPublicKeyCredentialSource(source3.rpid)
        assertThat(sources).hasSize(1)
        assertThat(sources[0].id).isEqualTo(source3.id)
        assertThat(sources[0].rpid).isEqualTo(source3.rpid)
        assertThat(sources[0].otherUI).isEqualTo(source3.otherUI)
        assertThat(sources[0].userHandle).isEqualTo(source3.userHandle)
        assertThat(sources[0].type).isEqualTo(source3.type)
    }

    @Test
    fun testDelete() {
        repository.persist(source1)
        repository.persist(source2)
        repository.delete(source1)
        val sources = repository.getPublicKeyCredentialSource(source2.rpid)
        assertThat(sources).hasSize(1)
        assertThat(sources[0].id).isEqualTo(source2.id)
    }

    @Test
    fun testDeleteByRpId() {
        repository.persist(source1)
        repository.persist(source2)
        var sources = repository.getPublicKeyCredentialSource(source1.rpid)
        assertThat(sources).hasSize(2)
        repository.delete(source1.rpid)
        sources = repository.getPublicKeyCredentialSource(source1.rpid)
        assertThat(sources).isEmpty()

    }

    @Test
    fun testDeleteEmpty() {
        repository.delete(source1)
        val sources = repository.getPublicKeyCredentialSource(source2.rpid)
        assertThat(sources).hasSize(0)
    }

    @Test
    fun testDeleteEmptyAfter() {
        repository.persist(source1)
        repository.delete(source1)
        val sources = repository.getPublicKeyCredentialSource(source2.rpid)
        assertThat(sources).hasSize(0)
    }

    @Test
    fun testMigrate() {

        val array = JSONArray()
        array.put(source1.toJson()).put(source2.toJson()).put(source3.toJson())
        sharedPreferences.edit().putString(ALLOW_CREDENTIALS, array.toString()).commit()

        val newSP = context.getSharedPreferences("New_file", Context.MODE_PRIVATE )

        val repository: WebAuthnDataRepository = object: WebAuthnDataRepository(context, sharedPreferences) {
            override fun getNewSharedPreferences(): SharedPreferences {
                return newSP
            }
        }

        assertThat(newSP.getStringSet(source1.rpid, emptySet())!!.size).isEqualTo(2)
        assertThat(newSP.getStringSet(source3.rpid, emptySet())!!.size).isEqualTo(1)

        repository.persist(source4)
        assertThat(repository.getPublicKeyCredentialSource(source1.rpid).size).isEqualTo(3)

        assertThat(sharedPreferences.getString(ALLOW_CREDENTIALS, null)).isNull()

    }

}