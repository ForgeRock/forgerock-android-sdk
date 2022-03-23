package org.forgerock.android.auth.broadcast

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import org.junit.Test
import org.mockito.kotlin.*

class SSOBroadcastModelTest {

    private val mockPackageManager = mock<PackageManager>()
    private val mockContext = mock<Context> {
        on { packageManager } doReturn mockPackageManager
    }
    private val intent = Intent("org.forgerock.android.auth.broadcast.SSO_LOGOUT")
    private val broadcastPermission = "org.forgerock.android.auth.broadcast.SSO_PERMISSION"

    @Test
    fun `sendBroadcastEventWhenPermissionIsEnabled`() {

        val activityInfo = ActivityInfo()
        activityInfo.permission = broadcastPermission
        val resolveInfo = ResolveInfo()
        resolveInfo.activityInfo = activityInfo

        whenever(mockPackageManager.queryBroadcastReceivers(intent, 0)).thenReturn(listOf(resolveInfo))
        val testObject = SSOBroadcastModel(mockContext, broadcastPermission, intent)
        testObject.sendBroadcast()

        verify(mockContext).sendBroadcast(intent, broadcastPermission)
    }

    @Test
    fun `doNotSendBroadcastEventWhenPermissionIsNotEnabled`() {

        whenever(mockPackageManager.queryBroadcastReceivers(intent, 0)).thenReturn(listOf<ResolveInfo>())
        val testObject = SSOBroadcastModel(mockContext, broadcastPermission, intent)
        testObject.sendBroadcast()

        verify(mockContext, times(0)).sendBroadcast(intent, broadcastPermission)
    }

    @Test
    fun `doNotSendBroadcastEventWhenContextIsNull`() {

        val testObject = SSOBroadcastModel(null, null, intent)
        testObject.sendBroadcast()

        verify(mockContext, times(0)).sendBroadcast(intent, broadcastPermission)
        verifyNoInteractions(mockContext)
    }

    @Test
    fun `doNotSendBroadcastEventWhenPermissionIsDifferent`() {

        val activityInfo = ActivityInfo()
        activityInfo.permission = "errorPermission"
        val resolveInfo = ResolveInfo()
        resolveInfo.activityInfo = activityInfo
        whenever(mockPackageManager.queryBroadcastReceivers(intent, 0)).thenReturn(listOf(resolveInfo))

        val testObject = SSOBroadcastModel(mockContext, broadcastPermission, intent)
        testObject.sendBroadcast()

        verify(mockContext, times(0)).sendBroadcast(intent, broadcastPermission)
    }
}