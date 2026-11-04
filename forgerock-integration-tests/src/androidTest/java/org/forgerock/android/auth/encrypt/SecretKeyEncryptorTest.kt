/*
 * Copyright (c) 2024 - 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.encrypt

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import java.security.KeyStore.PrivateKeyEntry
import java.security.KeyStore.SecretKeyEntry
import kotlin.system.measureTimeMillis
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@RunWith(AndroidJUnit4::class)
@SmallTest
class SecretKeyEncryptorTest {

    private val alias = "keystore-key"
    private val appContext: Context by lazy { ApplicationProvider.getApplicationContext<Application>() }

    private val keyStore: KeyStore
        get() {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            return keyStore
        }

    @After
    fun tearDown() {
        keyStore.deleteEntry(alias)
    }

    @Test
    fun testEncryptWithAsymmetricKey() = runTest {
        val encryptor = SecretKeyEncryptor {
            context = appContext
            keyAlias = alias
            enforceAsymmetricKey = true
        }

        val encrypted = encryptor.encrypt("test".toByteArray())
        val decrypted = encryptor.decrypt(encrypted)
        assertEquals("test", decrypted.toString(Charsets.UTF_8))

        //Make sure the key is stored in the keystore as a private key
        assertTrue(keyStore.getEntry(alias, null) is PrivateKeyEntry)
    }

    @Test
    fun testEncryptWithSymmetricKey() = runTest {
        val encryptor = SecretKeyEncryptor {
            context = appContext
            keyAlias = alias
        }

        val encrypted = encryptor.encrypt("test".toByteArray())
        val decrypted = encryptor.decrypt(encrypted)
        assertEquals("test", decrypted.toString(Charsets.UTF_8))

        //Make sure the secret key is not generated
        assertTrue(keyStore.getEntry(alias, null) is SecretKeyEntry)
    }

    @Test
    fun testEncryptWithSymmetricKeyThenAsymmetric() = runTest {
        val encryptor = SecretKeyEncryptor {
            context = appContext
            keyAlias = alias
        }

        val encrypted = encryptor.encrypt("test".toByteArray())
        val decrypted = encryptor.decrypt(encrypted)
        assertEquals("test", decrypted.toString(Charsets.UTF_8))

        //using the same alias, now switch to asymmetric key
        val encryptor2 = SecretKeyEncryptor {
            context = appContext
            keyAlias = alias
            enforceAsymmetricKey = true
        }

        //Since it was encrypted with a symmetric key, keep using the symmetric key to decrypt
        val decrypted2 = encryptor2.decrypt(encrypted)
        assertEquals("test", decrypted2.toString(Charsets.UTF_8))

        encryptor2.encrypt("test".toByteArray())
        //The key is now stored as a private key after encrypt
        assertTrue(keyStore.getEntry(alias, null) is PrivateKeyEntry)
    }

    @Test
    fun test_performance() = runTest(timeout = 3.toDuration(DurationUnit.SECONDS)) {
        val runs = 10
        val encryptor = SecretKeyEncryptor {
            symmetricKeySize = 256
            keyAlias = alias
            context = appContext
            strongBoxPreferred = false
        }
        val dataStr =
            "{\"type\": 0, \"value\": \"[\\\"abcedadsascadsasdfakdalkd;dkj23400702erasldknapdfasdfa;sdkfa;dkjadfad\\\\a;dsklcna;sdlckna;dsfladkfa0234\\/adasdadssdfasd;adkcna;dsckna;dsc\\\\ad;kajsd;fakdfadfa\\\\n\\\"]\"}".toByteArray()

        var totalEncryptTime = 0L
        var totalDecryptTime = 0L

        repeat(runs) {
            var output: ByteArray
            val encryptTime = measureTimeMillis {
                output = encryptor.encrypt(dataStr)
            }
            val decryptTime = measureTimeMillis {
                output = encryptor.decrypt(output)
            }
            totalEncryptTime += encryptTime
            totalDecryptTime += decryptTime
            assertEquals(String(dataStr), String(output))
        }

        println("Average encryption time: ${totalEncryptTime / runs} ms")
        println("Average decryption time: ${totalDecryptTime / runs} ms")

    }

    /**
     * Reproduces the 4.8.0 key-generation path: StrongBox forced on whenever the device
     * advertises the feature (4.8.0 had no flag, so there was no way to turn this off).
     * Related ticket: SDKS-5549. On a device with an unreliable StrongBox implementation
     * this is where corruption originates. The key's security level is logged (and asserted
     * on API 31+ when StrongBox is advertised) so device logs answer the attribution
     * question: does THIS device advertise StrongBox, and did the key actually land in it?
     */
    @Test
    fun testStrongBoxBackedKeyGeneration() = runTest {
        val strongBoxFeature = appContext.packageManager
            .hasSystemFeature(android.content.pm.PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        println("device=${Build.MANUFACTURER} ${Build.MODEL} sdk=${Build.VERSION.SDK_INT} " +
                "strongBoxFeature=$strongBoxFeature")

        val strongBoxAlias = "strongbox-key"
        val encryptor = SecretKeyEncryptor {
            context = appContext
            keyAlias = strongBoxAlias
            strongBoxPreferred = true // 4.8.0 behaviour: StrongBox forced when available
        }

        // Round-trip through the 4.8.0 encryption pipeline.
        val payload = "attribution-roundtrip".toByteArray()
        val encrypted = encryptor.encrypt(payload)
        val decrypted = encryptor.decrypt(encrypted)
        assertEquals(String(payload), String(decrypted))

        // Where did the key end up? SECURITY_LEVEL_STRONGBOX proves StrongBox-backed;
        // SOFTWARE or TRUSTED_ENVIRONMENT means the device fell back or lacks the feature.
        val entry = keyStore.getEntry(strongBoxAlias, null) as SecretKeyEntry
        val factory = javax.crypto.SecretKeyFactory.getInstance(
            entry.secretKey.algorithm, "AndroidKeyStore")
        val keyInfo = factory.getKeySpec(entry.secretKey, android.security.keystore.KeyInfo::class.java)
            as android.security.keystore.KeyInfo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // KeyInfo.getSecurityLevel() requires API 31+; below that only
            // isInsideSecureHardware() exists.
            println("key securityLevel=${keyInfo.securityLevel} " +
                    "insideSecureHardware=${keyInfo.isInsideSecureHardware}")
            if (strongBoxFeature) {
                assertEquals(
                    "Device advertises StrongBox but key did not land in StrongBox",
                    android.security.keystore.KeyProperties.SECURITY_LEVEL_STRONGBOX,
                    keyInfo.securityLevel
                )
            }
        } else {
            println("key insideSecureHardware=${keyInfo.isInsideSecureHardware} " +
                    "(securityLevel requires API 31+, device sdk=${Build.VERSION.SDK_INT})")
        }
        keyStore.deleteEntry(strongBoxAlias)
    }

    /**
     * High-volume soak through the 4.8.0 crypto path (StrongBox forced on when the device
     * advertises it). Related ticket: SDKS-5549. The customer's corruption surfaced once in
     * production; catching an intermittent secure-hardware fault needs operation volume,
     * not app relaunches. Each iteration encrypts a unique payload and verifies the
     * round-trip, so a single faulty operation fails the test. On devices without StrongBox
     * this exercises the TEE/software fallback path instead - a control, not attribution.
     */
    @Test
    fun testStrongBoxSoak() = runTest(timeout = 5.toDuration(DurationUnit.MINUTES)) {
        val iterations = 300
        val soakAlias = "soak-key"
        val encryptor = SecretKeyEncryptor {
            context = appContext
            keyAlias = soakAlias
            strongBoxPreferred = true // 4.8.0 behaviour
        }

        val failures = StringBuilder()
        for (i in 1..iterations) {
            // Unique, realistic-size payload per iteration (~customer record size).
            val payload = ("soak-$i-" + "x".repeat(200) + "-$i").toByteArray()
            try {
                val decrypted = encryptor.decrypt(encryptor.encrypt(payload))
                if (!decrypted.contentEquals(payload)) {
                    failures.append("iter $i: round-trip mismatch\n")
                }
            } catch (e: Throwable) {
                failures.append("iter $i: ${e.javaClass.simpleName}: ${e.message}\n")
            }
        }
        assertEquals("Crypto failures during soak:\n$failures", 0, failures.length)
        keyStore.deleteEntry(soakAlias)
    }

}