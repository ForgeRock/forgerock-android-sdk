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

class BiometricUtilTest {

    private val activity = mock<FragmentActivity>()
    private val biometricAuth = mock<BiometricAuth>()

    @Test
    fun testStrongTypeWithBiometricOnly() {
        whenever(biometricAuth.hasBiometricCapability(BiometricManager.Authenticators.BIOMETRIC_STRONG)).thenReturn(true)
        val testObject = BiometricUtil("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ONLY, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupportedForBiometricOnly())
        verify(biometricAuth).authenticatorType = AuthenticatorType.STRONG
    }

    @Test
    fun testWeakTypeWithBiometricOnly() {
        whenever(biometricAuth.hasBiometricCapability(BiometricManager.Authenticators.BIOMETRIC_WEAK)).thenReturn(true)
        val testObject = BiometricUtil("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ONLY, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupportedForBiometricOnly())
        verify(biometricAuth).authenticatorType = AuthenticatorType.WEAK
    }

    @Test
    fun testWeakTypeWithFingerPrint() {
        whenever(biometricAuth.hasEnrolledWithFingerPrint()).thenReturn(true)
        val testObject = BiometricUtil("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ONLY, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupportedForBiometricOnly())
        verify(biometricAuth).authenticatorType = AuthenticatorType.WEAK
    }

    @Test
    fun testFalseCaseForBiometricOnly() {
        whenever(biometricAuth.hasBiometricCapability(BiometricManager.Authenticators.BIOMETRIC_WEAK)).thenReturn(false)
        whenever(biometricAuth.hasBiometricCapability(BiometricManager.Authenticators.BIOMETRIC_STRONG)).thenReturn(false)
        whenever(biometricAuth.hasEnrolledWithFingerPrint()).thenReturn(false)
        val testObject = BiometricUtil("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ONLY, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertFalse(testObject.isSupportedForBiometricOnly())
        verify(biometricAuth, never()).authenticatorType = AuthenticatorType.WEAK
        verify(biometricAuth, never()).authenticatorType = AuthenticatorType.STRONG
    }

    @Test
    fun testSuccessCaseForBiometricOnlyWhenAnyOneisTrue() {
        whenever(biometricAuth.hasBiometricCapability(BiometricManager.Authenticators.BIOMETRIC_WEAK)).thenReturn(true)
        whenever(biometricAuth.hasBiometricCapability(BiometricManager.Authenticators.BIOMETRIC_STRONG)).thenReturn(false)
        whenever(biometricAuth.hasEnrolledWithFingerPrint()).thenReturn(false)
        val testObject = BiometricUtil("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ONLY, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupportedForBiometricOnly())
        verify(biometricAuth).authenticatorType = AuthenticatorType.WEAK
    }

    @Test
    fun testStrongTypeWithBiometricAndDeviceCredential() {
        whenever(biometricAuth.hasBiometricCapability(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)).thenReturn(true)
        val testObject = BiometricUtil("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupportedForBiometricAndDeviceCredential())
        verify(biometricAuth).authenticatorType = AuthenticatorType.STRONG
    }

    @Test
    fun testWeakTypeWithBiometricAndDeviceCredential() {
        whenever(biometricAuth.hasBiometricCapability(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)).thenReturn(true)
        val testObject = BiometricUtil("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupportedForBiometricAndDeviceCredential())
        verify(biometricAuth).authenticatorType = AuthenticatorType.WEAK
    }

    @Test
    fun testWeakTypeFingerPrintWithBiometricAndDeviceCredential() {
        whenever(biometricAuth.hasEnrolledWithFingerPrint()).thenReturn(true)
        val testObject = BiometricUtil("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupportedForBiometricAndDeviceCredential())
        verify(biometricAuth).authenticatorType = AuthenticatorType.WEAK
    }

    @Test
    fun testSuccessCaseForBiometricAndDeviceCredentialWhenAnyOneisTrue() {
        whenever(biometricAuth.hasBiometricCapability(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)).thenReturn(true)
        whenever(biometricAuth.hasBiometricCapability(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)).thenReturn(false)
        whenever(biometricAuth.hasEnrolledWithFingerPrint()).thenReturn(false)
        val testObject = BiometricUtil("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupportedForBiometricAndDeviceCredential())
        verify(biometricAuth).authenticatorType = AuthenticatorType.WEAK
    }

    @Test
    fun testKeyGuardManager() {
        whenever(biometricAuth.hasDeviceCredential()).thenReturn(true)
        val testObject = BiometricUtil("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        assertTrue(testObject.isSupportedForBiometricAndDeviceCredential())
        verify(biometricAuth, never()).authenticatorType = AuthenticatorType.WEAK
        verify(biometricAuth, never()).authenticatorType = AuthenticatorType.STRONG
    }

    @Test
    fun testListener() {
        val testObject = BiometricUtil("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)

        val listener = object: BiometricAuthCompletionHandler {
            override fun onSuccess(result: BiometricPrompt.AuthenticationResult?) {

            }
            override fun onError(errorCode: Int, errorMessage: String?) {}

        }
        testObject.setListener(listener)
        verify(biometricAuth).biometricAuthListener = listener
    }

    @Test
    fun testBiometricListenerForSuccess() {
        val latch = CountDownLatch(1)
        val testObject = BiometricUtil("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        val listener = testObject.getBiometricListener(timeout = 60) {
            assertEquals(it, BiometricStatus(true, null, null))
            assertNull(it.errorType)
            latch.countDown()
        }
        testObject.setListener(listener)
        listener.onSuccess(null)
        latch.await()
    }


    @Test
    fun testBiometricListenerForTimeoutFailure() {
        val latch = CountDownLatch(1)
        val testObject = BiometricUtil("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        val listener = testObject.getBiometricListener(timeout = -100) {
            assertEquals(false, it.isSucceeded)
            assertEquals("Timeout", it.errorType?.name)
            assertNotNull(it.errorType)
            latch.countDown()
        }
        testObject.setListener(listener)
        listener.onSuccess(null)
        latch.await()
    }

    @Test
    fun testBiometricListenerForWrongFingerPrintFailure() {
        val latch = CountDownLatch(1)
        val testObject = BiometricUtil("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        val listener = testObject.getBiometricListener(timeout = 60) {
            assertEquals(false, it.isSucceeded)
            assertEquals("Abort", it.errorType?.name)
            assertNotNull(it.errorType)
            latch.countDown()
        }
        testObject.setListener(listener)
        listener.onError(1, "invalid credential")
        latch.await()
    }



    @Test
    fun authenticate() {
        val testObject = BiometricUtil("title", "subtitle", "description", deviceBindAuthenticationType = DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK, fragmentActivity = activity, biometricAuth = biometricAuth)
        testObject.authenticate()
        verify(biometricAuth).authenticate()
    }
}