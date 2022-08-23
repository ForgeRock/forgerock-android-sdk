/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.forgerock.android.auth.biometric.BiometricAuthCompletionHandler
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.CountDownLatch

class DeviceBindAuthenticationTests {

    private val mockBiometricInterface = mock<BiometricInterface>()
    private val keyAware = mock<KeyAware>()

    @Test
    fun testSigningData() {
       val testObject = BiometricAndDeviceCredential(mockBiometricInterface, keyAware)
        val kpg: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keys = kpg.generateKeyPair()
        val keyPair = KeyPair(keys.public as RSAPublicKey, keys.private, "jeyAlias")
        val output = testObject.sign(keyPair, "1234", "3123123123", "77888")
        assertNotNull(output)
    }

    @Test
    fun testGenerateKeys() {
        val keyBuilder = mock<KeyGenParameterSpec.Builder>()
        whenever(keyAware.keyBuilder()).thenReturn(keyBuilder)
        whenever(keyAware.timeout).thenReturn(30)

        val testObject = BiometricAndDeviceCredential(mockBiometricInterface, keyAware)
        testObject.generateKeys()

        verify(keyBuilder).setUserAuthenticationValidityDurationSeconds(30)
        verify(keyBuilder).setUserAuthenticationRequired(true)
        verify(keyAware).createKeyPair(keyBuilder)

        val testObjectBiometric = BiometricOnly(mockBiometricInterface, keyAware)
        testObjectBiometric.generateKeys()

        verify(keyBuilder, times(2)).setUserAuthenticationValidityDurationSeconds(30)
        verify(keyBuilder, times(2)).setUserAuthenticationRequired(true)
        verify(keyAware, times(2)).createKeyPair(keyBuilder)
    }

    @Test
    fun testGenerateKeysForAPi30ForDeviceCredential() {
        val keyBuilder = mock<KeyGenParameterSpec.Builder>()
        whenever(keyAware.keyBuilder()).thenReturn(keyBuilder)
        whenever(keyAware.timeout).thenReturn(30)
        whenever(mockBiometricInterface.isApi30AndAbove()).thenReturn(true)

        val testObject = BiometricAndDeviceCredential(mockBiometricInterface, keyAware)
        testObject.generateKeys()

        verify(keyBuilder).setUserAuthenticationParameters(30, KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
        verify(keyBuilder).setUserAuthenticationRequired(true)
        verify(keyAware).createKeyPair(keyBuilder)
    }

    @Test
    fun testGenerateKeysForAPi30ForBiometricOnly() {
        val keyBuilder = mock<KeyGenParameterSpec.Builder>()
        whenever(keyAware.keyBuilder()).thenReturn(keyBuilder)
        whenever(keyAware.timeout).thenReturn(30)
        whenever(mockBiometricInterface.isApi30AndAbove()).thenReturn(true)

        val testObject = BiometricOnly(mockBiometricInterface, keyAware)
        testObject.generateKeys()

        verify(keyBuilder).setUserAuthenticationParameters(30, KeyProperties.AUTH_BIOMETRIC_STRONG)
        verify(keyBuilder).setUserAuthenticationRequired(true)
        verify(keyAware).createKeyPair(keyBuilder)
    }

    @Test
    fun testGenerateKeysNone() {
        val keyBuilder = mock<KeyGenParameterSpec.Builder>()
        whenever(keyAware.keyBuilder()).thenReturn(keyBuilder)

        val testObject = None(keyAware)
        testObject.generateKeys()

        verify(keyAware).createKeyPair(keyBuilder)
    }

    @Test
    fun testIsNotSupported() {
        whenever(mockBiometricInterface.isSupportedForBiometricOnly()).thenReturn(false)
        val testObject = BiometricAndDeviceCredential(mockBiometricInterface, keyAware)
        assertFalse(testObject.isSupported())
    }

    @Test
    fun testSupportedDeviceCred() {
        whenever(mockBiometricInterface.isSupportedForBiometricAndDeviceCredential()).thenReturn(true)
        val testObject = BiometricAndDeviceCredential(mockBiometricInterface, keyAware)
        assertTrue(testObject.isSupported())
    }

    @Test
    fun testSupportedBiometricOnly() {
        whenever(mockBiometricInterface.isSupportedForBiometricOnly()).thenReturn(true)
        val testObject = BiometricOnly(mockBiometricInterface, keyAware)
        assertTrue(testObject.isSupported())
    }

    @Test
    fun testSupportedNone() {
        val testObject = None(keyAware)
        assertTrue(testObject.isSupported())
    }

    @Test
    fun testAuthenticateForBiometric() {

        val result: (DeviceBindingStatus) -> (Unit) = {}
        val listener = mock<BiometricAuthCompletionHandler>()
        whenever(mockBiometricInterface.getBiometricListener(60, result)).thenReturn(listener)

        val testObject = BiometricOnly(mockBiometricInterface, keyAware)
        testObject.authenticate(60, result)

        verify(mockBiometricInterface).getBiometricListener(60, result)
        verify(mockBiometricInterface).setListener(listener)
        verify(mockBiometricInterface).authenticate()
    }

    @Test
    fun testAuthenticateForBiometricAndCredential() {

        val result: (DeviceBindingStatus) -> (Unit) = {}
        val listener = mock<BiometricAuthCompletionHandler>()
        whenever(mockBiometricInterface.getBiometricListener(60, result)).thenReturn(listener)

        val testObject = BiometricAndDeviceCredential(mockBiometricInterface, keyAware)
        testObject.authenticate(60, result)

        verify(mockBiometricInterface).getBiometricListener(60, result)
        verify(mockBiometricInterface).setListener(listener)
        verify(mockBiometricInterface).authenticate()
    }

    @Test
    fun testNoneAuthenticate() {
        val countDownLatch = CountDownLatch(1)
        val result: (DeviceBindingStatus) -> (Unit) = {
            assertEquals(it, Success)
            countDownLatch.countDown()
        }
        val testObject = None(keyAware)
        testObject.authenticate(60, result)
        countDownLatch.await()
    }

}