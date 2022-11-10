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
import androidx.test.core.app.ApplicationProvider
import com.nimbusds.jose.JWSObject
import org.assertj.core.api.Assertions
import org.forgerock.android.auth.CryptoKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.*
import java.util.concurrent.CountDownLatch

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
        val output = testObject.sign(keyPair, "1234", "3123123123", "77888", getExpiration())
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
        val output = testObject.sign(keyPair, "1234", "3123123123", "77888", exp)
        assertNotNull(output)
        val jws = JWSObject.parse(output);
        val actualExp = Calendar.getInstance();
        actualExp.time =
            Date.from(Instant.ofEpochSecond(jws.payload.toJSONObject().get("exp") as Long));
        Assertions.assertThat(actualExp.time).isEqualTo(exp);
    }

    @Test
    fun testGenerateKeys() {
        val keyGenParameterSpec = mock<KeyGenParameterSpec>()
        val keyBuilder = mock<KeyGenParameterSpec.Builder>()
        whenever(keyBuilder.build()).thenReturn(keyGenParameterSpec)
        whenever(cryptoKey.keyBuilder()).thenReturn(keyBuilder)
        whenever(cryptoKey.timeout).thenReturn(30)
        whenever(cryptoKey.createKeyPair(keyBuilder.build())).thenReturn(keyPair)

        val callback: (KeyPair) -> Unit = {}
        val testObject = BiometricAndDeviceCredential()
        testObject.setBiometricHandler(mockBiometricInterface)
        testObject.setKey(cryptoKey)
        testObject.isApi30OrAbove = false

        testObject.generateKeys(context, callback)

        verify(keyBuilder).setUserAuthenticationValidityDurationSeconds(30)
        verify(keyBuilder).setUserAuthenticationRequired(true)
        verify(cryptoKey).createKeyPair(keyBuilder.build())

        val testObjectBiometric = BiometricAndDeviceCredential()
        testObjectBiometric.setBiometricHandler(mockBiometricInterface)
        testObjectBiometric.setKey(cryptoKey)
        testObjectBiometric.isApi30OrAbove = false

        testObjectBiometric.generateKeys(context, callback)

        verify(keyBuilder, times(2)).setUserAuthenticationValidityDurationSeconds(30)
        verify(keyBuilder, times(2)).setUserAuthenticationRequired(true)
        verify(cryptoKey, times(2)).createKeyPair(keyBuilder.build())
    }

    @Test
    fun testGenerateKeysForAPi30ForDeviceCredential() {
        val keyGenParameterSpec = mock<KeyGenParameterSpec>()
        val keyBuilder = mock<KeyGenParameterSpec.Builder>()
        whenever(keyBuilder.build()).thenReturn(keyGenParameterSpec)
        whenever(cryptoKey.keyBuilder()).thenReturn(keyBuilder)
        whenever(cryptoKey.timeout).thenReturn(30)
        whenever(cryptoKey.createKeyPair(keyBuilder.build())).thenReturn(keyPair)

        val callback: (KeyPair) -> Unit = {}
        val testObject = BiometricAndDeviceCredential()
        testObject.setBiometricHandler(mockBiometricInterface)
        testObject.setKey(cryptoKey)
        testObject.isApi30OrAbove = true
        testObject.generateKeys(context, callback)

        verify(keyBuilder).setUserAuthenticationParameters(30,
            KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
        verify(keyBuilder).setUserAuthenticationRequired(true)
        verify(cryptoKey).createKeyPair(keyBuilder.build())
    }

    @Test
    fun testGenerateKeysForAPi30ForBiometricOnly() {
        val keyGenParameterSpec = mock<KeyGenParameterSpec>()
        val keyBuilder = mock<KeyGenParameterSpec.Builder>()
        whenever(keyBuilder.build()).thenReturn(keyGenParameterSpec)
        whenever(cryptoKey.keyBuilder()).thenReturn(keyBuilder)
        whenever(cryptoKey.timeout).thenReturn(30)
        whenever(cryptoKey.createKeyPair(keyBuilder.build())).thenReturn(keyPair)

        val callback: (KeyPair) -> Unit = {}
        val testObject = BiometricOnly()
        testObject.setBiometricHandler(mockBiometricInterface)
        testObject.setKey(cryptoKey)
        testObject.isApi30OrAbove = true
        testObject.generateKeys(context, callback)

        verify(keyBuilder).setUserAuthenticationParameters(30, KeyProperties.AUTH_BIOMETRIC_STRONG)
        verify(keyBuilder).setUserAuthenticationRequired(true)
        verify(cryptoKey).createKeyPair(keyBuilder.build())
    }

    @Test
    fun testGenerateKeysNone() {
        val keyGenParameterSpec = mock<KeyGenParameterSpec>()
        val keyBuilder = mock<KeyGenParameterSpec.Builder>()
        whenever(keyBuilder.build()).thenReturn(keyGenParameterSpec)
        whenever(cryptoKey.keyBuilder()).thenReturn(keyBuilder)
        whenever(cryptoKey.createKeyPair(keyBuilder.build())).thenReturn(keyPair)

        val callback: (KeyPair) -> Unit = {}
        val testObject = None()
        testObject.setKey(cryptoKey)
        testObject.generateKeys(context, callback)

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
        assertTrue(testObject.isSupported(context, ))
    }

    @Test
    fun testSupportedNone() {
        val testObject = None()
        assertTrue(testObject.isSupported(context))
    }

    @Test
    fun testAuthenticateForBiometric() {

        val result: (DeviceBindingStatus<PrivateKey>) -> (Unit) = {}
        val testObject = BiometricOnly()
        testObject.setBiometricHandler(mockBiometricInterface)
        testObject.setKey(cryptoKey)
        testObject.isApi30OrAbove = false
        testObject.authenticate(context, 60, result)
        verify(mockBiometricInterface).authenticate(any())
    }

    @Test
    fun testAuthenticateForBiometricAndCredential() {
        val result: (DeviceBindingStatus<PrivateKey>) -> (Unit) = {}
        val testObject = BiometricAndDeviceCredential()
        testObject.setBiometricHandler(mockBiometricInterface)
        testObject.setKey(cryptoKey)
        testObject.isApi30OrAbove = false

        testObject.authenticate(context, 60, result)
        verify(mockBiometricInterface).authenticate(any())
    }

    @Test
    fun testNoneAuthenticate() {
        val countDownLatch = CountDownLatch(1)
        val privateKey = mock<PrivateKey>()
        whenever(cryptoKey.getPrivateKey()).thenReturn(privateKey)
        val result: (DeviceBindingStatus<PrivateKey>) -> (Unit) = {
            assertEquals(it, Success(cryptoKey.getPrivateKey()))
            countDownLatch.countDown()
        }
        val testObject = None()
        testObject.setKey(cryptoKey)
        testObject.authenticate(context, 60, result)
        countDownLatch.await()
    }

    fun getExpiration(): Date {
        val date = Calendar.getInstance();
        date.add(Calendar.SECOND, 60)
        return date.time;
    }

}