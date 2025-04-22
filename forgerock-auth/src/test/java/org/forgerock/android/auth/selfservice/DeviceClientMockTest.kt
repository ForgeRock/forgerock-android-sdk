/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.selfservice

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.forgerock.android.auth.BaseTest
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.SSOToken
import org.forgerock.android.auth.ServerConfig
import org.forgerock.android.auth.exception.ApiException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection

@RunWith(AndroidJUnit4::class)
class DeviceClientMockTest : BaseTest() {

    var context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var deviceClient: DeviceClient

    @Before
    fun setUp() {
        Logger.set(Logger.Level.DEBUG)
        val serverConfig = ServerConfig.builder()
            .identifier("openam")
            .context(context)
            .url(url)
            .realm("alpha")
            .cookieName("5421aeddf91aa20")
        deviceClient = DeviceClient(serverConfig.build()) {
            SSOToken("ssoTokenValue")
        }
    }

    @Test
    fun oathDevice_returnsListOfOathDevices() = runTest {
        enqueue("/selfservice/sessionInfo.json", HttpURLConnection.HTTP_OK)
        enqueue("/selfservice/successOath.json", HttpURLConnection.HTTP_OK)
        val devices = deviceClient.oath.get()
        assert(devices.isNotEmpty())
        val sessionInfoReq = server.takeRequest()
        assertThat(sessionInfoReq.method).isEqualTo("POST")
        assertThat(sessionInfoReq.path).isEqualTo("/json/realms/alpha/sessions?_action=getSessionInfo")
        assertThat(sessionInfoReq.getHeader("5421aeddf91aa20")).isEqualTo("ssoTokenValue")
        assertThat(sessionInfoReq.getHeader("Accept-API-Version")).isEqualTo("resource=2.1, protocol=1.0")

        val oathReq = server.takeRequest()
        assertThat(oathReq.method).isEqualTo("GET")
        assertThat(oathReq.path).isEqualTo("/json/realms/alpha/users/c49e9f78-0193-402e-b8d1-be70da3c3d17/devices/2fa/oath?_queryFilter=true")
        assertThat(oathReq.getHeader("5421aeddf91aa20")).isEqualTo("ssoTokenValue")
        assertThat(oathReq.getHeader("Accept-API-Version")).isEqualTo("resource=1.0")

        assertThat(devices[0].deviceName).isEqualTo("OATH Device")
        assertThat(devices[0].id).isEqualTo("76c0337a-0d61-4e67-bf59-d87417403a91")
        assertThat(devices[0].createdDate).isEqualTo(1728415537308)
        assertThat(devices[0].lastAccessDate).isEqualTo(1728415537308)
        assertThat(devices[0].uuid).isEqualTo("76c0337a-0d61-4e67-bf59-d87417403a91")
    }

    @Test
    fun pushDevice_returnsListOfPushDevices() = runTest {
        enqueue("/selfservice/sessionInfo.json", HttpURLConnection.HTTP_OK)
        enqueue("/selfservice/successPush.json", HttpURLConnection.HTTP_OK)
        val devices = deviceClient.push.get()
        assert(devices.isNotEmpty())

        val sessionInfoReq = server.takeRequest()
        val oathReq = server.takeRequest()
        assertThat(oathReq.method).isEqualTo("GET")
        assertThat(oathReq.path).isEqualTo("/json/realms/alpha/users/c49e9f78-0193-402e-b8d1-be70da3c3d17/devices/2fa/push?_queryFilter=true")
        assertThat(oathReq.getHeader("5421aeddf91aa20")).isEqualTo("ssoTokenValue")
        assertThat(oathReq.getHeader("Accept-API-Version")).isEqualTo("resource=1.0")

        assertThat(devices[0].deviceName).isEqualTo("Push Device")
        assertThat(devices[0].id).isEqualTo("8e569eb8-1eb8-4459-88a4-2151b7e4ba91")
        assertThat(devices[0].createdDate).isEqualTo(1728415625836)
        assertThat(devices[0].lastAccessDate).isEqualTo(1728415625836)
        assertThat(devices[0].uuid).isEqualTo("8e569eb8-1eb8-4459-88a4-2151b7e4ba91")
    }

