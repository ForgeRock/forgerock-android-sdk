/*
 * Copyright (c) 2023 - 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.webauthn

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.*
import org.forgerock.android.auth.RemoteWebAuthnRepository
import org.forgerock.android.auth.WebAuthnDataRepository
import org.forgerock.android.auth.exception.ApiException
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * This test is only focus on the wrapper [FRWebAuthn], refer to WebAuthnDataRepositoryTest for
 * the actual test.
 */
@RunWith(AndroidJUnit4::class)
class FRWebAuthnTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val remoteWebAuthnRepository = mock<RemoteWebAuthnRepository>()
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
        frWebAuthn = FRWebAuthn(context, repository, remoteWebAuthnRepository)
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
    fun testDeleteCredentialsByRpId() {
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
    fun testLoadAllCredentials() {
        repository.persist(source1)
        repository.persist(source2)
        val sources = frWebAuthn.loadAllCredentials(source1.rpid)
        assertThat(sources).hasSize(2)
    }

    @Test
    fun testDeleteCredentialByPublicKeyCredentialSource() {
        val localKey = PublicKeyCredentialSource(
            "NbbX-JfFRKW00lCMEK0fKw".toByteArray(),
            PublicKeyCredentialType.PUBLIC_KEY.toString(),
            "rpid1",
            "user1".toByteArray(),
            "User One",
            1715204825651
        )

        repository.persist(localKey)

        frWebAuthn.deleteCredentials(localKey)
        assertThat(frWebAuthn.loadAllCredentials(localKey.rpid)).isEmpty()
    }

    @Test
    fun testDeleteCredentialByPublicKeyCredentialSourceNotForceDelete() : Unit =
        runBlocking {
            given(remoteWebAuthnRepository.delete(any())).willAnswer { throw ApiException(403,
                "Failed",
                "Failed") }
            val localKey = PublicKeyCredentialSource(
                "NbbX-JfFRKW00lCMEK0fKw".toByteArray(),
                PublicKeyCredentialType.PUBLIC_KEY.toString(),
                "rpid1",
                "user1".toByteArray(),
                "User One",
                1715204825651
            )

            repository.persist(localKey)

            try {
                frWebAuthn.deleteCredentials(localKey, false)
            } catch (e: Exception) {
                //ignore
            }

            verify(remoteWebAuthnRepository).delete(localKey)
            assertThat(frWebAuthn.loadAllCredentials(localKey.rpid)).isNotEmpty
    }

    @Test
    fun testDeleteCredentialByPublicKeyCredentialSourceForceDelete() : Unit =
        runBlocking {
            given(remoteWebAuthnRepository.delete(any())).willAnswer { throw ApiException(403,
                "Failed",
                "Failed") }

            val localKey = PublicKeyCredentialSource(
                "NbbX-JfFRKW00lCMEK0fKw".toByteArray(),
                PublicKeyCredentialType.PUBLIC_KEY.toString(),
                "rpid2",
                "user2".toByteArray(),
                "User Two",
                1715204825651
            )

            repository.persist(localKey)

            try {
                frWebAuthn.deleteCredentials(localKey, true)
            } catch (e: Exception) {
                //ignore
            }

            verify(remoteWebAuthnRepository).delete(localKey)
            assertThat(frWebAuthn.loadAllCredentials(localKey.rpid)).isEmpty()
        }

}