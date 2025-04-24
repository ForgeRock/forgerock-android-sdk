/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.selfservice

import android.net.Uri
import android.net.Uri.Builder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.forgerock.android.auth.Config
import org.forgerock.android.auth.OkHttpClientProvider
import org.forgerock.android.auth.SSOToken
import org.forgerock.android.auth.ServerConfig
import org.forgerock.android.auth.exception.ApiException
import org.forgerock.android.auth.json
import java.net.URL

private const val CONTENT_TYPE = "Content-Type"
private const val APPLICATION_JSON = "application/json"

/**
 * @property server The server configuration.
 * @property ssoTokenBlock A suspend function to retrieve the SSO token.
 */
class DeviceClient(
    private val server: ServerConfig = Config.getInstance().serverConfig,
    private val ssoTokenBlock: suspend () -> SSOToken = { ssoToken() }
) {

    private val httpClient: OkHttpClient = OkHttpClientProvider.getInstance()
        .lookup(server)

    val oath: ImmutableDevice<OathDevice> by lazy {
        object : ImmutableDevice<OathDevice> {
            /**
             * Retrieves a list of Oath devices.
             * @return A list of [OathDevice].
             */
            override suspend fun get(): List<OathDevice> = withContext(Dispatchers.IO) {
                httpClient.newCall(request("devices/2fa/oath")).execute().use { response ->
                    get(response)
                }
            }

            /**
             * Deletes the specified Oath device.
             * @param device The [OathDevice] to delete.
             */
            override suspend fun delete(device: OathDevice) = deleteDevice(device)
        }
    }

    val push: ImmutableDevice<PushDevice> by lazy {
        object : ImmutableDevice<PushDevice> {
            /**
             * Retrieves a list of Push devices.
             * @return A list of [PushDevice].
             */
            override suspend fun get(): List<PushDevice> = withContext(Dispatchers.IO) {
                httpClient.newCall(request("devices/2fa/push")).execute().use { response ->
                    get(response)
                }
            }

            /**
             * Deletes the specified Push device.
             * @param device The [PushDevice] to delete.
             */
            override suspend fun delete(device: PushDevice) = deleteDevice(device)
        }
    }

    val bound: MutableDevice<BoundDevice> by lazy {
        object : MutableDevice<BoundDevice> {
            /**
             * Retrieves a list of Bound devices.
             * @return A list of [BoundDevice].
             */
            override suspend fun get(): List<BoundDevice> = withContext(Dispatchers.IO) {
                httpClient.newCall(request("devices/2fa/binding")).execute().use { response ->
                    get(response)
                }
            }

            /**
             * Deletes the specified Bound device.
             * @param device The [BoundDevice] to delete.
             */
            override suspend fun delete(device: BoundDevice) = deleteDevice(device)

            /**
             * Updates the specified Bound device.
             * @param device The [BoundDevice] to update.
             */
            override suspend fun update(device: BoundDevice) = updateDevice(device)
        }
    }

    val profile: MutableDevice<ProfileDevice> by lazy {
        object : MutableDevice<ProfileDevice> {
            /**
             * Retrieves a list of Profile devices.
             * @return A list of [ProfileDevice].
             */
            override suspend fun get(): List<ProfileDevice> = withContext(Dispatchers.IO) {
                httpClient.newCall(request("devices/profile")).execute().use { response ->
                    get(response)
                }
            }

            /**
             * Deletes the specified Profile device.
             * @param device The [ProfileDevice] to delete.
             */
            override suspend fun delete(device: ProfileDevice) = deleteDevice(device)

            /**
             * Updates the specified Profile device.
             * @param device The [ProfileDevice] to update.
             */
            override suspend fun update(device: ProfileDevice) = updateDevice(device)
        }
    }

    val webAuthn: MutableDevice<WebAuthnDevice> by lazy {
        object : MutableDevice<WebAuthnDevice> {
            /**
             * Retrieves a list of WebAuthn devices.
             * @return A list of [WebAuthnDevice].
             */
            override suspend fun get(): List<WebAuthnDevice> = withContext(Dispatchers.IO) {
                httpClient.newCall(request("devices/2fa/webauthn")).execute().use { response ->
                    get(response)
                }
            }

            /**
             * Deletes the specified WebAuthn device.
             * @param device The [WebAuthnDevice] to delete.
             */
            override suspend fun delete(device: WebAuthnDevice) = deleteDevice(device)

            /**
             * Updates the specified WebAuthn device.
             * @param device The [WebAuthnDevice] to update.
             */
            override suspend fun update(device: WebAuthnDevice) = updateDevice(device)
        }
    }

    /**
     * Parses the response to retrieve a list of devices.
     * @param response The [Response] from the server.
     * @return A list of devices of type [T].
     * @throws ApiException if the response is not successful.
     */
    private inline fun <reified T> get(response: Response): List<T> {
        if (!response.isSuccessful) {
            throw ApiException(response.code,
                response.message,
                response.body?.string() ?: "Failed to query device.")
        }
        val body = response.body?.string()
        return body?.let {
            val jsonObject = json.parseToJsonElement(body).jsonObject
            val result = jsonObject["result"]?.jsonArray
            result?.map {
                val obj = it.jsonObject
                json.decodeFromString(obj.toString())
            }
        } ?: emptyList()
    }

    /**
     * Checks the response for success.
     * @param response The [Response] from the server.
     * @throws ApiException if the response is not successful.
     */
    private fun put(response: Response) {
        if (!response.isSuccessful) {
            throw ApiException(response.code,
                response.message,
                response.body?.string() ?: "Failed to query device.")
        }
    }

    /**
     * Builds a request for the given URI.
     * @param uri The [Uri] to build the request for.
     * @return A [Request.Builder] for the given URI.
     */
    private suspend fun request(uri: Uri): Request.Builder {
        return Request.Builder()
            .url(URL(uri.toString()))
            .header(server.cookieName, ssoTokenBlock().value)
            .header(ServerConfig.ACCEPT_API_VERSION, ServerConfig.API_VERSION_1_0)
            .header(CONTENT_TYPE, APPLICATION_JSON)
    }

    /**
     * Builds a request for the given suffix.
     * @param suffix The suffix to append to the URL.
     * @return A [Request] for the given suffix.
     */
    private suspend fun request(suffix: String): Request {
        val url = urlPrefix()
            .appendEncodedPath(suffix)
            .appendQueryParameter("_queryFilter", "true")
        return request(url.build()).build()
    }

    /**
     * Builds a PUT request for the given device.
     * @param device The [Device] to build the request for.
     * @return A [Request] to update the device.
     */
    private suspend fun putRequest(device: Device): Request {
        val url = urlPrefix()
            .appendEncodedPath(device.urlSuffix)
            .appendPath(device.id)
        return request(url.build())
            .put(json.encodeToString(json.serializersModule.serializer(), device).toRequestBody())
            .build()
    }

    /**
     * Builds a DELETE request for the given device.
     * @param device The [Device] to build the request for.
     * @return A [Request] to delete the device.
     */
    private suspend fun deleteRequest(device: Device): Request {
        val url = urlPrefix()
            .appendEncodedPath(device.urlSuffix)
            .appendPath(device.id)
        return request(url.build())
            .delete()
            .build()
    }

    /**
     * Builds the URL prefix for the requests.
     * @return A [Builder] for the URL prefix.
     */
    private suspend fun urlPrefix(): Builder {
        return server.am().appendPath("json")
            .appendPath("realms")
            .appendPath(server.realm)
            .appendPath("users")
            .appendPath(session(server, ssoTokenBlock).username)
    }

    /**
     * Updates the given device.
     * @param device The [Device] to update.
     */
    private suspend fun updateDevice(device: Device) = withContext(Dispatchers.IO) {
        httpClient.newCall(putRequest(device)).execute().use { response ->
            put(response)
        }
    }

    /**
     * Deletes the given device.
     * @param device The [Device] to delete.
     */
    private suspend fun deleteDevice(device: Device) = withContext(Dispatchers.IO) {
        httpClient.newCall(deleteRequest(device)).execute().use { response ->
            put(response)
        }
    }
}