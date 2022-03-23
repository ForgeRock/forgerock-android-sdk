package org.forgerock.android.auth.broadcast

import android.content.Context
import org.forgerock.android.auth.Config
import org.forgerock.android.auth.SessionManager
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SSOBroadcastReceiverTest {

    private val config = mock<Config>()
    private val context = mock<Context>()
    private val sessionManager = mock<SessionManager>()

    @Test
    fun `sendBroadcastEventWhenPermissionIsEnabled`() {
        whenever(config.sessionManager).thenReturn(sessionManager)
        val testObject = SSOBroadcastReceiver(config)
        testObject.onReceive(context, null)
        verify(config).init(context)
        verify(sessionManager).close()
    }
}