    @Test
    fun bindingDevice_returnsListOfBindingDevices() = runTest {
        enqueue("/selfservice/sessionInfo.json", HttpURLConnection.HTTP_OK)
        enqueue("/selfservice/successDeviceBinding.json", HttpURLConnection.HTTP_OK)

        val devices = deviceClient.bound.get()
        assert(devices.isNotEmpty())
        assertThat(devices.size).isEqualTo(4)
        val sessionInfoReq = server.takeRequest()
        val oathReq = server.takeRequest()
        assertThat(oathReq.method).isEqualTo("GET")
        assertThat(oathReq.path).isEqualTo("/json/realms/alpha/users/c49e9f78-0193-402e-b8d1-be70da3c3d17/devices/2fa/binding?_queryFilter=true")
        assertThat(oathReq.getHeader("5421aeddf91aa20")).isEqualTo("ssoTokenValue")
        assertThat(oathReq.getHeader("Accept-API-Version")).isEqualTo("resource=1.0")

        assertThat(devices[0].deviceName).isEqualTo("Test2")
        assertThat(devices[0].id).isEqualTo("c026fcf5-633e-4d06-894f-aa23ba32bc0b")
        assertThat(devices[0].createdDate).isEqualTo(1726012192353)
        assertThat(devices[0].lastAccessDate).isEqualTo(1726012206459)
        assertThat(devices[0].uuid).isEqualTo("c026fcf5-633e-4d06-894f-aa23ba32bc0b")

    }

    @Test
    fun webAuthnDevice_returnsListOfWebAuthnDevices() = runTest {
        enqueue("/selfservice/sessionInfo.json", HttpURLConnection.HTTP_OK)
        enqueue("/selfservice/successWebAuthn.json", HttpURLConnection.HTTP_OK)

        val devices = deviceClient.webAuthn.get()
        assert(devices.isNotEmpty())
        val sessionInfoReq = server.takeRequest()
        val oathReq = server.takeRequest()
        assertThat(oathReq.method).isEqualTo("GET")
        assertThat(oathReq.path).isEqualTo("/json/realms/alpha/users/c49e9f78-0193-402e-b8d1-be70da3c3d17/devices/2fa/webauthn?_queryFilter=true")
        assertThat(oathReq.getHeader("5421aeddf91aa20")).isEqualTo("ssoTokenValue")
        assertThat(oathReq.getHeader("Accept-API-Version")).isEqualTo("resource=1.0")

        assertThat(devices[0].deviceName).isEqualTo("sdk_gphone64_arm64")
        assertThat(devices[0].id).isEqualTo("4f5420a8-cfce-438b-843e-6b9ca6b738af")
        assertThat(devices[0].createdDate).isEqualTo(1728415453606)
        assertThat(devices[0].lastAccessDate).isEqualTo(1728415453606)
        assertThat(devices[0].uuid).isEqualTo("4f5420a8-cfce-438b-843e-6b9ca6b738af")

    }

    @Test
    fun profileDevice_returnsListOfProfileDevices() = runTest {
        enqueue("/selfservice/sessionInfo.json", HttpURLConnection.HTTP_OK)
        enqueue("/selfservice/successDeviceProfile.json", HttpURLConnection.HTTP_OK)

        val devices = deviceClient.profile.get()
        assert(devices.isNotEmpty())

        val sessionInfoReq = server.takeRequest()
        val oathReq = server.takeRequest()
        assertThat(oathReq.method).isEqualTo("GET")
        assertThat(oathReq.path).isEqualTo("/json/realms/alpha/users/c49e9f78-0193-402e-b8d1-be70da3c3d17/devices/profile?_queryFilter=true")
        assertThat(oathReq.getHeader("5421aeddf91aa20")).isEqualTo("ssoTokenValue")
        assertThat(oathReq.getHeader("Accept-API-Version")).isEqualTo("resource=1.0")

        assertThat(devices[0].deviceName).isEqualTo("test")
        assertThat(devices[0].id).isEqualTo("ce0677ca57da8b38-5bfaa23e9a8ddc7899638da7cccbfe6a8879b6cf")
        assertThat(devices[0].identifier).isEqualTo("ce0677ca57da8b38-5bfaa23e9a8ddc7899638da7cccbfe6a8879b6cf")
        assertThat(devices[0].lastSelectedDate).isEqualTo(1727110785783)

    }

    @Test
    fun update_DeviceSuccessfully() = runTest {

        enqueue("/selfservice/sessionInfo.json", HttpURLConnection.HTTP_OK)
        enqueue("/selfservice/successDeviceBinding.json", HttpURLConnection.HTTP_OK)
        enqueue("/selfservice/sessionInfo.json", HttpURLConnection.HTTP_OK)
        enqueue("/selfservice/successUpdateDeviceBinding.json", HttpURLConnection.HTTP_OK)

        val devices = deviceClient.bound.get()
        assert(devices.isNotEmpty())
        assertThat(devices.size).isEqualTo(4)
        deviceClient.bound.update(devices[0])
        val sessionInfoReq = server.takeRequest()
        val bindingReq = server.takeRequest()
        val sessionInfoReq2 = server.takeRequest()
        val updateReq = server.takeRequest()
        assertThat(updateReq.method).isEqualTo("PUT")
        assertThat(updateReq.path).isEqualTo("/json/realms/alpha/users/c49e9f78-0193-402e-b8d1-be70da3c3d17/devices/2fa/binding/c026fcf5-633e-4d06-894f-aa23ba32bc0b")
        assertThat(updateReq.getHeader("5421aeddf91aa20")).isEqualTo("ssoTokenValue")
        assertThat(updateReq.getHeader("Accept-API-Version")).isEqualTo("resource=1.0")
    }

