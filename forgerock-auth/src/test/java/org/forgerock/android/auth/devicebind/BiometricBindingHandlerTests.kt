/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import org.forgerock.android.auth.biometric.AuthenticatorType
import org.forgerock.android.auth.biometric.BiometricAuth
import org.forgerock.android.auth.biometric.BiometricAuthCompletionHandler
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*
import java.util.concurrent.CountDownLatch

class BiometricBindingHandlerTests {

    private val activity = mock<FragmentActivity>()
    private val biometricAuth = mock<BiometricAuth>()

    @Test
    fun testStrongTypeWithBiometricOnly() {
        whenever(biometricAuth.hasBiometricCapability(BiometricManager.Authenticators.BIOMETRIC_STRONG)).thenReturn(true)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ONLY, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupported())
        verify(biometricAuth).authenticatorType = AuthenticatorType.STRONG
    }

    @Test
    fun testWeakTypeWithBiometricOnly() {
        whenever(biometricAuth.hasBiometricCapability(BiometricManager.Authenticators.BIOMETRIC_WEAK)).thenReturn(true)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ONLY, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupported())
        verify(biometricAuth).authenticatorType = AuthenticatorType.WEAK
    }

    @Test
    fun testWeakTypeWithFingerPrint() {
        whenever(biometricAuth.hasEnrolledWithFingerPrint()).thenReturn(true)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ONLY, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupported())
        verify(biometricAuth).authenticatorType = AuthenticatorType.WEAK
    }

    @Test
    fun testFalseCaseForBiometricOnly() {
        whenever(biometricAuth.hasBiometricCapability(BiometricManager.Authenticators.BIOMETRIC_WEAK)).thenReturn(false)
        whenever(biometricAuth.hasBiometricCapability(BiometricManager.Authenticators.BIOMETRIC_STRONG)).thenReturn(false)
        whenever(biometricAuth.hasEnrolledWithFingerPrint()).thenReturn(false)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ONLY, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertFalse(testObject.isSupported())
        verify(biometricAuth, never()).authenticatorType = AuthenticatorType.WEAK
        verify(biometricAuth, never()).authenticatorType = AuthenticatorType.STRONG
    }

    @Test
    fun testSuccessCaseForBiometricOnlyWhenAnyOneisTrue() {
        whenever(biometricAuth.hasBiometricCapability(BiometricManager.Authenticators.BIOMETRIC_WEAK)).thenReturn(true)
        whenever(biometricAuth.hasBiometricCapability(BiometricManager.Authenticators.BIOMETRIC_STRONG)).thenReturn(false)
        whenever(biometricAuth.hasEnrolledWithFingerPrint()).thenReturn(false)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ONLY, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupported())
        verify(biometricAuth).authenticatorType = AuthenticatorType.WEAK
    }

    @Test
    fun testStrongTypeWithBiometricAndDeviceCredential() {
        whenever(biometricAuth.hasBiometricCapability(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)).thenReturn(true)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupported(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL, BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL))
        verify(biometricAuth).authenticatorType = AuthenticatorType.STRONG
    }

    @Test
    fun testWeakTypeWithBiometricAndDeviceCredential() {
        whenever(biometricAuth.hasBiometricCapability(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)).thenReturn(true)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupported(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL, BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL))
        verify(biometricAuth).authenticatorType = AuthenticatorType.WEAK
    }

    @Test
    fun testWeakTypeFingerPrintWithBiometricAndDeviceCredential() {
        whenever(biometricAuth.hasEnrolledWithFingerPrint()).thenReturn(true)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupported(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL, BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL))
        verify(biometricAuth).authenticatorType = AuthenticatorType.WEAK
    }

    @Test
    fun testSuccessCaseForBiometricAndDeviceCredentialWhenAnyOneisTrue() {
        whenever(biometricAuth.hasBiometricCapability(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)).thenReturn(true)
        whenever(biometricAuth.hasBiometricCapability(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)).thenReturn(false)
        whenever(biometricAuth.hasEnrolledWithFingerPrint()).thenReturn(false)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupported(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL, BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL))
        verify(biometricAuth).authenticatorType = AuthenticatorType.WEAK
    }

    @Test
    fun testKeyGuardManager() {
        whenever(biometricAuth.hasDeviceCredential()).thenReturn(true)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupported(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL, BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL))
        verify(biometricAuth, never()).authenticatorType = AuthenticatorType.WEAK
        verify(biometricAuth, never()).authenticatorType = AuthenticatorType.STRONG
    }

    @Test
    fun testListener() {
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        val result: (DeviceBindingStatus) -> Unit = {}
        testObject.authenticate(60, result)
        verify(biometricAuth).biometricAuthListener = testObject.biometricListener
        verify(biometricAuth).authenticate()
    }

    @Test
    fun testBiometricListenerForSuccess() {
        val latch = CountDownLatch(1)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)

        testObject.authenticate(timeout = 60) {
            assertEquals(it, Success)
            latch.countDown()
        }
        testObject.biometricListener?.onSuccess(null)
        latch.await()
    }


    @Test
    fun testBiometricListenerForTimeoutFailure() {
        val latch = CountDownLatch(1)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        testObject.authenticate(timeout = -100) {
            assertTrue(it is Timeout)
            assertEquals("Biometric Timeout", it.message)
            latch.countDown()
        }
        testObject.biometricListener?.onSuccess(null)
        latch.await()
    }

    @Test
    fun testBiometricListenerForWrongFingerPrintFailure() {
        val latch = CountDownLatch(1)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        testObject.authenticate(timeout = 60) {
            assertTrue(it is Abort)
            assertEquals("invalid credential", it.message)
            latch.countDown()
        }
        testObject.biometricListener?.onError(1, "invalid credential")
        latch.await()
    }
}