/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
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
import okhttp3.Request
import okhttp3.Response
import org.forgerock.android.auth.exception.ApiException
import org.forgerock.android.auth.webauthn.PublicKeyCredentialSource
import org.forgerock.android.auth.webauthn.WebAuthnRepository
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val TAG = RemoteWebAuthnRepository::class.java.simpleName

internal class RemoteWebAuthnRepository(val serverConfig: ServerConfig = Config.getInstance().serverConfig) :
    WebAuthnRepository {

    override suspend fun delete(publicKeyCredentialSource: PublicKeyCredentialSource) {
        val credentialId = String(publicKeyCredentialSource.id)
        val userId = String(publicKeyCredentialSource.userHandle)

        val findResponse = find(userId, credentialId)
        val resourceId = findResponse.optString("uuid", "")
        if (resourceId.isNotEmpty()) {
            val deleteResponse = invokeDelete(userId, resourceId)
            if (deleteResponse.code != HttpURLConnection.HTTP_OK) {
                throw ApiException(deleteResponse.code, deleteResponse.message,
                    "Failed to delete resources")
            }
        }
    }

    private suspend fun find(userId: String, credentialId: String): JSONObject {
        val response = invokeGet(userId, credentialId)
        if (response.code != HttpURLConnection.HTTP_OK) {
            throw ApiException(response.code, response.message, "Failed to find resource")
        } else {
            return JSONObject(response.body.toString())
        }
    }

    private suspend fun invokeGet(userId: String, credentialId: String): Response =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                val client =
                    OkHttpClientProvider.getInstance().lookup(serverConfig)
                val url = getUrl(userId, credentialId)
                val request = Request.Builder()
                    .header(ServerConfig.ACCEPT_API_VERSION, "resource=1.0")
                    .url(url)
                    .get().build()
                val call = client.newCall(request)
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (continuation.isCancelled) return
                        Logger.warn(TAG, e, "Delete webauthn device failed."  )
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

    private suspend fun invokeDelete(userId: String, resourceId:String): Response =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                val client =
                    OkHttpClientProvider.getInstance().lookup(serverConfig)
                val url = getUrl(userId, resourceId)
                val request = Request.Builder()
                    .header(ServerConfig.ACCEPT_API_VERSION, "resource=1.0")
                    .url(url)
                    .delete().build()
                val call = client.newCall(request)
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (continuation.isCancelled) return
                        Logger.warn(TAG, e, "Delete webauthn device failed."  )
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

    private fun getUrl(userId: String, resourceId:String): URL {
        val builder = Uri.parse(serverConfig.url).buildUpon()
        builder.appendPath("json")
            .appendPath("realms")
            .appendPath(serverConfig.realm)
            .appendPath("users")
            .appendPath(userId)
            .appendPath("devices")
            .appendPath("2fa")
            .appendPath("webauthn")
            .appendPath(resourceId)
        return URL(builder.build().toString())
    }

}