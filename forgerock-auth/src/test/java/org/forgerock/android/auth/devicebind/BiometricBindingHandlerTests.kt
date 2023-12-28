/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.BiometricPrompt.AuthenticationResult
import androidx.biometric.BiometricPrompt.ERROR_TIMEOUT
import androidx.fragment.app.FragmentActivity
import org.forgerock.android.auth.biometric.BiometricAuth
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch

class BiometricBindingHandlerTests {

    private val activity = mock<FragmentActivity>()
    private val biometricAuth = mock<BiometricAuth>()

    @Test
    fun testStrongTypeWithBiometricOnly() {
        whenever(biometricAuth.hasBiometricCapability(BIOMETRIC_STRONG)).thenReturn(true)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ONLY, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupported())
    }

    @Test
    fun testWeakTypeWithBiometricOnly() {
        whenever(biometricAuth.hasBiometricCapability(BIOMETRIC_STRONG)).thenReturn(false)
        whenever(biometricAuth.hasBiometricCapability(BIOMETRIC_WEAK)).thenReturn(true)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ONLY, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupported())
    }

    @Test
    fun testWeakTypeWithFingerPrint() {
        whenever(biometricAuth.hasBiometricCapability(BIOMETRIC_STRONG)).thenReturn(false)
        whenever(biometricAuth.hasBiometricCapability(BIOMETRIC_WEAK)).thenReturn(false)
        whenever(biometricAuth.hasEnrolledWithFingerPrint()).thenReturn(true)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ONLY, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupported())
    }

    @Test
    fun testFalseCaseForBiometricOnly() {
        whenever(biometricAuth.hasBiometricCapability(BIOMETRIC_WEAK)).thenReturn(false)
        whenever(biometricAuth.hasBiometricCapability(BIOMETRIC_STRONG)).thenReturn(false)
        whenever(biometricAuth.hasEnrolledWithFingerPrint()).thenReturn(false)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ONLY, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertFalse(testObject.isSupported())
    }

    @Test
    fun testSuccessCaseForBiometricOnlyWhenAnyOneisTrue() {
        whenever(biometricAuth.hasBiometricCapability(BIOMETRIC_WEAK)).thenReturn(true)
        whenever(biometricAuth.hasBiometricCapability(BIOMETRIC_STRONG)).thenReturn(false)
        whenever(biometricAuth.hasEnrolledWithFingerPrint()).thenReturn(false)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ONLY, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupported())
    }

    @Test
    fun testStrongTypeWithBiometricAndDeviceCredential() {
        whenever(biometricAuth.hasBiometricCapability(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)).thenReturn(true)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupported(BIOMETRIC_STRONG or DEVICE_CREDENTIAL, BIOMETRIC_WEAK or DEVICE_CREDENTIAL))
    }

    @Test
    fun testWeakTypeWithBiometricAndDeviceCredential() {
        whenever(biometricAuth.hasBiometricCapability(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)).thenReturn(true)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupported(BIOMETRIC_STRONG or DEVICE_CREDENTIAL, BIOMETRIC_WEAK or DEVICE_CREDENTIAL))
    }

    @Test
    fun testWeakTypeFingerPrintWithBiometricAndDeviceCredential() {
        whenever(biometricAuth.hasEnrolledWithFingerPrint()).thenReturn(true)
        whenever(biometricAuth.hasBiometricCapability(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)).thenReturn(false)
        whenever(biometricAuth.hasBiometricCapability(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)).thenReturn(false)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupported(BIOMETRIC_STRONG or DEVICE_CREDENTIAL, BIOMETRIC_WEAK or DEVICE_CREDENTIAL))
    }

    @Test
    fun testSuccessCaseForBiometricAndDeviceCredentialWhenAnyOneisTrue() {
        whenever(biometricAuth.hasBiometricCapability(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)).thenReturn(true)
        whenever(biometricAuth.hasBiometricCapability(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)).thenReturn(false)
        whenever(biometricAuth.hasEnrolledWithFingerPrint()).thenReturn(false)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupported(BIOMETRIC_STRONG or DEVICE_CREDENTIAL, BIOMETRIC_WEAK or DEVICE_CREDENTIAL))
    }

    @Test
    fun testKeyGuardManager() {
        whenever(biometricAuth.hasDeviceCredential()).thenReturn(true)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupported(BIOMETRIC_STRONG or DEVICE_CREDENTIAL, BIOMETRIC_WEAK or DEVICE_CREDENTIAL))
    }

    @Test
    fun testListener() {
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        val result = object : AuthenticationCallback() {}
        testObject.authenticate(result)
        verify(biometricAuth).biometricAuthListener = testObject.biometricListener
        verify(biometricAuth).authenticate()
    }

    @Test
    fun testBiometricListenerForSuccess() {
        val authenticationResult = mock<AuthenticationResult>()
        val latch = CountDownLatch(1)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)

        val result = object : AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: AuthenticationResult) {
                latch.countDown()
            }
        }
        testObject.authenticate(result)
        testObject.biometricListener?.onAuthenticationSucceeded(authenticationResult)
        latch.await()
    }


    @Test
    fun testBiometricListenerForTimeoutFailure() {
        val latch = CountDownLatch(1)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        val result = object : AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                assertEquals(ERROR_TIMEOUT, errorCode)
                latch.countDown()
            }
        }
        testObject.authenticate(result)
        testObject.biometricListener?.onAuthenticationError(ERROR_TIMEOUT, "Timeout")
        latch.await()
    }

    @Test
    fun testBiometricListenerForWrongFingerPrintFailure() {
        val latch = CountDownLatch(1)
        val testObject = BiometricBindingHandler("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)

        val result = object : AuthenticationCallback() {
            override fun onAuthenticationFailed() {
                latch.countDown()
            }
        }
        testObject.authenticate(result)
        testObject.biometricListener?.onAuthenticationFailed()
        latch.await()

    }
}