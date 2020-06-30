/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.callback.DeviceProfileCollectorCallbackAndroidTest;
import org.forgerock.android.auth.collector.FRDeviceIdentifierTest;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Categories.class)
@Suite.SuiteClasses({
        DefaultTokenManagerTest.class,
        DefaultSingleSignOnManagerTest.class,
        FRDeviceIdentifierTest.class,
        FRDeviceProfileTest.class,
        DeviceProfileCollectorCallbackAndroidTest.class,

})
public class AuthInstrumentTestSuite {
}
