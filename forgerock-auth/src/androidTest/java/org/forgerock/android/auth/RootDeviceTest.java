/*
 * Copyright (c) 2019 - 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.os.Build;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.forgerock.android.auth.detector.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.os.Build.VERSION.SDK_INT;

@RunWith(AndroidJUnit4.class)
public class RootDeviceTest extends AndroidBaseTest {

    @Test
    public void testBuildTagDetector() {
        RootDetector rootDetector = FRRootDetector.builder()
                .detector(new BuildTagsDetector())
                .build();
        Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001);
    }

    @Test
    public void testDangerousPropertyDetector() {
        RootDetector rootDetector = FRRootDetector.builder()
                .detector(new DangerousPropertyDetector())
                .build();
        //Expect 1.0 on emulator
        if (isEmulator()) {
            Assert.assertEquals(1.0, rootDetector.isRooted(context), 0.0001);
        } else {
            Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001);
        }
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
        //expect 1.0 on emulator
        if (isEmulator() && SDK_INT < 30) {
            Assert.assertEquals(1.0, rootDetector.isRooted(context), 0.0001);
        } else {
            Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001);
        }
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
        Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001);
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
                .detector(new BuildTagsDetector())
                .detector(new NativeDetector())
                .detector(new PermissionDetector())
                .detector(new RootApkDetector())
                .detector(new RootAppDetector())
                .detector(new RootCloakingAppDetector())
                .detector(new RootRequiredAppDetector())
                .detector(new BusyBoxProgramFileDetector())
                .build();
        Assert.assertEquals(0.0, rootDetector.isRooted(context), 0.0001);
    }

    @Test
    public void testDefault() {
        if (!isEmulator()) {
            Assert.assertEquals(0.0, FRRootDetector.DEFAULT.isRooted(context), 0.0001);
        }
    }

    private boolean isEmulator() {
        return Build.PRODUCT.matches(".*_?sdk_?.*");
    }
}
