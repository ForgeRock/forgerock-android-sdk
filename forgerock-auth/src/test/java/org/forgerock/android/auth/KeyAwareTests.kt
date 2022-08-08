package org.forgerock.android.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.forgerock.android.auth.devicebind.KeyAware
import org.forgerock.android.auth.devicebind.KeyPair
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey

@RunWith(AndroidJUnit4::class)
class KeyAwareTests {

    val context: Context = ApplicationProvider.getApplicationContext()


    @Test
    fun testSigning() {
       val testObject = KeyAware(userId = "jey")
        val kpg: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keys = kpg.generateKeyPair()
        val keyPair = KeyPair(keys.public as RSAPublicKey, keys.private, "jeyAlias")
        val output = testObject.sign(keyPair, "1234", "3123123123")
        assertNotNull(output)
    }

    @Test
    fun getHashProduceSameResultForKey() {
        val testObject = KeyAware()
        val output = testObject.getHash("jey")
        assertEquals("LSMN01yoaef/FfrhERUTtaMdt/AtUNeGZwLZn3V3AFM=\n", output)
        val nextOutput = testObject.getHash("jey")
        assertEquals("LSMN01yoaef/FfrhERUTtaMdt/AtUNeGZwLZn3V3AFM=\n", nextOutput)
    }

}