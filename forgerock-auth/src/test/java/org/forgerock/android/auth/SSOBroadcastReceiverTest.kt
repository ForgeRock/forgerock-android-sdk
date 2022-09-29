/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */


package org.forgerock.android.auth

import android.content.Context
import android.content.Intent
import org.junit.Test
import org.mockito.kotlin.*


class SSOBroadcastReceiverTest {

    private val config = mock<Config>()
    private val intent = mock<Intent>()
    private val tokenManager = mock<TokenManager>()
    private val resources = mock<android.content.res.Resources>()
    private val context = mock<Context> {
        on { packageName } doReturn "com.forgerock.sso"
        on { resources } doReturn resources
        on { resources.getString(R.string.forgerock_sso_logout) } doReturn "com.forgerock.action"
    }

    @Test
    fun receiveBroadcastEventAndInvokeSessionManager() {
        whenever(intent.action).thenReturn("com.forgerock.action")
        whenever(intent.getStringExtra("BROADCAST_PACKAGE_KEY")).thenReturn("com.forgerock.action")
        whenever(config.tokenManager).thenReturn(tokenManager)

        val testObject = SSOBroadcastReceiver(config)
        testObject.onReceive(context, intent)

        verify(tokenManager).revoke(null)

    }

    @Test
    fun doNotInvokeSessionManagerWhenTheContextIsNull() {
        val testObject = SSOBroadcastReceiver(config)
        testObject.onReceive(null, null)
        verifyNoInteractions(tokenManager)
    }
}