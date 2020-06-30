/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.os.Build;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

public class AndroidVersionAwareTestRunner extends BlockJUnit4ClassRunner {
    public AndroidVersionAwareTestRunner(Class klass) throws InitializationError {
        super(klass);
    }

    @Override
    public void runChild(FrameworkMethod method, RunNotifier notifier) {
        TargetApi condition = method.getAnnotation(TargetApi.class);
        if(condition == null ||  Build.VERSION.SDK_INT >= condition.value()) {
            super.runChild(method, notifier);
        } else {
            notifier.fireTestIgnored(describeChild(method));
        }
    }
}