    @Test
    fun delete_DeviceSuccessfully() = runTest {
        enqueue("/selfservice/sessionInfo.json", HttpURLConnection.HTTP_OK)
        enqueue("/selfservice/successDeviceBinding.json", HttpURLConnection.HTTP_OK)
        enqueue("/selfservice/sessionInfo.json", HttpURLConnection.HTTP_OK)
        enqueue("/selfservice/successUpdateDeviceBinding.json", HttpURLConnection.HTTP_OK)

        val devices = deviceClient.bound.get()
        assert(devices.isNotEmpty())
        deviceClient.bound.delete(devices[0])
        val sessionInfoReq = server.takeRequest()
        val bindingReq = server.takeRequest()
        val sessionInfoReq2 = server.takeRequest()
        val deleteReq = server.takeRequest()
        assertThat(deleteReq.method).isEqualTo("DELETE")
        assertThat(deleteReq.path).isEqualTo("/json/realms/alpha/users/c49e9f78-0193-402e-b8d1-be70da3c3d17/devices/2fa/binding/c026fcf5-633e-4d06-894f-aa23ba32bc0b")
        assertThat(deleteReq.getHeader("5421aeddf91aa20")).isEqualTo("ssoTokenValue")
        assertThat(deleteReq.getHeader("Accept-API-Version")).isEqualTo("resource=1.0")

    }

    @Test(expected = ApiException::class)
    fun accessDenied() = runTest {
        enqueue("/selfservice/sessionInfo.json", HttpURLConnection.HTTP_OK)
        enqueue("/selfservice/successDeviceBinding.json", HttpURLConnection.HTTP_OK)
        enqueue("/selfservice/sessionInfo.json", HttpURLConnection.HTTP_OK)
        enqueue("/selfservice/accessDenied.json", HttpURLConnection.HTTP_UNAUTHORIZED)

        val devices = deviceClient.bound.get()
        assert(devices.isNotEmpty())
        deviceClient.bound.delete(devices[0])
        val sessionInfoReq = server.takeRequest()
        val bindingReq = server.takeRequest()
        val sessionInfoReq2 = server.takeRequest()
        try {
            val deleteReq = server.takeRequest()
        } catch (e: ApiException) {
            assertThat(e.statusCode).isEqualTo(HttpURLConnection.HTTP_UNAUTHORIZED)
            assertThat(e.error).isEqualTo("{\"code\": 401, \"reason\": \"Unauthorized\", \"message\": \"Access Denied\"}")
            throw e
        }
    }

    @Test(expected = ApiException::class)
    fun forbidden() = runTest {
        enqueue("/selfservice/sessionInfo.json", HttpURLConnection.HTTP_OK)
        enqueue("/selfservice/successDeviceBinding.json", HttpURLConnection.HTTP_OK)
        enqueue("/selfservice/sessionInfo.json", HttpURLConnection.HTTP_OK)
        enqueue("/selfservice/forbidden.json", HttpURLConnection.HTTP_UNAUTHORIZED)

        val devices = deviceClient.bound.get()
        assert(devices.isNotEmpty())
        deviceClient.bound.delete(devices[0])
        val sessionInfoReq = server.takeRequest()
        val bindingReq = server.takeRequest()
        val sessionInfoReq2 = server.takeRequest()
        try {
            val deleteReq = server.takeRequest()
        } catch (e: ApiException) {
            assertThat(e.statusCode).isEqualTo(HttpURLConnection.HTTP_FORBIDDEN)
            assertThat(e.error).isEqualTo("{\"code\":403,\"reason\":\"Forbidden\",\"message\":\"User not permitted.\"}")
            throw e
        }
    }


    @Test(expected = ApiException::class)
    fun sessionExpired() = runTest {
        enqueue("/selfservice/accessDenied.json", HttpURLConnection.HTTP_UNAUTHORIZED)
        try {
             deviceClient.bound.get()
        } catch (e: ApiException) {
            assertThat(e.statusCode).isEqualTo(HttpURLConnection.HTTP_UNAUTHORIZED)
            throw e
        }
    }
}