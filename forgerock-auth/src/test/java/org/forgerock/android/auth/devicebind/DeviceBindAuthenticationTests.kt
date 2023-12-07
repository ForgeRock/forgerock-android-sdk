/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.BiometricPrompt.AuthenticationResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nimbusds.jose.JWSObject
import com.nimbusds.jwt.JWTClaimNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions
import org.assertj.core.data.Offset
import org.forgerock.android.auth.CryptoKey
import org.forgerock.android.auth.FRLogger
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.Logger.Companion.set
import org.forgerock.android.auth.callback.Attestation
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus.ClientNotRegistered
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.shadows.ShadowLog
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Calendar
import java.util.Date

class DeviceBindAuthenticationTests {

    private val mockBiometricInterface = mock<BiometricHandler>()
    private val cryptoKey = mock<CryptoKey>()
    private val keyPair = mock<java.security.KeyPair>()
    private var context = mock<Context>()


    @Before
    fun setUp() {
        Logger.setCustomLogger(object : FRLogger {
            override fun error(tag: String?, t: Throwable?, message: String?, vararg values: Any?) {
            }

            override fun error(tag: String?, message: String?, vararg values: Any?) {
            }

            override fun warn(tag: String?, message: String?, vararg values: Any?) {
            }

            override fun warn(tag: String?, t: Throwable?, message: String?, vararg values: Any?) {
            }

            override fun debug(tag: String?, message: String?, vararg values: Any?) {
            }

            override fun info(tag: String?, message: String?, vararg values: Any?) {
            }

            override fun network(tag: String?, message: String?, vararg values: Any?) {
            }
        })
        val publicKey = mock<RSAPublicKey>()
        val privateKey = mock<PrivateKey>()
        whenever(keyPair.public).thenReturn(publicKey)
        whenever(keyPair.private).thenReturn(privateKey)
        whenever(cryptoKey.keyAlias).thenReturn("jeyAlias")
    }

    @Test
    fun testSigningData() {
        val testObject = BiometricAndDeviceCredential()
        testObject.setBiometricHandler(mockBiometricInterface)
        testObject.setKey(cryptoKey)
        testObject.isApi30OrAbove = false
        val kpg: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keys = kpg.generateKeyPair()
        val keyPair = KeyPair(keys.public as RSAPublicKey, keys.private, "jeyAlias")
        val output =
            testObject.sign(context, keyPair, null, "1234", "3123123123", "77888", getExpiration())
        assertNotNull(output)
        val jws = JWSObject.parse(output);
        assertEquals("1234", jws.header.keyID)
        assertEquals("3123123123", jws.payload.toJSONObject()["sub"])
        Assertions.assertThat(jws.payload.toJSONObject()["nbf"] as Long)
            .isCloseTo(Date().time / 1000L, Offset.offset(10))
        Assertions.assertThat(jws.payload.toJSONObject()["iat"] as Long)
            .isCloseTo(Date().time / 1000L, Offset.offset(10))

        assertNotNull(jws.payload.toJSONObject()["exp"])
    }

    @Test
    fun testSigningDataWithSignature() {
        val testObject = BiometricOnly()
        testObject.setBiometricHandler(mockBiometricInterface)
        testObject.setKey(cryptoKey)
        testObject.isApi30OrAbove = false
        val kpg: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keys = kpg.generateKeyPair()
        val keyPair = KeyPair(keys.public as RSAPublicKey, keys.private, "jeyAlias")
        val output =
            testObject.sign(context,
                keyPair,
                testObject.getSignature(keyPair.privateKey),
                "1234",
                "3123123123",
                "77888",
                getExpiration())
        assertNotNull(output)
        val jws = JWSObject.parse(output);
        assertEquals("1234", jws.header.keyID)
        assertEquals("3123123123", jws.payload.toJSONObject()["sub"])
        Assertions.assertThat(jws.payload.toJSONObject()["nbf"] as Long)
            .isCloseTo(Date().time / 1000L, Offset.offset(10))
        Assertions.assertThat(jws.payload.toJSONObject()["iat"] as Long)
            .isCloseTo(Date().time / 1000L, Offset.offset(10))

        assertNotNull(jws.payload.toJSONObject()["exp"])
    }

