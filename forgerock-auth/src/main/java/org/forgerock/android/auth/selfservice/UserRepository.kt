/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.selfservice

import android.net.Uri
import android.net.Uri.Builder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
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
 * Interface defining the repository for user devices.
 */
interface UserRepo {
    /**
     * Retrieves a list of Oath devices.
     * @return A list of [OathDevice].
     */
    suspend fun oathDevice(): List<OathDevice>

    /**
     * Retrieves a list of Push devices.
     * @return A list of [PushDevice].
     */
    suspend fun pushDevice(): List<PushDevice>

    /**
     * Retrieves a list of Binding devices.
     * @return A list of [BindingDevice].
     */
    suspend fun bindingDevice(): List<BindingDevice>

    /**
     * Retrieves a list of WebAuthn devices.
     * @return A list of [WebAuthnDevice].
     */
    suspend fun webAuthnDevice(): List<WebAuthnDevice>

    /**
     * Retrieves a list of Profile devices.
     * @return A list of [ProfileDevice].
     */
    suspend fun profileDevice(): List<ProfileDevice>

    /**
     * Updates the given device.
     * @param device The [Device] to update.
     */
    suspend fun update(device: Device)

    /**
     * Deletes the given device.
     * @param device The [Device] to delete.
     */
    suspend fun delete(device: Device)
}

/**
 * Implementation of [UserRepo] for managing user devices.
 * @property server The server configuration.
 * @property ssoTokenBlock A suspend function to retrieve the SSO token.
 */
class UserRepository(
    private val server: ServerConfig = Config.getInstance().serverConfig,
    private val ssoTokenBlock: suspend () -> SSOToken = { ssoToken() }
) : UserRepo {

    private val httpClient: OkHttpClient = OkHttpClientProvider.getInstance()
        .lookup(server)

    /**
     * Retrieves a list of Oath devices.
     * @return A list of [OathDevice].
     */
    override suspend fun oathDevice(): List<OathDevice> = withContext(Dispatchers.IO) {
        httpClient.newCall(request("devices/2fa/oath")).execute().use { response ->
            get(response)
        }
    }

    /**
     * Retrieves a list of Push devices.
     * @return A list of [PushDevice].
     */
    override suspend fun pushDevice(): List<PushDevice> = withContext(Dispatchers.IO) {
        httpClient.newCall(request("devices/2fa/push")).execute().use { response ->
            get(response)
        }
    }

    /**
     * Retrieves a list of Binding devices.
     * @return A list of [BindingDevice].
     */
    override suspend fun bindingDevice(): List<BindingDevice> = withContext(Dispatchers.IO) {
        httpClient.newCall(request("devices/2fa/binding")).execute().use { response ->
            get(response)
        }
    }

    /**
     * Retrieves a list of WebAuthn devices.
     * @return A list of [WebAuthnDevice].
     */
    override suspend fun webAuthnDevice(): List<WebAuthnDevice> = withContext(Dispatchers.IO) {
        httpClient.newCall(request("devices/2fa/webauthn")).execute().use { response ->
            get(response)
        }
    }

    /**
     * Retrieves a list of Profile devices.
     * @return A list of [ProfileDevice].
     */
    override suspend fun profileDevice(): List<ProfileDevice> = withContext(Dispatchers.IO) {
        httpClient.newCall(request("devices/profile")).execute().use { response ->
            get(response)
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
    override suspend fun update(device: Device) = withContext(Dispatchers.IO) {
        httpClient.newCall(putRequest(device)).execute().use { response ->
            put(response)
        }
    }

    /**
     * Deletes the given device.
     * @param device The [Device] to delete.
     */
    override suspend fun delete(device: Device) = withContext(Dispatchers.IO) {
        httpClient.newCall(deleteRequest(device)).execute().use { response ->
            put(response)
        }
    }
}

/**
 * Abstract class representing a device.
 */
@Serializable
sealed class Device {
    abstract val id: String
    abstract val deviceName: String
    abstract val urlSuffix: String
}

/**
 * Data class representing a Binding device.
 * @property id The ID of the device.
 * @property deviceName The name of the device.
 * @property deviceId The device ID.
 * @property uuid The UUID of the device.
 * @property createdDate The creation date of the device.
 * @property lastAccessDate The last access date of the device.
 */
@Serializable
data class BindingDevice(
    @SerialName("_id")
    override val id: String,
    override var deviceName: String,
    val deviceId: String,
    val uuid: String,
    val createdDate: Long,
    val lastAccessDate: Long) : Device() {
    override var urlSuffix: String = "devices/2fa/binding"
}

/**
 * Data class representing an Oath device.
 * @property id The ID of the device.
 * @property deviceName The name of the device.
 * @property uuid The UUID of the device.
 * @property createdDate The creation date of the device.
 * @property lastAccessDate The last access date of the device.
 */
@Serializable
data class OathDevice(
    @SerialName("_id")
    override val id: String,
    override val deviceName: String,
    val uuid: String,
    val createdDate: Long,
    val lastAccessDate: Long,
) : Device() {
    override var urlSuffix: String = "devices/2fa/oath"
}

/**
 * Data class representing a Push device.
 * @property id The ID of the device.
 * @property deviceName The name of the device.
 * @property uuid The UUID of the device.
 * @property createdDate The creation date of the device.
 * @property lastAccessDate The last access date of the device.
 */
@Serializable
data class PushDevice(
    @SerialName("_id")
    override val id: String,
    override val deviceName: String,
    val uuid: String,
    val createdDate: Long,
    val lastAccessDate: Long,
) : Device() {
    override var urlSuffix: String = "devices/2fa/push"
}

/**
 * Data class representing a WebAuthn device.
 * @property id The ID of the device.
 * @property deviceName The name of the device.
 * @property uuid The UUID of the device.
 * @property credentialId The credential ID of the device.
 * @property createdDate The creation date of the device.
 * @property lastAccessDate The last access date of the device.
 */
@Serializable
data class WebAuthnDevice(
    @SerialName("_id")
    override val id: String,
    override var deviceName: String,
    val uuid: String,
    val credentialId: String,
    val createdDate: Long,
    val lastAccessDate: Long,
) : Device() {
    override var urlSuffix: String = "devices/2fa/webauthn"
}

/**
 * Data class representing a Profile device.
 * @property id The ID of the device.
 * @property deviceName The name of the device (alias).
 * @property identifier The identifier of the device.
 * @property metadata The metadata of the device.
 * @property location The location of the device.
 * @property lastSelectedDate The last selected date of the device.
 */
@Serializable
data class ProfileDevice(
    @SerialName("_id")
    override val id: String,
    @SerialName("alias")
    override var deviceName: String, //alias
    val identifier: String,
    val metadata: JsonObject,
    val location: Location? = null,
    val lastSelectedDate: Long) : Device() {
    override var urlSuffix: String = "devices/profile"
}

/**
 * Data class representing a location.
 * @property latitude The latitude of the location.
 * @property longitude The longitude of the location.
 */
@Serializable
data class Location(val latitude: Double, val longitude: Double)