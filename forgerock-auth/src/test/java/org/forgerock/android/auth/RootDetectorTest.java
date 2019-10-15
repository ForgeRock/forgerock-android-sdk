/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.forgerock.android.auth.detector.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class RootDetectorTest {

    private Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void testBuildTagDetector() {
        RootDetector rootDetector = FRRootDetector.builder()
                .detector(new BuildTagsDetector())
                .build();
        Assert.assertEquals(1.0, rootDetector.isRooted(context), 0.0001);
    }

    @Test
    public void testDangerousPropertyDetector() {
        RootDetector rootDetector = FRRootDetector.builder()
                .detector(new DangerousPropertyDetector())
                .build();
        Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001);
    }

    @Test
    public void testNativeDetector() {
        RootDetector rootDetector = FRRootDetector.builder()
                .detector(new NativeDetector())
                .build();
        Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001);
    }

    @Test
    public void testPermissionDetector() {
        RootDetector rootDetector = FRRootDetector.builder()
                .detector(new PermissionDetector())
                .build();
        Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001);
    }

    @Test
    public void testRootApkDetector() {
        RootDetector rootDetector = FRRootDetector.builder()
                .detector(new RootApkDetector())
                .build();
        Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001);
    }

    @Test
    public void testRootAppDetector() {
        RootDetector rootDetector = FRRootDetector.builder()
                .detector(new RootAppDetector())
                .build();
        Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001);
    }

    @Test
    public void testRootCloakingAppDetector() {
        RootDetector rootDetector = FRRootDetector.builder()
                .detector(new RootCloakingAppDetector())
                .build();
        Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001);
    }

    @Test
    public void testRootProgramFileDetector() {
        RootDetector rootDetector = FRRootDetector.builder()
                .detector(new RootProgramFileDetector())
                .build();
        Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001);
    }

    @Test
    public void testRootRequiredAppDetector() {
        RootDetector rootDetector = FRRootDetector.builder()
                .detector(new RootRequiredAppDetector())
                .build();
        Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001);
    }

    @Test
    public void testSuCommandDetector() {
        RootDetector rootDetector = FRRootDetector.builder()
                .detector(new SuCommandDetector())
                .build();
        Assert.assertEquals(1.0, rootDetector.isRooted(context), 0.0001);
    }

    @Test
    public void testBusyBoxProgramFileDetector() {
        RootDetector rootDetector = FRRootDetector.builder()
                .detector(new BusyBoxProgramFileDetector())
                .build();
        Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001);
    }

    @Test
    public void testPartialDetector() {
        RootDetector rootDetector = FRRootDetector.builder()
                .detector(new DangerousPropertyDetector())
                .detector(new NativeDetector())
                .detector(new PermissionDetector())
                .detector(new RootApkDetector())
                .detector(new RootAppDetector())
                .detector(new RootCloakingAppDetector())
                .detector(new RootProgramFileDetector())
                .detector(new RootRequiredAppDetector())
                .detector(new BusyBoxProgramFileDetector())
                .build();
        Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001);
    }
}
