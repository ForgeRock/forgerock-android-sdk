package org.forgerock.android.auth.broadcast

import android.content.Context
import android.content.Intent
import org.forgerock.android.auth.Config
import org.forgerock.android.auth.R
import org.forgerock.android.auth.SessionManager
import org.junit.Test
import org.mockito.kotlin.*


class SSOBroadcastReceiverTest {

    private val config = mock<Config>()
    private val intent = mock<Intent>()
    private val resources = mock<android.content.res.Resources>()
    private val context = mock<Context> {
        on { packageName } doReturn "com.forgerock.sso"
        on { resources } doReturn resources
        on { resources.getString(R.string.forgerock_sso_logout) } doReturn "com.forgerock.action"
    }
    private val sessionManager = mock<SessionManager>()

    @Test
    fun `receiveBroadcastEventAndInvokeSessionManager`() {
        whenever(intent.action).thenReturn("com.forgerock.action")
        whenever(config.sessionManager).thenReturn(sessionManager)
        whenever(intent.getStringExtra("BROADCAST_PACKAGE_KEY")).thenReturn("com.forgerock.action")
        val testObject = SSOBroadcastReceiver(config)
        testObject.onReceive(context, intent)
        verify(config).init(context)
        verify(sessionManager).close()
    }

    @Test
    fun `doNotInvokeSessionManagerWhenTheContextIsNull`() {
        whenever(config.sessionManager).thenReturn(sessionManager)
        val testObject = SSOBroadcastReceiver(config)
        testObject.onReceive(null, null)
        verifyNoInteractions(sessionManager)
    }
}