    @Test
    fun testSigningDataWithPrivateKey() {
        val testObject = BiometricAndDeviceCredential()
        testObject.setBiometricHandler(mockBiometricInterface)
        testObject.setKey(cryptoKey)
        testObject.isApi30OrAbove = false
        val kpg: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keys = kpg.generateKeyPair()
        val userKey = UserKey("id", "3123123123", "username", "1234",
            DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)
        val output = testObject.sign(context, userKey, keys.private, null, "77888", getExpiration())
        assertNotNull(output)
        val jws = JWSObject.parse(output);
        assertEquals("1234", jws.header.keyID)
        assertEquals("3123123123", jws.payload.toJSONObject()["sub"])
        Assertions.assertThat(jws.payload.toJSONObject()["nbf"] as Long)
            .isCloseTo(Date().time / 1000L, Offset.offset(10))
        Assertions.assertThat(jws.payload.toJSONObject()["iat"] as Long)
            .isCloseTo(Date().time / 1000L, Offset.offset(10))

        assertNotNull(jws.payload.toJSONObject()["exp"])
    }

    @Test
    fun testSigningDataVerifierWithSignature() {
        val testObject = BiometricOnly()
        testObject.setBiometricHandler(mockBiometricInterface)
        testObject.setKey(cryptoKey)
        testObject.isApi30OrAbove = false
        val kpg: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keys = kpg.generateKeyPair()
        val userKey = UserKey("id", "3123123123", "username", "1234",
            DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)
        val output = testObject.sign(context,
            userKey,
            keys.private,
            testObject.getSignature(keys.private),
            "77888",
            getExpiration())
        assertNotNull(output)
        val jws = JWSObject.parse(output);
        assertEquals("1234", jws.header.keyID)
        assertEquals("3123123123", jws.payload.toJSONObject()["sub"])
        Assertions.assertThat(jws.payload.toJSONObject()["nbf"] as Long)
            .isCloseTo(Date().time / 1000L, Offset.offset(10))
        Assertions.assertThat(jws.payload.toJSONObject()["iat"] as Long)
            .isCloseTo(Date().time / 1000L, Offset.offset(10))

        assertNotNull(jws.payload.toJSONObject()["exp"])
    }

    @Test
    fun testSigningWithExpiration() {
        val testObject = BiometricAndDeviceCredential()
        testObject.setBiometricHandler(mockBiometricInterface)
        testObject.setKey(cryptoKey)
        testObject.isApi30OrAbove = false
        val kpg: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keys = kpg.generateKeyPair()
        val keyPair = KeyPair(keys.public as RSAPublicKey, keys.private, "jeyAlias")
        val expectedExp = Calendar.getInstance();
        expectedExp.add(Calendar.SECOND, 10);
        val exp = Date.from(Instant.ofEpochSecond(expectedExp.time.time / 1000));
        val output = testObject.sign(context, keyPair, null, "1234", "3123123123", "77888", exp)
        assertNotNull(output)
        val jws = JWSObject.parse(output);
        val actualExp = Calendar.getInstance();
        actualExp.time =
            Date.from(Instant.ofEpochSecond(jws.payload.toJSONObject().get("exp") as Long));
        Assertions.assertThat(actualExp.time).isEqualTo(exp);
    }

    @Test
    fun testGenerateKeys(): Unit = runBlocking {
        val keyGenParameterSpec = mock<KeyGenParameterSpec>()
        val keyBuilder = mock<KeyGenParameterSpec.Builder>()
        whenever(keyBuilder.build()).thenReturn(keyGenParameterSpec)
        whenever(cryptoKey.keyBuilder()).thenReturn(keyBuilder)
        whenever(cryptoKey.timeout).thenReturn(30)
        whenever(cryptoKey.createKeyPair(keyBuilder.build())).thenReturn(keyPair)

        val testObject = BiometricAndDeviceCredential()
        testObject.setBiometricHandler(mockBiometricInterface)
        testObject.setKey(cryptoKey)
        testObject.isApi30OrAbove = false

        testObject.generateKeys(context, Attestation.None)

        verify(keyBuilder).setUserAuthenticationRequired(true)
        verify(cryptoKey).createKeyPair(keyBuilder.build())

        val testObjectBiometric = BiometricAndDeviceCredential()
        testObjectBiometric.setBiometricHandler(mockBiometricInterface)
        testObjectBiometric.setKey(cryptoKey)
        testObjectBiometric.isApi30OrAbove = false

        testObjectBiometric.generateKeys(context, Attestation.None)

        verify(cryptoKey, times(2)).createKeyPair(keyBuilder.build())
    }

