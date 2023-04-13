/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.BiometricPrompt.AuthenticationResult
import com.nimbusds.jose.JWSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions
import org.forgerock.android.auth.CryptoKey
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.*

class DeviceBindAuthenticationTests {

    private val mockBiometricInterface = mock<BiometricHandler>()
    private val cryptoKey = mock<CryptoKey>()
    private val keyPair = mock<java.security.KeyPair>()
    private var context = mock<Context>()


    @Before
    fun setUp() {
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
        val output = testObject.sign(context, keyPair, "1234", "3123123123", "77888", getExpiration())
        assertNotNull(output)
        val jws = JWSObject.parse(output);
        assertEquals("1234", jws.header.keyID)
        assertEquals("3123123123", jws.payload.toJSONObject().get("sub"))
        assertNotNull(jws.payload.toJSONObject().get("exp"))
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
        val output = testObject.sign(context, keyPair, "1234", "3123123123", "77888", exp)
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

        testObject.generateKeys(context)

        verify(keyBuilder).setUserAuthenticationValidityDurationSeconds(30)
        verify(keyBuilder).setUserAuthenticationRequired(true)
        verify(cryptoKey).createKeyPair(keyBuilder.build())

        val testObjectBiometric = BiometricAndDeviceCredential()
        testObjectBiometric.setBiometricHandler(mockBiometricInterface)
        testObjectBiometric.setKey(cryptoKey)
        testObjectBiometric.isApi30OrAbove = false

        testObjectBiometric.generateKeys(context)

        verify(keyBuilder, times(2)).setUserAuthenticationValidityDurationSeconds(30)
        verify(keyBuilder, times(2)).setUserAuthenticationRequired(true)
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
        testObject.generateKeys(context)

        verify(keyBuilder).setUserAuthenticationParameters(30,
            KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
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
        testObject.generateKeys(context)

        verify(keyBuilder).setUserAuthenticationParameters(30, KeyProperties.AUTH_BIOMETRIC_STRONG)
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
        testObject.generateKeys(context)

        verify(cryptoKey).createKeyPair(keyBuilder.build())
    }

    @Test
    fun testIsNotSupported() {
        whenever(mockBiometricInterface.isSupported()).thenReturn(false)
        val testObject = BiometricAndDeviceCredential()
        testObject.setBiometricHandler(mockBiometricInterface)
        testObject.setKey(cryptoKey)
        testObject.isApi30OrAbove = false
        assertFalse(testObject.isSupported(context))
    }

    @Test
    fun testSupportedDeviceCred() {
        whenever(mockBiometricInterface.isSupported(BIOMETRIC_STRONG or DEVICE_CREDENTIAL,
            BIOMETRIC_WEAK or DEVICE_CREDENTIAL)).thenReturn(true)
        val testObject = BiometricAndDeviceCredential()
        testObject.setBiometricHandler(mockBiometricInterface)
        testObject.setKey(cryptoKey)
        testObject.isApi30OrAbove = false
        assertTrue(testObject.isSupported(context))
    }

    @Test
    fun testSupportedBiometricOnly() {
        whenever(mockBiometricInterface.isSupported()).thenReturn(true)
        val testObject = BiometricOnly()
        testObject.setBiometricHandler(mockBiometricInterface)
        testObject.setKey(cryptoKey)
        testObject.isApi30OrAbove = false
        assertTrue(testObject.isSupported(context))
    }

    @Test
    fun testSupportedNone() {
        val testObject = None()
        assertTrue(testObject.isSupported(context))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testAuthenticateForBiometric() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val privateKey = mock<PrivateKey>()
            val authenticationResult = mock<AuthenticationResult>()
            whenever(mockBiometricInterface.authenticate(any())).thenAnswer {
                (it.arguments[0] as AuthenticationCallback).onAuthenticationSucceeded(
                    authenticationResult)
            }
            whenever(cryptoKey.getPrivateKey()).thenReturn(privateKey)
            val testObject = BiometricOnly()
            testObject.setBiometricHandler(mockBiometricInterface)
            testObject.setKey(cryptoKey)
            testObject.isApi30OrAbove = false
            testObject.authenticate(context)
            verify(mockBiometricInterface).authenticate(any())
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
            whenever(mockBiometricInterface.authenticate(any())).thenAnswer {
                (it.arguments[0] as AuthenticationCallback).onAuthenticationSucceeded(
                    authenticationResult)
            }
            whenever(cryptoKey.getPrivateKey()).thenReturn(privateKey)
            val testObject = BiometricAndDeviceCredential()
            testObject.setBiometricHandler(mockBiometricInterface)
            testObject.setKey(cryptoKey)
            testObject.isApi30OrAbove = false
            testObject.authenticate(context)
            verify(mockBiometricInterface).authenticate(any())
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
            verify(mockBiometricInterface, never()).authenticate(any())
        } finally {
            Dispatchers.resetMain()
        }
    }

    fun getExpiration(): Date {
        val date = Calendar.getInstance();
        date.add(Calendar.SECOND, 60)
        return date.time;
    }


}