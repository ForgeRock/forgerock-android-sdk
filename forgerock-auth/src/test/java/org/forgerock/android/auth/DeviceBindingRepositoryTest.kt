/*
 * Copyright (c) 2022 - 2023  ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.squareup.okhttp.mockwebserver.MockResponse
import junit.framework.Assert.fail
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import org.forgerock.android.auth.devicebind.DeviceBindingRepository
import org.forgerock.android.auth.devicebind.UserKey
import org.forgerock.android.auth.exception.ApiException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection

@RunWith(AndroidJUnit4::class)
class DeviceBindingRepositoryTest : BaseTest() {

    private lateinit var deviceBindingRepository: DeviceBindingRepository

    @Before
    fun setUp() {
        deviceBindingRepository = RemoteDeviceBindingRepository()
    }

    @Test
    fun `Test delete api with 200 status code returned`(): Unit = runBlocking {
        server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_OK))
        val userKey = UserKey("id", "id=demo,ou=user,dc=openam,dc=forgerock,dc=org", "demo", "1234", DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)
        deviceBindingRepository.delete(userKey)
        val recordedRequest = server.takeRequest()
        assertThat(recordedRequest.method).isEqualTo("DELETE")
        assertThat(recordedRequest.path).isEqualTo("/json/realms/root/users/id%3Ddemo%2Cou%3Duser%2Cdc%3Dopenam%2Cdc%3Dforgerock%2Cdc%3Dorg/devices/2fa/binding/1234")
    }

    @Test
    fun `Test delete api with 403 status code returned`(): Unit = runBlocking {
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_FORBIDDEN))
        val userKey = UserKey("id", "demo", "demo", "1234", DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK )
        try {
            deviceBindingRepository.delete(userKey)
            fail()
        } catch (e: ApiException) {
            assertThat(e.statusCode).isEqualTo(HttpURLConnection.HTTP_FORBIDDEN)
        }
    }
}