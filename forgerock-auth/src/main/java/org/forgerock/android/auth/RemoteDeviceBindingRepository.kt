/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.forgerock.android.auth.ServerConfig.ACCEPT_API_VERSION
import org.forgerock.android.auth.devicebind.DeviceBindingRepository
import org.forgerock.android.auth.devicebind.UserKey
import org.forgerock.android.auth.exception.ApiException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val TAG = RemoteDeviceBindingRepository::class.java.simpleName

internal class RemoteDeviceBindingRepository(val serverConfig: ServerConfig = Config.getInstance().serverConfig) :
    DeviceBindingRepository {
    override suspend fun persist(userKey: UserKey) {
        throw UnsupportedOperationException()
    }

    override suspend fun getAllKeys(): List<UserKey> {
        throw UnsupportedOperationException()
    }

    override suspend fun delete(userKey: UserKey) {
        val response = invoke(userKey)
        if (response.code != HttpURLConnection.HTTP_OK) {
            throw ApiException(response.code, response.message, "Failed to delete resources")
        }
    }

    private suspend fun invoke(userKey: UserKey): Response =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                val client =
                    OkHttpClientProvider.getInstance().lookup(serverConfig)
                val request = okhttp3.Request.Builder()
                    .header(ACCEPT_API_VERSION, "resource=1.0")
                    .url(getUrl(userKey))
                    .delete().build()
                val call = client.newCall(request)
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (continuation.isCancelled) return
                        Logger.warn(TAG, e, "Delete bound device failed."  )
                        continuation.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        continuation.resume(response)
                    }
                })
                continuation.invokeOnCancellation {
                    try {
                        call.cancel()
                    } catch (e: Exception) {
                        Logger.warn(TAG, e, "Cancel API call failed")
                    }
                }
            }

        }

    private fun getUrl(userKey: UserKey): URL {
        val builder = Uri.parse(serverConfig.url).buildUpon()
        builder.appendPath("json")
            .appendPath("realms")
            .appendPath(serverConfig.realm)
            .appendPath("users")
            .appendPath(userKey.userId)
            .appendPath("devices")
            .appendPath("2fa")
            .appendPath("binding")
            .appendPath(userKey.kid)
        return URL(builder.build().toString())
    }
}