    @Test
    fun testGenerateKeysForAPi30ForDeviceCredential(): Unit = runBlocking {
        val keyGenParameterSpec = mock<KeyGenParameterSpec>()
        val keyBuilder = mock<KeyGenParameterSpec.Builder>()
        whenever(keyBuilder.build()).thenReturn(keyGenParameterSpec)
        whenever(cryptoKey.keyBuilder()).thenReturn(keyBuilder)
        whenever(cryptoKey.timeout).thenReturn(30)
        whenever(cryptoKey.createKeyPair(keyBuilder.build())).thenReturn(keyPair)

        val testObject = BiometricAndDeviceCredential()
        testObject.setBiometricHandler(mockBiometricInterface)
        testObject.setKey(cryptoKey)
        testObject.isApi30OrAbove = true
        testObject.generateKeys(context, Attestation.None)

        verify(keyBuilder).setUserAuthenticationRequired(true)
        verify(cryptoKey).createKeyPair(keyBuilder.build())
    }

    @Test
    fun testGenerateKeysForAPi30ForBiometricOnly(): Unit = runBlocking {
        val keyGenParameterSpec = mock<KeyGenParameterSpec>()
        val keyBuilder = mock<KeyGenParameterSpec.Builder>()
        whenever(keyBuilder.build()).thenReturn(keyGenParameterSpec)
        whenever(cryptoKey.keyBuilder()).thenReturn(keyBuilder)
        whenever(cryptoKey.timeout).thenReturn(30)
        whenever(cryptoKey.createKeyPair(keyBuilder.build())).thenReturn(keyPair)

        val testObject = BiometricOnly()
        testObject.setBiometricHandler(mockBiometricInterface)
        testObject.setKey(cryptoKey)
        testObject.isApi30OrAbove = true
        testObject.generateKeys(context, Attestation.None)

        verify(keyBuilder).setUserAuthenticationRequired(true)
        verify(cryptoKey).createKeyPair(keyBuilder.build())
    }

    @Test
    fun testGenerateKeysNone(): Unit = runBlocking {
        val keyGenParameterSpec = mock<KeyGenParameterSpec>()
        val keyBuilder = mock<KeyGenParameterSpec.Builder>()
        whenever(keyBuilder.build()).thenReturn(keyGenParameterSpec)
        whenever(cryptoKey.keyBuilder()).thenReturn(keyBuilder)
        whenever(cryptoKey.createKeyPair(keyBuilder.build())).thenReturn(keyPair)

        val testObject = None()
        testObject.setKey(cryptoKey)
        testObject.generateKeys(context, Attestation.None)

        verify(cryptoKey).createKeyPair(keyBuilder.build())
    }

    @Test
    fun testIsNotSupported() {
        whenever(mockBiometricInterface.isSupported()).thenReturn(false)
        val testObject = BiometricAndDeviceCredential()
        testObject.setBiometricHandler(mockBiometricInterface)
        testObject.setKey(cryptoKey)
        testObject.isApi30OrAbove = false
        assertFalse(testObject.isSupported(context, Attestation.None))
    }

    @Test
    fun testSupportedDeviceCred() {
        whenever(mockBiometricInterface.isSupported(BIOMETRIC_STRONG or DEVICE_CREDENTIAL,
            BIOMETRIC_WEAK or DEVICE_CREDENTIAL)).thenReturn(true)
        val testObject = BiometricAndDeviceCredential()
        testObject.setBiometricHandler(mockBiometricInterface)
        testObject.setKey(cryptoKey)
        testObject.isApi30OrAbove = false
        assertTrue(testObject.isSupported(context, Attestation.None))
    }

    @Test
    fun testSupportedBiometricOnly() {
        whenever(mockBiometricInterface.isSupported()).thenReturn(true)
        val testObject = BiometricOnly()
        testObject.setBiometricHandler(mockBiometricInterface)
        testObject.setKey(cryptoKey)
        testObject.isApi30OrAbove = false
        assertTrue(testObject.isSupported(context, Attestation.None))
    }

