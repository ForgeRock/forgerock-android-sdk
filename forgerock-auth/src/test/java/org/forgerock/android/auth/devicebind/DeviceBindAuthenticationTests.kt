package org.forgerock.android.auth.devicebind

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.forgerock.android.auth.biometric.BiometricAuthCompletionHandler
import org.forgerock.android.auth.devicebind.KeyPair
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey

@RunWith(AndroidJUnit4::class)
class DeviceBindAuthenticationTests {

    val context: Context = ApplicationProvider.getApplicationContext()
    val mockBiometricInterface = mock<BiometricInterface>()
    val deviceBindAuthentication = mock<DeviceBindAuthentication>()

    @Test
    fun testSigningData() {
       val testObject = BiometricAndDeviceCredential(mockBiometricInterface, deviceBindAuthentication)
        val kpg: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keys = kpg.generateKeyPair()
        val keyPair = KeyPair(keys.public as RSAPublicKey, keys.private, "jeyAlias")
        val output = testObject.sign(keyPair, "1234", "3123123123", "77888")
        assertNotNull(output)
    }

}