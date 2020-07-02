/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.callback.BooleanAttributeInputCallbackTest;
import org.forgerock.android.auth.callback.ChoiceCallbackTest;
import org.forgerock.android.auth.callback.ConfirmationCallbackTest;
import org.forgerock.android.auth.callback.ConsentMappingCallbackTest;
import org.forgerock.android.auth.callback.DeviceProfileCallbackTest;
import org.forgerock.android.auth.callback.HiddenValueCallbackTest;
import org.forgerock.android.auth.callback.KbaCreateCallbackTest;
import org.forgerock.android.auth.callback.NameCallbackTest;
import org.forgerock.android.auth.callback.NumberAttributeInputCallbackTest;
import org.forgerock.android.auth.callback.PageCallback65Test;
import org.forgerock.android.auth.callback.PageCallbackTest;
import org.forgerock.android.auth.callback.PasswordCallbackTest;
import org.forgerock.android.auth.callback.PollingWaitCallbackTest;
import org.forgerock.android.auth.callback.ReCaptchaCallbackTest;
import org.forgerock.android.auth.callback.StringAttributeInputCallbackTest;
import org.forgerock.android.auth.callback.TermsAndConditionCallbackTest;
import org.forgerock.android.auth.callback.ValidatedCreateUsernameCallbackTest;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Categories.class)
@Suite.SuiteClasses({
        //Callback
        BooleanAttributeInputCallbackTest.class,
        ChoiceCallbackTest.class,
        ConfirmationCallbackTest.class,
        ConsentMappingCallbackTest.class,
        DeviceProfileCallbackTest.class,
        HiddenValueCallbackTest.class,
        KbaCreateCallbackTest.class,
        NameCallbackTest.class,
        NumberAttributeInputCallbackTest.class,
        PageCallback65Test.class,
        PageCallbackTest.class,
        PasswordCallbackTest.class,
        PollingWaitCallbackTest.class,
        ReCaptchaCallbackTest.class,
        StringAttributeInputCallbackTest.class,
        TermsAndConditionCallbackTest.class,
        ValidatedCreateUsernameCallbackTest.class,

        //Other
        SetPersistentCookieTest.class,

        //Developer Facing
        FRUserTest.class,
        FRSessionTest.class

})
public class IntegrationTestSuite {
}