    @Test
    fun testSupportedNone() {
        val testObject = None()
        assertTrue(testObject.isSupported(context, Attestation.None))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testAuthenticateForBiometric() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val privateKey = mock<PrivateKey>()
            val authenticationResult = mock<AuthenticationResult>()
            whenever(authenticationResult.cryptoObject).thenReturn(null)
            var result = false
            val biometricHandler = object : BiometricHandler {
                override fun isSupported(strongAuthenticators: Int,
                                         weakAuthenticators: Int): Boolean {
                    return true
                }

                override fun isSupportedBiometricStrong(): Boolean {
                    return true
                }

                override fun authenticate(authenticationCallback: AuthenticationCallback,
                                          cryptoObject: BiometricPrompt.CryptoObject?) {
                    result = true
                    authenticationCallback.onAuthenticationSucceeded(authenticationResult)
                }
            }

            whenever(cryptoKey.getPrivateKey()).thenReturn(privateKey)
            val testObject = BiometricOnly()
            testObject.setBiometricHandler(biometricHandler)
            testObject.setKey(cryptoKey)
            testObject.isApi30OrAbove = false
            testObject.authenticate(context)
            assertTrue(result)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testAuthenticateForBiometricAndCredential() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val privateKey = mock<PrivateKey>()
            val authenticationResult = mock<AuthenticationResult>()
            whenever(authenticationResult.cryptoObject).thenReturn(null)
            var result = false
            val biometricHandler = object : BiometricHandler {
                override fun isSupported(strongAuthenticators: Int,
                                         weakAuthenticators: Int): Boolean {
                    return true
                }

                override fun isSupportedBiometricStrong(): Boolean {
                    return true
                }

                override fun authenticate(authenticationCallback: AuthenticationCallback,
                                          cryptoObject: BiometricPrompt.CryptoObject?) {
                    result = true
                    authenticationCallback.onAuthenticationSucceeded(authenticationResult)
                }
            }
            whenever(cryptoKey.getPrivateKey()).thenReturn(privateKey)
            val testObject = BiometricAndDeviceCredential()
            testObject.setBiometricHandler(biometricHandler)
            testObject.setKey(cryptoKey)
            testObject.isApi30OrAbove = false
            testObject.authenticate(context)
            assertTrue(result)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun testNoneAuthenticate() = runBlocking {
        val privateKey = mock<PrivateKey>()
        whenever(cryptoKey.getPrivateKey()).thenReturn(privateKey)
        val testObject = None()
        testObject.setKey(cryptoKey)
        assertEquals(testObject.authenticate(context), Success(cryptoKey.getPrivateKey()!!))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testDoNotInvokeBiometricWhenThePrivateKeyIsNull() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            whenever(cryptoKey.getPrivateKey()).thenReturn(null)
            val testObject = BiometricAndDeviceCredential()
            testObject.setBiometricHandler(mockBiometricInterface)
            testObject.setKey(cryptoKey)
            testObject.isApi30OrAbove = false
            assertEquals(ClientNotRegistered(), testObject.authenticate(context))
            verify(mockBiometricInterface, never()).authenticate(any(), any())
        } finally {
            Dispatchers.resetMain()
        }
    }

    fun getExpiration(): Date {
        val date = Calendar.getInstance();
        date.add(Calendar.SECOND, 60)
        return date.time;
    }

    @Test
    fun testValidateCustomClaimsForValidClaims() {
        val testObject = None()
        assertTrue(testObject.validateCustomClaims(customClaims = mapOf("name" to "demo", "email_verified" to true)))
    }

    @Test
    fun testValidateCustomClaimsForInvalidClaims() {
        val testObject = None()

        assertFalse(testObject.validateCustomClaims(mapOf(JWTClaimNames.SUBJECT to "demo")))
        assertFalse(testObject.validateCustomClaims(mapOf(JWTClaimNames.EXPIRATION_TIME to "demo")))
        assertFalse(testObject.validateCustomClaims(mapOf(JWTClaimNames.ISSUED_AT to "demo")))
        assertFalse(testObject.validateCustomClaims(mapOf(JWTClaimNames.NOT_BEFORE to "demo")))
        assertFalse(testObject.validateCustomClaims(mapOf(JWTClaimNames.ISSUER to "demo")))
        assertFalse(testObject.validateCustomClaims(mapOf("challenge" to "demo")))
        assertFalse(testObject.validateCustomClaims(mapOf(JWTClaimNames.ISSUER to "demo", JWTClaimNames.EXPIRATION_TIME to Date())))
    }

    @Test
    fun testValidateCustomClaimsForEmptyClaims() {
        val testObject = None()
        assertTrue(testObject.validateCustomClaims(emptyMap()))
    }

}