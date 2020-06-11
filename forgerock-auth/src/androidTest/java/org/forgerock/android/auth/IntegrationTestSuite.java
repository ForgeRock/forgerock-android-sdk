/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.callback.ChoiceCallbackTest;
import org.forgerock.android.auth.callback.ConfirmationCallbackTest;
import org.forgerock.android.auth.callback.DeviceProfileCallbackTest;
import org.forgerock.android.auth.callback.DeviceProfileCollectorCallbackAndroidTest;
import org.forgerock.android.auth.callback.NameCallbackTest;
import org.forgerock.android.auth.callback.PageCallback65Test;
import org.forgerock.android.auth.callback.PageCallbackTest;
import org.forgerock.android.auth.callback.PasswordCallbackTest;
import org.forgerock.android.auth.collector.FRDeviceIdentifierTest;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Categories.class)
@Suite.SuiteClasses({
        //Callback
        ChoiceCallbackTest.class,
        ConfirmationCallbackTest.class,
        DeviceProfileCallbackTest.class,
        NameCallbackTest.class,
        PasswordCallbackTest.class,
        PageCallbackTest.class,
        PageCallback65Test.class,

        //Other
        SetPersistentCookieTest.class,

        //Developer Facing
        FRUserTest.class,
        FRSessionTest.class

})
public class IntegrationTestSuite {
}
