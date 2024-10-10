/*
 * Copyright (c) 2021 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Parcel
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.safeparcel.SafeParcelWriter
import org.assertj.core.api.Assertions
import org.forgerock.android.auth.callback.CallbackFactory.Companion.getInstance
import org.forgerock.android.auth.callback.IdPCallback
import org.forgerock.android.auth.callback.SelectIdPCallback
import org.forgerock.android.auth.idp.AppleSignInHandler
import org.forgerock.android.auth.idp.GoogleIdentityServicesHandler
import org.forgerock.android.auth.storage.Storage
import org.json.JSONException
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import java.net.HttpURLConnection
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException


@RunWith(AndroidJUnit4::class)
class SocialLoginTest : BaseTest() {

    private lateinit var ssoTokenStorage: Storage<SSOToken>
    private lateinit var cookiesStorage: Storage<Collection<String>>
    private lateinit var storage: Storage<AccessToken>

    @Before
    fun setUpStorage() {
        ssoTokenStorage = sharedPreferencesStorage<SSOToken>(context = context,
            filename = "ssotoken",
            key = "ssotoken", cacheable = false)
        cookiesStorage = sharedPreferencesStorage<Collection<String>>(context = context,
            filename = "cookies",
            key = "cookies", cacheable = false)
        storage = MemoryStorage()
    }

    @After
    fun cleanUp() {
        ssoTokenStorage.delete()
        cookiesStorage.delete()
        storage.delete()
    }


    private fun getGoogleIdentityServicesHandler(activity: FragmentActivity): GoogleIdentityServicesHandler? {
        val fragment = activity.supportFragmentManager
            .findFragmentByTag(GoogleIdentityServicesHandler.TAG)
        if (fragment == null) {
            return null
        }
        return fragment as GoogleIdentityServicesHandler?
    }

    private fun getAppleSignInHandler(activity: FragmentActivity): AppleSignInHandler? {
        val fragment = activity.supportFragmentManager
            .findFragmentByTag(AppleSignInHandler.TAG)
        if (fragment == null) {
            return null
        }
        return fragment as AppleSignInHandler?
    }

    @After
    fun resetIdPCallback() {
        getInstance().register(IdPCallback::class.java)
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class, JSONException::class)
    fun testSelectIdPLocalAuthentication() {
        enqueue("/authTreeMockTest_Authenticate_SelectIdPCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        Config.getInstance().oidcStorage = storage
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage
        Config.getInstance().url = url

        val nodeListenerFuture: NodeListenerFuture<FRSession> =
            object : NodeListenerFuture<FRSession>() {
                override fun onCallbackReceived(state: Node) {
                    if (state.getCallback(SelectIdPCallback::class.java) != null) {
                        state.getCallback(SelectIdPCallback::class.java)
                            .setValue("localAuthentication")
                        state.next(context, this)
                    }
                }
            }

        FRSession.authenticate(context, "", nodeListenerFuture)

        Assert.assertNotNull(nodeListenerFuture.get())
        Assert.assertNotNull(FRUser.getCurrentUser())

        var recordedRequest = server.takeRequest() //First request
        //SelectIdPCallback
        recordedRequest = server.takeRequest()
        val body = recordedRequest.body.readUtf8()
        val result = JSONObject(body)
        Assertions.assertThat(result.getJSONArray("callbacks").length()).isEqualTo(1)
        val selectIdPCallback = result.getJSONArray("callbacks").getJSONObject(0)
        Assertions.assertThat(selectIdPCallback.getString("type")).isEqualTo("SelectIdPCallback")
        Assertions.assertThat(selectIdPCallback.getJSONArray("input").length()).isEqualTo(1)
        val value = selectIdPCallback.getJSONArray("input").getJSONObject(0).getString("value")
        Assertions.assertThat(value).isEqualTo("localAuthentication")
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class, JSONException::class)
    fun testHappyPathWithGoogle() {
        enqueue("/authTreeMockTest_Authenticate_SelectIdPCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_IdPCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        Config.getInstance().oidcStorage = storage
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage
        Config.getInstance().url = url

        val scenario: ActivityScenario<*> = ActivityScenario.launch(
            DummyActivity::class.java)
        scenario.onActivity(InitProvider::setCurrentActivity)

        val executeTree = CountDownLatch(2)

        val idPCallback = arrayOf<IdPCallback?>(null)
        val finalState = arrayOf<Node?>(null)
        val nodeListenerFuture: NodeListenerFuture<FRSession?> =
            object : NodeListenerFuture<FRSession?>() {
                override fun onCallbackReceived(state: Node) {
                    if (state.getCallback<SelectIdPCallback?>(SelectIdPCallback::class.java) != null) {
                        state.getCallback(SelectIdPCallback::class.java).setValue("google")
                        state.next(context, this)
                        executeTree.countDown()
                        return
                    }

                    if (state.getCallback<IdPCallback?>(IdPCallback::class.java) != null) {
                        //idPCallback.signIn needs to be run in Main thread in order to
                        //launch the Fragment
                        executeTree.countDown()
                        idPCallback[0] = state.getCallback(IdPCallback::class.java)
                        finalState[0] = state
                    }
                }
            }

        FRSession.authenticate(context, "", nodeListenerFuture)

        //Wait for the idPCallback to finish
        executeTree.await()

        idPCallback[0]!!.signIn(null, object : FRListener<Void?> {
            override fun onSuccess(result: Void?) {
                finalState[0]!!.next(context, nodeListenerFuture)
            }

            override fun onException(e: Exception) {
                Assert.fail(e.message)
            }
        })
        val status = Status.RESULT_SUCCESS
        val statusParcel = Parcel.obtain()
        status.writeToParcel(statusParcel, 0)
        val bytes = statusParcel.marshall()

        val signInCredentialParcel = Parcel.obtain()
        // NOTE: Upgrading the Google Library to latest breaks this test because,  SignInCredential class is package private now, cannot be used in test . keeping this for reference and applied a workaround.
        // SignInCredential signInCredential = new SignInCredential("1234", "", "", "", null, "", "dummy_id_token", "");
        writeToParcel(signInCredentialParcel, 0, "1234", "dummy_id_token")
        val bytes2 = signInCredentialParcel.marshall()


        val intent = Intent()
        intent.putExtra("sign_in_credential", bytes2)
        intent.putExtra("status", bytes)

        scenario.onActivity { activity: Activity ->
            getGoogleIdentityServicesHandler(activity as FragmentActivity)!!.onActivityResult(
                GoogleIdentityServicesHandler.RC_SIGN_IN,
                Activity.RESULT_OK,
                intent)
        }

        Assert.assertNotNull(nodeListenerFuture.get())
        Assert.assertNotNull(FRUser.getCurrentUser())

        var recordedRequest = server.takeRequest() //First request
        //SelectIdPCallback
        recordedRequest = server.takeRequest()
        var body = recordedRequest.body.readUtf8()
        var result = JSONObject(body)
        Assertions.assertThat(result.getJSONArray("callbacks").length()).isEqualTo(1)
        val selectIdPCallback = result.getJSONArray("callbacks").getJSONObject(0)
        Assertions.assertThat(selectIdPCallback.getString("type")).isEqualTo("SelectIdPCallback")
        Assertions.assertThat(selectIdPCallback.getJSONArray("input").length()).isEqualTo(1)
        val value = selectIdPCallback.getJSONArray("input").getJSONObject(0).getString("value")
        Assertions.assertThat(value).isEqualTo("google")

        //IdPCallback
        recordedRequest = server.takeRequest() //Select IdPCallback
        body = recordedRequest.body.readUtf8()
        result = JSONObject(body)
        Assertions.assertThat(result.getJSONArray("callbacks").length()).isEqualTo(1)
        val idPCallbackReq = result.getJSONArray("callbacks").getJSONObject(0)
        Assertions.assertThat(idPCallbackReq.getString("type")).isEqualTo("IdPCallback")
        Assertions.assertThat(idPCallbackReq.getJSONArray("input").length()).isEqualTo(2)
        val token = idPCallbackReq.getJSONArray("input").getJSONObject(0).getString("value")
        Assertions.assertThat(token).isEqualTo("dummy_id_token")
        val tokenType = idPCallbackReq.getJSONArray("input").getJSONObject(1).getString("value")
        Assertions.assertThat(tokenType).isEqualTo("id_token")
    }


    private fun writeToParcel(dest: Parcel, flags: Int, id: String, token: String) {
        val var10000 = SafeParcelWriter.beginObjectHeader(dest)
        SafeParcelWriter.writeString(dest, 1, id, false)
        SafeParcelWriter.writeString(dest, 2, "", false)
        SafeParcelWriter.writeString(dest, 3, "", false)
        SafeParcelWriter.writeString(dest, 4, "", false)
        SafeParcelWriter.writeParcelable(dest, 5, Uri.EMPTY, flags, false)
        SafeParcelWriter.writeString(dest, 6, "", false)
        SafeParcelWriter.writeString(dest, 7, token, false)
        SafeParcelWriter.writeString(dest, 8, "", false)
        SafeParcelWriter.finishObjectHeader(dest, var10000)
    }


    @Test
    @Throws(InterruptedException::class, ExecutionException::class, JSONException::class)
    fun testErrorWithGoogle() {
        enqueue("/authTreeMockTest_Authenticate_SelectIdPCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_IdPCallback.json", HttpURLConnection.HTTP_OK)

        Config.getInstance().oidcStorage = storage
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage
        Config.getInstance().url = url

        //Create a dummy Fragment
        val scenario: ActivityScenario<*> = ActivityScenario.launch(
            DummyActivity::class.java)
        scenario.onActivity(InitProvider::setCurrentActivity)


        val executeTree = CountDownLatch(2)

        val idPCallback = arrayOf<IdPCallback?>(null)
        val nodeListenerFuture: NodeListenerFuture<FRSession> =
            object : NodeListenerFuture<FRSession>() {
                override fun onCallbackReceived(state: Node) {
                    if (state.getCallback<SelectIdPCallback?>(SelectIdPCallback::class.java) != null) {
                        state.getCallback(SelectIdPCallback::class.java).setValue("google")
                        state.next(context, this)
                        executeTree.countDown()
                        return
                    }

                    if (state.getCallback<IdPCallback?>(IdPCallback::class.java) != null) {
                        //idPCallback.signIn needs to be run in Main thread in order to
                        //launch the Fragment
                        executeTree.countDown()
                        idPCallback[0] = state.getCallback(IdPCallback::class.java)
                    }
                }
            }

        FRSession.authenticate(context, "", nodeListenerFuture)

        //Wait for the idPCallback to finish
        executeTree.await()

        val countDownLatch = CountDownLatch(1)
        idPCallback[0]!!.signIn(null, object : FRListener<Void> {
            override fun onSuccess(result: Void) {
                countDownLatch.countDown()
                Assert.fail()
            }

            override fun onException(e: Exception) {
                Assertions.assertThat(e).isInstanceOf(ApiException::class.java)
                countDownLatch.countDown()
            }
        })

        scenario.onActivity { activity: Activity ->
            getGoogleIdentityServicesHandler(activity as FragmentActivity)!!.onActivityResult(
                GoogleIdentityServicesHandler.RC_SIGN_IN,
                Activity.RESULT_OK,
                null)
        }

        countDownLatch.await()
    }

    @Test
    @Throws(InterruptedException::class,
        ExecutionException::class,
        JSONException::class,
        PackageManager.NameNotFoundException::class)
    fun testHappyPathWithApple() {
        getInstance().register(IdPCallbackMock::class.java)

        enqueue("/authTreeMockTest_Authenticate_SelectIdPCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_IdPCallback_apple.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        Config.getInstance().oidcStorage = storage
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage
        Config.getInstance().url = url

        val scenario: ActivityScenario<*> = ActivityScenario.launch(
            DummyActivity::class.java)
        scenario.onActivity(InitProvider::setCurrentActivity)


        val executeTree = CountDownLatch(2)

        val idPCallback = arrayOf<IdPCallbackMock?>(null)
        val finalState = arrayOf<Node?>(null)
        val nodeListenerFuture: NodeListenerFuture<FRSession?> =
            object : NodeListenerFuture<FRSession?>() {
                override fun onCallbackReceived(state: Node) {
                    if (state.getCallback<SelectIdPCallback?>(SelectIdPCallback::class.java) != null) {
                        state.getCallback(SelectIdPCallback::class.java).setValue("apple")
                        state.next(context, this)
                        executeTree.countDown()
                        return
                    }

                    if (state.getCallback<IdPCallbackMock?>(IdPCallbackMock::class.java) != null) {
                        //idPCallback.signIn needs to be run in Main thread in order to
                        //launch the Fragment
                        idPCallback[0] = state.getCallback(IdPCallbackMock::class.java)
                        finalState[0] = state
                        executeTree.countDown()
                    }
                }
            }

        FRSession.authenticate(context, "", nodeListenerFuture)

        //Wait for the idPCallback to finish
        executeTree.await()

        idPCallback[0]!!.signIn(null, object : FRListener<Void?> {
            override fun onSuccess(result: Void?) {
                finalState[0]!!.next(context, nodeListenerFuture)
            }

            override fun onException(e: Exception) {
                Assert.fail(e.message)
            }
        })

        val intent = Intent()
        intent.setData(Uri.parse("https://opeam.example.com?form_post_entry=dummyValue"))
        scenario.onActivity { activity: Activity ->
            getAppleSignInHandler(activity as FragmentActivity)!!.onActivityResult(
                GoogleIdentityServicesHandler.RC_SIGN_IN,
                Activity.RESULT_OK,
                intent)
        }


        Assert.assertNotNull(nodeListenerFuture.get())
        Assert.assertNotNull(FRUser.getCurrentUser())

        var recordedRequest = server.takeRequest() //First request
        //SelectIdPCallback
        recordedRequest = server.takeRequest()
        var body = recordedRequest.body.readUtf8()
        var result = JSONObject(body)
        Assertions.assertThat(result.getJSONArray("callbacks").length()).isEqualTo(1)
        val selectIdPCallback = result.getJSONArray("callbacks").getJSONObject(0)
        Assertions.assertThat(selectIdPCallback.getString("type")).isEqualTo("SelectIdPCallback")
        Assertions.assertThat(selectIdPCallback.getJSONArray("input").length()).isEqualTo(1)
        val value = selectIdPCallback.getJSONArray("input").getJSONObject(0).getString("value")
        Assertions.assertThat(value).isEqualTo("apple")

        //IdPCallback
        recordedRequest = server.takeRequest() //Select IdPCallback
        body = recordedRequest.body.readUtf8()
        result = JSONObject(body)
        Assertions.assertThat(result.getJSONArray("callbacks").length()).isEqualTo(1)
        val idPCallbackReq = result.getJSONArray("callbacks").getJSONObject(0)
        Assertions.assertThat(idPCallbackReq.getString("type")).isEqualTo("IdPCallback")
        Assertions.assertThat(idPCallbackReq.getJSONArray("input").length()).isEqualTo(2)
        val token = idPCallbackReq.getJSONArray("input").getJSONObject(0).getString("value")
        Assertions.assertThat(token).isEqualTo("form_post_entry")
        val tokenType = idPCallbackReq.getJSONArray("input").getJSONObject(1).getString("value")
        Assertions.assertThat(tokenType).isEqualTo("authorization_code")
        Assertions.assertThat(Uri.parse(recordedRequest.path).getQueryParameter("form_post_entry"))
            .isEqualTo("dummyValue")
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class, JSONException::class)
    fun testAppleSignInWithoutNonce() {
        getInstance().register(IdPCallbackMock::class.java)

        enqueue("/authTreeMockTest_Authenticate_SelectIdPCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_IdPCallback_apple_no_nonce.json",
            HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        Config.getInstance().oidcStorage = storage
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage
        Config.getInstance().url = url

        val fragmentActivity = Robolectric.buildActivity(
            FragmentActivity::class.java).setup().get()
        InitProvider.setCurrentActivity(fragmentActivity)

        val executeTree = CountDownLatch(2)

        val idPCallback = arrayOf<IdPCallbackMock?>(null)
        val finalState = arrayOf<Node?>(null)
        val nodeListenerFuture: NodeListenerFuture<FRSession> =
            object : NodeListenerFuture<FRSession>() {
                override fun onCallbackReceived(state: Node) {
                    if (state.getCallback<SelectIdPCallback?>(SelectIdPCallback::class.java) != null) {
                        state.getCallback(SelectIdPCallback::class.java).setValue("apple")
                        state.next(context, this)
                        executeTree.countDown()
                        return
                    }

                    if (state.getCallback<IdPCallbackMock?>(IdPCallbackMock::class.java) != null) {
                        //idPCallback.signIn needs to be run in Main thread in order to
                        //launch the Fragment
                        idPCallback[0] = state.getCallback(IdPCallbackMock::class.java)
                        finalState[0] = state
                        executeTree.countDown()
                    }
                }
            }

        FRSession.authenticate(context, "", nodeListenerFuture)

        //Wait for the idPCallback to finish
        executeTree.await()

        val result = booleanArrayOf(false)
        val signInCountDownLatch = CountDownLatch(1)
        idPCallback[0]!!.signIn(null, object : FRListener<Void> {
            override fun onSuccess(result: Void) {
                Assert.fail("Expect fail when no nonce")
            }

            override fun onException(e: Exception) {
                result[0] = true
                Assertions.assertThat(e).isInstanceOf(IllegalArgumentException::class.java)
                signInCountDownLatch.countDown()
            }
        })

        signInCountDownLatch.await()
        Assertions.assertThat(result[0]).isTrue()
    }


    @Test
    @Throws(InterruptedException::class)
    fun testUnsupportedHandler() {
        enqueue("/authTreeMockTest_Authenticate_SelectIdPCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_IdPCallback_unsupport_provider.json",
            HttpURLConnection.HTTP_OK)

        Config.getInstance().url = url

        //Create a dummy Fragment
        val fragmentActivity = Robolectric.buildActivity(
            FragmentActivity::class.java).setup().get()
        InitProvider.setCurrentActivity(fragmentActivity)

        val nodeListenerFuture: NodeListenerFuture<FRSession> =
            object : NodeListenerFuture<FRSession>() {
                override fun onCallbackReceived(state: Node) {
                    if (state.getCallback(SelectIdPCallback::class.java) != null) {
                        state.getCallback(SelectIdPCallback::class.java).setValue("dummy")
                        state.next(context, this)
                        return
                    }

                    if (state.getCallback(IdPCallback::class.java) != null) {
                        val nodeListener: NodeListenerFuture<FRSession> = this
                        val idPCallback = state.getCallback(IdPCallback::class.java)
                        idPCallback.signIn(null, object : FRListener<Void> {
                            override fun onSuccess(result: Void) {
                                Assert.fail()
                            }

                            override fun onException(e: Exception) {
                                nodeListener.onException(e)
                            }
                        })
                    }
                }
            }

        FRSession.authenticate(context, "", nodeListenerFuture)

        try {
            nodeListenerFuture.get()
            Assert.fail()
        } catch (e: ExecutionException) {
            Assertions.assertThat(e.cause).isInstanceOf(UnsupportedOperationException::class.java)
        }
    }

    companion object {
        private const val DEFAULT_TOKEN_MANAGER_TEST = "DefaultTokenManagerTest"
        private const val DEFAULT_SSO_TOKEN_MANAGER_TEST = "DefaultSSOManagerTest"
    }
}
