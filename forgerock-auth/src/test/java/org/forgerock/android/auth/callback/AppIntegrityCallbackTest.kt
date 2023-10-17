/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback

import android.app.Activity
import android.content.Context
import android.content.res.Resources.NotFoundException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.IntegrityManager
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.IntegrityTokenResponse
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.forgerock.android.auth.Node
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppIntegrityCallbackTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val integrityManager = object : IntegrityManager {
        override fun requestIntegrityToken(request: IntegrityTokenRequest): Task<IntegrityTokenResponse> {
            return object : AbstractTask<IntegrityTokenResponse>() {

                override fun getException(): java.lang.Exception? {
                    return null
                }

                override fun getResult(): IntegrityTokenResponse {
                    return object : IntegrityTokenResponse() {
                        override fun showDialog(p0: Activity?, p1: Int): Task<Int> {
                            TODO("Not yet implemented")
                        }

                        override fun token(): String {
                            val token = JSONObject()
                            token.put("value", "test-integrity-token")
                            token.put("nonce", request.nonce())
                            return token.toString()
                        }
                    }
                }

                override fun isCanceled(): Boolean {
                    return false
                }

                override fun isComplete(): Boolean {
                    return true
                }

                override fun isSuccessful(): Boolean {
                    return true
                }
            }
        }
    }

    private val integrityManagerWithException = object : IntegrityManager {
        override fun requestIntegrityToken(request: IntegrityTokenRequest): Task<IntegrityTokenResponse> {
            return object : AbstractTask<IntegrityTokenResponse>() {
                override fun getException(): Exception {
                    return NotFoundException()
                }

                override fun getResult(): IntegrityTokenResponse {
                    return object : IntegrityTokenResponse() {
                        override fun showDialog(p0: Activity?, p1: Int): Task<Int> {
                            TODO("Not yet implemented")
                        }

                        override fun token(): String {
                            return "test-integrity-token"
                        }
                    }
                }

                override fun <X : Throwable?> getResult(p0: Class<X>): IntegrityTokenResponse {
                    throw UnsupportedOperationException()
                }

                override fun isCanceled(): Boolean {
                    return false
                }

                override fun isComplete(): Boolean {
                    return true
                }

                override fun isSuccessful(): Boolean {
                    return false
                }
            }
        }
    }

    private val standardIntegrityManager = object : StandardIntegrityManager {

        override fun prepareIntegrityToken(request: StandardIntegrityManager.PrepareIntegrityTokenRequest): Task<StandardIntegrityManager.StandardIntegrityTokenProvider> {
            return object :
                AbstractTask<StandardIntegrityManager.StandardIntegrityTokenProvider>() {

                override fun getException(): java.lang.Exception? {
                    return null
                }

                override fun getResult(): StandardIntegrityManager.StandardIntegrityTokenProvider {
                    return object : StandardIntegrityManager.StandardIntegrityTokenProvider {
                        override fun request(request: StandardIntegrityManager.StandardIntegrityTokenRequest): Task<StandardIntegrityToken> {
                            return object : AbstractTask<StandardIntegrityToken>() {
                                override fun getException(): Exception? {
                                    return null
                                }

                                override fun getResult(): StandardIntegrityToken {
                                    return object : StandardIntegrityToken() {
                                        override fun showDialog(p0: Activity?, p1: Int): Task<Int> {
                                            TODO("Not yet implemented")
                                        }

                                        override fun token(): String {
                                            val token = JSONObject()
                                            token.put("value", "test-standard-integrity-token")
                                            token.put("requestHash", request.a())
                                            return token.toString()
                                        }
                                    }
                                }

                                override fun <X : Throwable?> getResult(p0: Class<X>): StandardIntegrityToken {
                                    throw UnsupportedOperationException()
                                }

                                override fun isCanceled(): Boolean {
                                    return false
                                }

                                override fun isComplete(): Boolean {
                                    return true
                                }

                                override fun isSuccessful(): Boolean {
                                    return false
                                }
                            }
                        }
                    }
                }

                override fun isCanceled(): Boolean {
                    return false
                }

                override fun isComplete(): Boolean {
                    return true
                }

                override fun isSuccessful(): Boolean {
                    return true
                }
            }
        }
    }

    private val content = "{\n" +
            "            \"type\": \"AppIntegrityCallback\",\n" +
            "            \"output\": [\n" +
            "                {\n" +
            "                    \"name\": \"requestType\",\n" +
            "                    \"value\": \"Classic\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"name\": \"projectNumber\",\n" +
            "                    \"value\": \"12345678\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"name\": \"nonce\",\n" +
            "                    \"value\": \"FsFTX3CInUu3qgDL_B90EvFNDMyQS3-v2zG6gjIhKhU\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"input\": [\n" +
            "                {\n" +
            "                    \"name\": \"IDToken1token\",\n" +
            "                    \"value\": \"\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"name\": \"IDToken1clientError\",\n" +
            "                    \"value\": \"\"\n" +
            "                }\n" +
            "            ]\n" +
            "        }"

    private val standard = "{\n" +
            "            \"type\": \"AppIntegrityCallback\",\n" +
            "            \"output\": [\n" +
            "                {\n" +
            "                    \"name\": \"requestType\",\n" +
            "                    \"value\": \"Standard\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"name\": \"projectNumber\",\n" +
            "                    \"value\": \"12345678\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"name\": \"nonce\",\n" +
            "                    \"value\": \"FsFTX3CInUu3qgDL_B90EvFNDMyQS3-v2zG6gjIhKhU\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"input\": [\n" +
            "                {\n" +
            "                    \"name\": \"IDToken1token\",\n" +
            "                    \"value\": \"\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"name\": \"IDToken1clientError\",\n" +
            "                    \"value\": \"\"\n" +
            "                }\n" +
            "            ]\n" +
            "        }"


    private val node = Node("dummy-auth-id", "", "",
        "", "", mutableListOf())


    @Test
    fun testValuesAreSetProperly() {
        val callback = AppIntegrityCallback(JSONObject(content), 0)
        callback.setNode(node)
        assertThat(callback.nonce).isEqualTo("FsFTX3CInUu3qgDL_B90EvFNDMyQS3-v2zG6gjIhKhU")
        assertThat(callback.projectNumber).isEqualTo("12345678")
        assertThat(callback.type).isEqualTo("AppIntegrityCallback")
    }

    @Test
    fun testClassicIntegrity(): Unit = runBlocking {
        val callback = object : AppIntegrityCallback(JSONObject(content), 0) {
            override fun getIntegrityManager(context: Context): IntegrityManager {
                return integrityManager
            }
        }
        callback.setNode(node)
        callback.requestIntegrityToken(context)
        val token = JSONObject(callback.getInputValue(0) as String)
        assertThat(token.getString("value")).isEqualTo("test-integrity-token")
        assertThat(token.getString("nonce")).isEqualTo("aGW6dVKHOUQMCc8LwYWd15beWfFv07qJUfBKT3ocZp0=")
    }

    @Test
    fun testStandardIntegrity(): Unit = runBlocking {
        val callback = object : AppIntegrityCallback(JSONObject(standard), 0) {
            override fun getStandardIntegrityManager(context: Context): StandardIntegrityManager {
                return standardIntegrityManager
            }
        }
        callback.clearCache();
        callback.setNode(node)
        callback.requestIntegrityToken(context)
        val token = JSONObject(callback.getInputValue(0) as String)
        assertThat(token.getString("value")).isEqualTo("test-standard-integrity-token")
        assertThat(token.getString("requestHash")).isEqualTo("96HWZnzgu2NkhBtEHJy_66ee_eo_hGNj-jHoOw97o3w=")
        assertThat(AppIntegrityCallback.cache.size).isEqualTo(1)
    }

    @Test
    fun testException(): Unit = runBlocking {
        val callback = object : AppIntegrityCallback(JSONObject(content), 0) {
            override fun getIntegrityManager(context: Context): IntegrityManager {
                return integrityManagerWithException
            }
        }
        callback.setNode(node)
        try {
            callback.requestIntegrityToken(context)
            fail("Should failed")
        } catch (exception: Exception) {
            assertThat(callback.getInputValue(1)).isEqualTo("ClientDeviceErrors")
            assertThat(exception).isInstanceOf(NotFoundException::class.java)
        }
    }

    @Test
    fun testCustomError() {
        val callback = AppIntegrityCallback(JSONObject(content), 0)
        callback.setClientError("test-error")
        assertThat(callback.getInputValue(1)).isEqualTo("test-error")
    }

}