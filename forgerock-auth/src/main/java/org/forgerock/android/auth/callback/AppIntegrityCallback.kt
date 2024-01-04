/*
 * Copyright (c) 2023-2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.content.Context
import android.util.Base64
import androidx.annotation.Keep
import androidx.annotation.OpenForTesting
import com.google.android.play.core.integrity.IntegrityManager
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.Listener
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.Node
import org.forgerock.android.auth.callback.RequestType.CLASSIC
import org.forgerock.android.auth.callback.RequestType.STANDARD
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private val TAG = AppIntegrityCallback::class.java.simpleName

/**
 * Callback to collect integrity token.
 */
open class AppIntegrityCallback : NodeAware, AbstractCallback {

    @Keep
    @JvmOverloads
    constructor(jsonObject: JSONObject, index: Int) : super(jsonObject, index)

    @Keep
    @JvmOverloads
    constructor() : super()

    @OpenForTesting
    companion object {
        val cache = ConcurrentHashMap<String, StandardIntegrityTokenProvider>()
    }

    /**
     * The [Node] that associate with this Callback
     */
    private lateinit var node: Node

    /**
     * The request type
     */
    lateinit var requestType: RequestType
        private set

    /**
     * The projectNumber received from server
     */
    lateinit var projectNumber: String
        private set

    /**
     * The nonce received from server
     */
    lateinit var nonce: String
        private set

    final override fun setAttribute(name: String, value: Any) = when (name) {
        "requestType" -> requestType = RequestType.valueOf((value as String).uppercase())
        "projectNumber" -> projectNumber = value as String
        "nonce" -> nonce = value as String
        else -> {}
    }

    override fun getType(): String {
        return "AppIntegrityCallback"
    }

    /**
     * Input the Token to the server
     * @param value The JWS value.
     */
    fun setToken(value: String) {
        super.setValue(value, 0)
    }

    /**
     * Input the Client Error to the server
     * @param value Error String.
     */
    fun setClientError(value: String) {
        super.setValue(value, 1)
    }

    /**
     * Retrieve the timeout to retrieve an Integrity Token
     * Default to 10 Seconds
     */
    open fun getTimeout(): Duration {
        return 10.toDuration(DurationUnit.SECONDS)
    }


    /**
     * Request for Integrity Token from Google SDK
     *
     * @param context  The Application Context
     * @param listener The Listener to listen for the result
     */
    open fun requestIntegrityToken(context: Context,
                                   listener: FRListener<Void>) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                requestIntegrityToken(context)
                Listener.onSuccess(listener, null)
            } catch (e: Exception) {
                Listener.onException(listener, e)
            }
        }
    }

    /**
     * Request the integrity token
     *
     * @param context  The Application Context
     */
    open suspend fun requestIntegrityToken(context: Context) {

        try {
            withTimeout(getTimeout()) {
                when (requestType) {
                    CLASSIC -> {
                        val integrityManager = getIntegrityManager(context)
                        val builder = IntegrityTokenRequest.builder()
                        builder.setCloudProjectNumber(projectNumber.toLong())
                        builder.setNonce(hashWithNonce(getAuthId()))
                        setToken(integrityManager.requestIntegrityToken(builder.build()).await()
                            .token())
                    }

                    STANDARD -> {
                        setToken(getStandardIntegrityTokenProvider(context)
                            .request(StandardIntegrityTokenRequest
                                .builder().setRequestHash(hash(getAuthId())).build())
                            .await().token())
                    }
                }
            }
        } catch (e: Exception) {
            Logger.error(TAG, t = e, message = e.message)
            setClientError("ClientDeviceErrors")
            //This can be IntegrityServiceException
            throw e
        }
    }

    /**
     * Clear the cache to store the [StandardIntegrityTokenProvider]
     */
    open fun clearCache() {
        cache.clear()
    }

    @OpenForTesting
    open fun getIntegrityManager(context: Context): IntegrityManager {
        return IntegrityManagerFactory.create(context)
    }

    /**
     * For Standard API
     */
    @OpenForTesting
    open fun getStandardIntegrityManager(context: Context): StandardIntegrityManager {
        return IntegrityManagerFactory.createStandard(context)
    }

    open suspend fun getStandardIntegrityTokenProvider(context: Context):
            StandardIntegrityTokenProvider {
        return cache.getOrElse(projectNumber) {
            val integrityManager = getStandardIntegrityManager(context)
            val request = PrepareIntegrityTokenRequest.builder()
                .setCloudProjectNumber(projectNumber.toLong()).build()
            val provider = integrityManager.prepareIntegrityToken(request).await()
            cache[projectNumber] = provider
            return provider
        }
    }

    @OpenForTesting
    open fun getAuthId(): ByteArray {
        return node.authId.toByteArray()
    }

    private fun hashWithNonce(input: ByteArray): String {
        val flags = Base64.NO_WRAP or Base64.URL_SAFE
        val bytes = Base64.decode(nonce, flags)
        val result = input.plus(bytes)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(result)
        return Base64.encodeToString(messageDigest.digest(), flags);
    }

    private fun hash(input: ByteArray): String {
        val flags = Base64.NO_WRAP or Base64.URL_SAFE
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(input)
        return Base64.encodeToString(messageDigest.digest(), flags);
    }


    override fun setNode(node: Node) {
        this.node = node
    }

}

/**
 * The Request Type, please see https://developer.android.com/google/play/integrity/overview for
 * detail.
 */
enum class RequestType {
    CLASSIC,
    STANDARD;
}