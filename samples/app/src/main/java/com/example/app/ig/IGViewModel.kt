/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.ig

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.forgerock.android.auth.FRListenerFuture
import org.forgerock.android.auth.Logger.Companion.isDebugEnabled
import org.forgerock.android.auth.PolicyAdvice
import org.forgerock.android.auth.SecureCookieJar
import org.forgerock.android.auth.interceptor.AccessTokenInterceptor
import org.forgerock.android.auth.interceptor.AdviceHandler
import org.forgerock.android.auth.interceptor.IdentityGatewayAdviceInterceptor
import java.io.IOException
import java.util.concurrent.Future

class IGViewModel() : ViewModel() {

    val state = MutableStateFlow(IGState())

    fun invoke(context: Context, api: String, useHeader: Boolean) {
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .followRedirects(false)

        if (isDebugEnabled()) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            builder.addInterceptor(interceptor)
        }

        builder.addInterceptor(object : IdentityGatewayAdviceInterceptor() {
            override fun getAdviceHandler(advice: PolicyAdvice): AdviceHandler {
                return object : AdviceHandler {
                    override suspend fun onAdviceReceived(context: Context,
                                                          advice: PolicyAdvice) {
                        suspendCancellableCoroutine { continuation ->
                            state.update {
                                it.copy(IGTransitionState.Authenticate(advice, continuation))
                            }
                        }
                    }
                }
            }
        })
        builder.addInterceptor(AccessTokenInterceptor())
        builder.cookieJar(SecureCookieJar.builder()
            .context(context)
            .build())

        val client: OkHttpClient = builder.build()
        val requestBuilder: Request.Builder = Request.Builder()
            .url(api)
        if (useHeader) {
            requestBuilder.addHeader("x-authenticate-response", "header");
        }
        val request = requestBuilder.build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                state.update { it.copy(IGTransitionState.Finished(exception = e)) }
            }

            override fun onResponse(call: Call, response: Response) {
                var result: String
                response.let {
                    result = if (it.isSuccessful) {
                        try {
                            "Response:" + it.body!!.string()
                        } catch (e: IOException) {
                            e.message.toString()
                        }
                    } else {
                        "Failed:" + it.message
                    }
                }
                state.update { it.copy(IGTransitionState.Finished(result)) }
            }
        })
    }

    companion object {
        fun factory(
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return IGViewModel() as T
            }
        }
    }
}