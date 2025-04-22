/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer
import org.forgerock.android.auth.SSOToken
import org.forgerock.android.auth.json

class SSOTokenTest {

    @Test
    fun serializeSSOToken() {
        val ssoToken = SSOToken("mySSOToken", "http://success.url", "myRealm")
        val json = json.encodeToString(serializer(), ssoToken)
        assertEquals("{\"value\":\"mySSOToken\",\"successUrl\":\"http://success.url\",\"realm\":\"myRealm\"}", json)
    }

    @Test
    fun deserializeSSOToken() {
        val source = "{\"value\":\"mySSOToken\",\"successUrl\":\"http://success.url\",\"realm\":\"myRealm\"}"
        val ssoToken = json.decodeFromString<SSOToken>(source)
        assertEquals("mySSOToken", ssoToken.value)
        assertEquals("http://success.url", ssoToken.successUrl)
        assertEquals("myRealm", ssoToken.realm)
    }

    @Test
    fun deserializeInvalidSSOToken() {
        val source = "{\"value\":12345,\"successUrl\":\"http://success.url\",\"realm\":\"myRealm\"}"
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<SSOToken>(source)
        }
    }

    @Test
    fun deserializeMissingFields() {
        val source = "{\"value\":\"mySSOToken\"}"
        val ssoToken = json.decodeFromString<SSOToken>(source)
        assertEquals("mySSOToken", ssoToken.value)
        assertEquals("", ssoToken.successUrl)
        assertEquals("", ssoToken.realm)
    }
}