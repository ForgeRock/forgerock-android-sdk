/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.util.concurrent.CancellationException

/**
 * Tests for corrupted-record handling in [SecuredSharedPreferences], reported in
 * SDKS-5549: a record that decrypts successfully (valid MAC and GCM tag) but whose
 * plaintext is malformed JSON caused `RuntimeException` on every read, crash
 * looping the app until the app data was cleared. The SDK now discards the poisoned
 * entry on the confirmed second failure and returns null.
 *
 * Uses a fake [Encryptor] (AndroidKeyStore is unavailable under Robolectric) whose
 * framing lets each test place a record exactly on the failure path it targets:
 * decrypt-success-but-malformed-JSON, undecryptable, and cancellation.
 */
@RunWith(RobolectricTestRunner::class)
class SecuredSharedPreferencesCorruptionTest {

    private lateinit var context: Context
    private lateinit var raw: SharedPreferences
    private lateinit var prefs: SecuredSharedPreferences

    /** Reversible fake codec: MAGIC-prefixed framing, throws on anything else. */
    private val framingEncryptor = object : Encryptor {
        override fun encrypt(clearText: ByteArray): ByteArray = MAGIC + clearText

        override fun decrypt(encryptedData: ByteArray): ByteArray {
            if (encryptedData.size < MAGIC.size || !encryptedData.copyOfRange(0, MAGIC.size)
                    .contentEquals(MAGIC)
            ) {
                throw IllegalArgumentException("Not a framed blob")
            }
            return encryptedData.copyOfRange(MAGIC.size, encryptedData.size)
        }
    }

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        raw = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        raw.edit().clear().commit()
        prefs = SecuredSharedPreferences(context, PREFS_FILE, TEST_ALIAS, framingEncryptor)
    }

    @After
    fun tearDown() {
        raw.edit().clear().commit()
        File(context.filesDir.parent + "/shared_prefs/$PREFS_FILE.xml").delete()
    }

    /**
     * A poisoned record (decrypts cleanly, malformed JSON) is discarded and read as null
     * instead of throwing on every access. This is the SDKS-5549 crash loop.
     */
    @Test
    fun corruptRecordIsDiscardedAndReturnsNull() {
        raw.edit().putString(
            KEY, base64(framingEncryptor.encrypt("{not valid json".toByteArray(Charsets.UTF_8)))
        ).commit()

        assertNull(prefs.getString(KEY, null))
        // The poisoned entry is gone from the underlying storage, later reads stay clean.
        assertNull(raw.getString(KEY, null))
    }

    /** Only the unreadable key is discarded - other records survive. */
    @Test
    fun otherEntriesSurviveDiscard() {
        prefs.edit().putString("other", "kept").commit()
        raw.edit().putString(
            KEY, base64(framingEncryptor.encrypt("malformed".toByteArray(Charsets.UTF_8)))
        ).commit()

        assertNull(prefs.getString(KEY, null))
        assertEquals("kept", prefs.getString("other", null))
    }

    /** A well-formed record still round-trips through the same code path unchanged. */
    @Test
    fun wellFormedRecordStillReads() {
        prefs.edit().putString(KEY, "durable-value").commit()

        assertEquals("durable-value", prefs.getString(KEY, null))
    }

    /**
     * A record the encryptor cannot decrypt goes through the pre-existing decrypt-failure
     * self-heal (whole-file clear) and returns null - behaviour unchanged by this fix.
     */
    @Test
    fun undecryptableRecordStillSelfHeals() {
        prefs.edit().putString("other", "kept").commit()
        raw.edit().putString(KEY, base64(byteArrayOf(0x00, 0x01, 0x02))).commit()

        assertNull(prefs.getString(KEY, null))
        assertNull(prefs.getString("other", null))
    }

    /**
     * A CancellationException must propagate: cancelling a coroutine mid-read must not be
     * treated as corruption (no discard, no null).
     */
    @Test
    fun cancellationExceptionPropagates() {
        val cancelling = object : Encryptor {
            override fun encrypt(clearText: ByteArray): ByteArray = clearText
            override fun decrypt(encryptedData: ByteArray): ByteArray =
                throw CancellationException("coroutine cancelled")
        }
        val cancellingPrefs = SecuredSharedPreferences(context, PREFS_FILE, TEST_ALIAS, cancelling)
        cancellingPrefs.edit().putString(KEY, "value").commit()

        assertThrows(CancellationException::class.java) { cancellingPrefs.getString(KEY, null) }
        // The record was not discarded on cancellation: the raw storage still holds the
        // base64 of the encrypted {"type":0,"value":"value"} wrapper written by putString.
        assertTrue(!raw.getString(KEY, null).isNullOrEmpty())
    }

    private fun base64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.DEFAULT)

    companion object {
        private const val PREFS_FILE = "sdks5549_unit"
        private const val TEST_ALIAS = "SDKS5549UnitAlias"
        private const val KEY = "session_token"
        private val MAGIC = byteArrayOf(0x53, 0x44, 0x4B, 0x53) // "SDKS"
    }
}
