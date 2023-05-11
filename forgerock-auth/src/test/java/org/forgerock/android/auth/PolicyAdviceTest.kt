/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.squareup.okhttp.mockwebserver.MockResponse
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.assertj.core.api.Assertions
import org.forgerock.android.auth.PolicyAdvice.Companion.parse
import org.forgerock.android.auth.interceptor.AdviceHandler
import org.forgerock.android.auth.interceptor.IdentityGatewayAdviceInterceptor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.net.HttpURLConnection

@RunWith(AndroidJUnit4::class)
class PolicyAdviceTest : BaseTest() {

    @Before
    fun setUp() {
        val scenario = ActivityScenario.launch(
            DummyActivity::class.java)
        scenario.onActivity { activity: DummyActivity? ->
            InitProvider.setCurrentActivity(activity)
        }
    }

    @Test
    fun testParse() {
        val source = "<Advices>" +
                "<AttributeValuePair>" +
                "<Attribute name=\"dummyName\"/>" +
                "<Value>dummyValue</Value>" +
                "</AttributeValuePair>" +
                "</Advices>"
        val advice = parse(source)
        Assertions.assertThat(advice.toString()).isEqualTo(source)
    }

    @Test
    fun testTransactionConditionAdvice() {
        val redirect =
            "http://openam.example.com:8081/openam/?goto=http://openig.example.com:8080/products?_txid%3D3b8c1b2b-0aed-461a-a49b-f35da8276d12&realm=/&authIndexType=composite_advice&authIndexValue=%3CAdvices%3E%3CAttributeValuePair%3E%3CAttribute%20name%3D%22TransactionConditionAdvice%22/%3E%3CValue%3E3b8c1b2b-0aed-461a-a49b-f35da8276d12%3C/Value%3E%3C/AttributeValuePair%3E%3C/Advices%3E"
        server.enqueue(MockResponse()
            .addHeader("Location", redirect)
            .setResponseCode(307))
        enqueue("/products.json", HttpURLConnection.HTTP_OK)
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .followRedirects(false)
        builder.addInterceptor(object : IdentityGatewayAdviceInterceptor() {
            override fun getAdviceHandler(advice: PolicyAdvice): AdviceHandler {

                return object : AdviceHandler {
                    override suspend fun onAdviceReceived(context: Context, advice: PolicyAdvice) {
                        Assertions.assertThat(advice.toString())
                            .isEqualTo("<Advices><AttributeValuePair><Attribute name=\"TransactionConditionAdvice\"/><Value>3b8c1b2b-0aed-461a-a49b-f35da8276d12</Value></AttributeValuePair></Advices>")
                    }
                }
            }
        })
        val client: OkHttpClient = builder.build()
        val request: Request = Request.Builder().url("$url/products").build()
        val future = FRListenerFuture<Response>()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                future.onException(e)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                future.onSuccess(response)
            }
        })
        val result = future.get()
        Assertions.assertThat(result.body!!.string()).isEqualTo("""{"products": "Android"}""")
        server.takeRequest() //Redirect
        val recordedRequest = server.takeRequest() //resource
        Assertions.assertThat(Uri.parse(recordedRequest.path).getQueryParameter("_txid"))
            .isEqualTo("3b8c1b2b-0aed-461a-a49b-f35da8276d12")
    }

    @Test
    fun testAuthenticationToServiceConditionAdvice() {
        val redirect =
            "http://openam.example.com:8081/openam/?goto=http://openig.example.com:8080/products&realm=/&authIndexType=composite_advice&authIndexValue=%3CAdvices%3E%3CAttributeValuePair%3E%3CAttribute%20name%3D%22AuthenticateToServiceConditionAdvice%22/%3E%3CValue%3E/:Example%3C/Value%3E%3C/AttributeValuePair%3E%3C/Advices%3E"
        server.enqueue(MockResponse()
            .addHeader("Location", redirect)
            .setResponseCode(307))
        enqueue("/products.json", HttpURLConnection.HTTP_OK)
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .followRedirects(false)
        builder.addInterceptor(object : IdentityGatewayAdviceInterceptor() {
            override fun getAdviceHandler(advice: PolicyAdvice): AdviceHandler {

                return object : AdviceHandler {
                    override suspend fun onAdviceReceived(context: Context, advice: PolicyAdvice) {
                        Assertions.assertThat(advice.toString())
                            .isEqualTo("<Advices><AttributeValuePair><Attribute name=\"AuthenticateToServiceConditionAdvice\"/><Value>/:Example</Value></AttributeValuePair></Advices>")
                    }
                }
            }
        })
        val client: OkHttpClient = builder.build()
        val request: Request = Request.Builder().url("$url/products").build()
        val future = FRListenerFuture<Response>()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                future.onException(e)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                future.onSuccess(response)
            }
        })
        val result = future.get()
        Assertions.assertThat(result.body!!.string()).isEqualTo("""{"products": "Android"}""")
        server.takeRequest() //Redirect
        server.takeRequest() //resource
    }

    @Test
    fun testNoAdvice() {
        val redirect =
            "http://openam.example.com:8081/openam/?goto=http://openig.example.com:8080/products&realm=/"
        server.enqueue(MockResponse()
            .addHeader("Location", redirect)
            .setResponseCode(307))
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .followRedirects(false)
        builder.addInterceptor(object : IdentityGatewayAdviceInterceptor() {
            override fun getAdviceHandler(advice: PolicyAdvice): AdviceHandler {
                throw IllegalArgumentException()
            }
        })
        val client: OkHttpClient = builder.build()
        val request: Request = Request.Builder().url("$url/products").build()
        val future = FRListenerFuture<Response>()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                future.onException(e)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                future.onSuccess(response)
            }
        })
        val result = future.get()

        //When no advice return the original response.
        Assertions.assertThat(result.code).isEqualTo(307)
        Assertions.assertThat(result.header("location")).isEqualTo(redirect)
    }

    @Test
    fun testFailedToParseAdvice() {

        //Invalid advice xml format
        val redirect =
            "http://openam.example.com:8081/openam/?goto=http://openig.example.com:8080/products&realm=/&authIndexType=composite_advice&authIndexValue=<Advices><AttributeValuePair><Attribute name=\"AuthenticateToServiceConditionAdvice\"/><Value>/:Example</Value></AttributeValuePair>"
        server.enqueue(MockResponse()
            .addHeader("Location", redirect)
            .setResponseCode(307))
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .followRedirects(false)
        builder.addInterceptor(object : IdentityGatewayAdviceInterceptor() {
            override fun getAdviceHandler(advice: PolicyAdvice): AdviceHandler {
                throw IllegalArgumentException()
            }
        })
        val client: OkHttpClient = builder.build()
        val request: Request = Request.Builder().url("$url/products").build()
        val future = FRListenerFuture<Response>()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                future.onException(e)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                future.onSuccess(response)
            }
        })
        val result = future.get()

        //When no advice return the original response.
        Assertions.assertThat(result.code).isEqualTo(307)
        Assertions.assertThat(result.header("location")).isEqualTo(redirect)
    }

    @Test
    fun testAdviceWithBase64Encoded() {
        val redirect =
            "http://openam.example.com:8081/openam/?goto=http://openig.example.com:8080/products?_txid%ca267811-2cf0-4823-857c-93aa230e21ca&realm=/&authIndexType=composite_advice&authIndexValue=PEFkdmljZXM-PEF0dHJpYnV0ZVZhbHVlUGFpcj48QXR0cmlidXRlIG5hbWU9IlRyYW5zYWN0aW9uQ29uZGl0aW9uQWR2aWNlIi8-PFZhbHVlPmNhMjY3ODExLTJjZjAtNDgyMy04NTdjLTkzYWEyMzBlMjFjYTwvVmFsdWU-PC9BdHRyaWJ1dGVWYWx1ZVBhaXI-PC9BZHZpY2VzPg"
        server.enqueue(MockResponse()
            .addHeader("Location", redirect)
            .setResponseCode(307))
        enqueue("/products.json", HttpURLConnection.HTTP_OK)
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .followRedirects(false)
        builder.addInterceptor(object : IdentityGatewayAdviceInterceptor() {
            override fun getAdviceHandler(advice: PolicyAdvice): AdviceHandler {

                return object : AdviceHandler {
                    override suspend fun onAdviceReceived(context: Context, advice: PolicyAdvice) {
                        Assertions.assertThat(advice.toString())
                            .isEqualTo("<Advices><AttributeValuePair><Attribute name=\"TransactionConditionAdvice\"/><Value>ca267811-2cf0-4823-857c-93aa230e21ca</Value></AttributeValuePair></Advices>")
                    }
                }
            }
        })
        val client: OkHttpClient = builder.build()
        val request: Request = Request.Builder().url("$url/products").build()
        val future = FRListenerFuture<Response>()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                future.onException(e)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                future.onSuccess(response)
            }
        })
        val result = future.get()
        Assertions.assertThat(result.body!!.string()).isEqualTo("""{"products": "Android"}""")
        server.takeRequest() //Redirect
        val recordedRequest = server.takeRequest() //resource
        Assertions.assertThat(Uri.parse(recordedRequest.path).getQueryParameter("_txid"))
            .isEqualTo("ca267811-2cf0-4823-857c-93aa230e21ca")
    }

    @Test
    fun testAdviceWithHeaderResponse() {
        server.enqueue(MockResponse()
            .addHeader("WWW-Authenticate", "SSOADVICE realm=\"/\",am_uri=\"https://default.forgeops.petrov.ca/am/\",advices=\"eyJUcmFuc2FjdGlvbkNvbmRpdGlvbkFkdmljZSI6WyJmOWRiMDE2YS01NTU1LTRjMjgtYTBjOS1jNzg5MTZjZmViZGIiXX0=\"")
            .setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED))
        enqueue("/products.json", HttpURLConnection.HTTP_OK)
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
        builder.addInterceptor(object : IdentityGatewayAdviceInterceptor() {
            override fun getAdviceHandler(advice: PolicyAdvice): AdviceHandler {

                return object : AdviceHandler {
                    override suspend fun onAdviceReceived(context: Context, advice: PolicyAdvice) {
                        Assertions.assertThat(advice.toString())
                            .isEqualTo("<Advices><AttributeValuePair><Attribute name=\"TransactionConditionAdvice\"/><Value>f9db016a-5555-4c28-a0c9-c78916cfebdb</Value></AttributeValuePair></Advices>")
                    }
                }
            }
        })
        val client: OkHttpClient = builder.build()
        val request: Request = Request.Builder().url("$url/products").build()
        val future = FRListenerFuture<Response>()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                future.onException(e)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                future.onSuccess(response)
            }
        })
        val result = future.get()
        Assertions.assertThat(result.body!!.string()).isEqualTo("""{"products": "Android"}""")
        server.takeRequest() //Redirect
        val recordedRequest = server.takeRequest() //resource
        Assertions.assertThat(Uri.parse(recordedRequest.path).getQueryParameter("_txid"))
            .isEqualTo("f9db016a-5555-4c28-a0c9-c78916cfebdb")
    }
}