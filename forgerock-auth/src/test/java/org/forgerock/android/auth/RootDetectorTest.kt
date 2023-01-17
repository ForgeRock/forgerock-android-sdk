/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.forgerock.android.auth.detector.RootDetector
import org.forgerock.android.auth.detector.FRRootDetector
import org.forgerock.android.auth.detector.BuildTagsDetector
import org.forgerock.android.auth.detector.DangerousPropertyDetector
import org.forgerock.android.auth.detector.NativeDetector
import org.forgerock.android.auth.detector.PermissionDetector
import org.forgerock.android.auth.detector.RootApkDetector
import org.forgerock.android.auth.detector.RootAppDetector
import org.forgerock.android.auth.detector.RootCloakingAppDetector
import org.forgerock.android.auth.detector.RootProgramFileDetector
import org.forgerock.android.auth.detector.RootRequiredAppDetector
import org.forgerock.android.auth.detector.SuCommandDetector
import org.forgerock.android.auth.detector.BusyBoxProgramFileDetector
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RootDetectorTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Test
    fun testBuildTagDetector() {
        val rootDetector: RootDetector = FRRootDetector.builder()
            .detector(BuildTagsDetector())
            .build()
        Assert.assertEquals(1.0, rootDetector.isRooted(context), 0.0001)
    }

    @Test
    fun testDangerousPropertyDetector() {
        val rootDetector: RootDetector = FRRootDetector.builder()
            .detector(DangerousPropertyDetector())
            .build()
        Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001)
    }

    @Test
    fun testNativeDetector() {
        val rootDetector: RootDetector = FRRootDetector.builder()
            .detector(NativeDetector())
            .build()
        Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001)
    }

    @Test
    fun testPermissionDetector() {
        val rootDetector: RootDetector = FRRootDetector.builder()
            .detector(PermissionDetector())
            .build()
        Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001)
    }

    @Test
    fun testRootApkDetector() {
        val rootDetector: RootDetector = FRRootDetector.builder()
            .detector(RootApkDetector())
            .build()
        Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001)
    }

    @Test
    fun testRootAppDetector() {
        val rootDetector: RootDetector = FRRootDetector.builder()
            .detector(RootAppDetector())
            .build()
        Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001)
    }

    @Test
    fun testRootCloakingAppDetector() {
        val rootDetector: RootDetector = FRRootDetector.builder()
            .detector(RootCloakingAppDetector())
            .build()
        Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001)
    }

    @Test
    fun testRootProgramFileDetector() {
        val rootDetector: RootDetector = FRRootDetector.builder()
            .detector(RootProgramFileDetector())
            .build()
        Assert.assertEquals(1.0, rootDetector.isRooted(context), 0.0001)
    }

    @Test
    fun testRootRequiredAppDetector() {
        val rootDetector: RootDetector = FRRootDetector.builder()
            .detector(RootRequiredAppDetector())
            .build()
        Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001)
    }

    @Test
    fun testSuCommandDetector() {
        val rootDetector: RootDetector = FRRootDetector.builder()
            .detector(SuCommandDetector())
            .build()
        Assert.assertEquals(1.0, rootDetector.isRooted(context), 0.0001)
    }

    @Test
    fun testBusyBoxProgramFileDetector() {
        val rootDetector: RootDetector = FRRootDetector.builder()
            .detector(BusyBoxProgramFileDetector())
            .build()
        Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001)
    }

    @Test
    fun testPartialDetector() {
        val rootDetector: RootDetector = FRRootDetector.builder()
            .detector(DangerousPropertyDetector())
            .detector(NativeDetector())
            .detector(PermissionDetector())
            .detector(RootApkDetector())
            .detector(RootAppDetector())
            .detector(RootCloakingAppDetector())
            .detector(RootRequiredAppDetector())
            .detector(BusyBoxProgramFileDetector())
            .build()
        Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001)
    }
}