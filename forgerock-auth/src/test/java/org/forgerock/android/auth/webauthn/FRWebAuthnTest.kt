/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.webauthn

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.*
import org.forgerock.android.auth.WebAuthnDataRepository
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test is only focus on the wrapper [FRWebAuthn], refer to WebAuthnDataRepositoryTest for
 * the actual test.
 */
@RunWith(AndroidJUnit4::class)
class FRWebAuthnTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var sharedPreferences = context.getSharedPreferences("Test", Context.MODE_PRIVATE)
    private var repository: WebAuthnDataRepository =
        object : WebAuthnDataRepository(context, sharedPreferences) {
            override fun getNewSharedPreferences(): SharedPreferences {
                return sharedPreferences
            }
        }
    private lateinit var source1: PublicKeyCredentialSource
    private lateinit var source2: PublicKeyCredentialSource
    private lateinit var frWebAuthn: FRWebAuthn

    @Before
    fun prepare() {
        frWebAuthn = FRWebAuthn(context, repository)
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
    }

    @After
    fun clearupData() {
        sharedPreferences.edit().clear().apply()
    }

    @Test
    fun deleteCredentialsByRpId() {
        repository.persist(source1)
        frWebAuthn.deleteCredentials(source1.rpid)
        assertThat(frWebAuthn.loadAllCredentials(source1.rpid)).isEmpty()
    }

    @Test
    fun testDeleteCredentials() {
        repository.persist(source1)
        repository.persist(source2)
        frWebAuthn.deleteCredentials(source1)
        val sources = frWebAuthn.loadAllCredentials(source1.rpid)
        assertThat(sources).hasSize(1)
    }

    @Test
    fun loadAllCredentials() {
        repository.persist(source1)
        repository.persist(source2)
        val sources = frWebAuthn.loadAllCredentials(source1.rpid)
        assertThat(sources).hasSize(2)
    }
}