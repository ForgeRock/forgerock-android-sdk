/*
 *
 *  * Copyright (c) 2022 ForgeRock. All rights reserved.
 *  *
 *  * This software may be modified and distributed under the terms
 *  * of the MIT license. See the LICENSE file for details.
 *
 *
 */

package org.forgerock.android.auth

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import org.junit.Test
import org.mockito.kotlin.*

class SSOBroadcastModelTest {

    private val mockPackageManager = mock<PackageManager>()
    private val packageName = "com.test.forgerock"
    private val broadcastPermission = "org.forgerock.android.auth.broadcast.SSO_PERMISSION"
    private val broadcastAction = "org.forgerock.android.auth.broadcast.SSO_LOGOUT"

    private val resources = mock<android.content.res.Resources>()
    private val mockContext = mock<Context> {
        on { packageManager } doReturn mockPackageManager
        on { packageName } doReturn packageName
        on { resources } doReturn resources
        on { resources.getString(R.string.forgerock_sso_permission) } doReturn broadcastPermission
        on { resources.getString(R.string.forgerock_sso_logout) } doReturn broadcastAction
    }

    private val intent = mock<Intent>()

    @Test
    fun sendBroadcastEventWhenPermissionIsEnabled() {

        val activityInfo = ActivityInfo()
        activityInfo.permission = broadcastPermission
        val resolveInfo = ResolveInfo()
        resolveInfo.activityInfo = activityInfo

        whenever(mockPackageManager.queryBroadcastReceivers(intent, 0)).thenReturn(listOf(resolveInfo))

        val testObject = SSOBroadcastModel(mockContext, intent)
        testObject.sendLogoutBroadcast()

        verify(intent).putExtra("BROADCAST_PACKAGE_KEY", packageName)
        verify(intent).flags = Intent.FLAG_RECEIVER_FOREGROUND
        verify(mockContext).sendBroadcast(intent, broadcastPermission)

    }

    @Test
    fun doNotSendBroadcastEventWhenPermissionIsNotEnabled() {

        whenever(mockPackageManager.queryBroadcastReceivers(intent, 0)).thenReturn(listOf<ResolveInfo>())
        val testObject = SSOBroadcastModel(mockContext, intent)
        testObject.sendLogoutBroadcast()

        verify(mockContext, times(0)).sendBroadcast(intent, broadcastPermission)
    }

    @Test
    fun doNotSendBroadcastEventWhenContextIsNull() {

        val testObject = SSOBroadcastModel(null, Intent())
        testObject.sendLogoutBroadcast()

        verify(mockContext, times(0)).sendBroadcast(intent, broadcastPermission)
    }

    @Test
    fun doNotSendBroadcastEventWhenPermissionIsDifferent() {

        val activityInfo = ActivityInfo()
        activityInfo.permission = "errorPermission"
        val resolveInfo = ResolveInfo()
        resolveInfo.activityInfo = activityInfo
        whenever(mockPackageManager.queryBroadcastReceivers(intent, 0)).thenReturn(listOf(resolveInfo))

        val testObject = SSOBroadcastModel(mockContext, intent)
        testObject.sendLogoutBroadcast()

        verify(mockContext, times(0)).sendBroadcast(intent, broadcastPermission)
    }
}