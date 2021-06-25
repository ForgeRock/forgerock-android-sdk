/*
 * Copyright (c) 2019 - 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.callback.*;
import org.forgerock.android.auth.webauthn.WebAuthnAuthenticationTest;
import org.forgerock.android.auth.webauthn.WebAuthnRegistrationTest;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Categories.class)
@Suite.SuiteClasses({

        ServerConfigTest.class,
        ChoiceCallbackTest.class,
        ConfirmationCallbackTest.class,
        LocationCallbackTest.class,
        NameCallbackTest.class,
        PasswordCallbackTest.class,
        PollingWaitCallbackTest.class,
        TextOutputCallbackTest.class,
        ReCaptchaCallbackTest.class,
        ConsentMappingCallbackTest.class,
        DeviceProfileCollectorCallbackTest.class,
        MetadataCallbackTest.class,
        StringAttributeInputCallbackTest.class,
        BooleanAttributeInputCallbackTest.class,
        NumberAttributeInputCallbackTest.class,
        SelectIdPCallbackTest.class,
        IdPCallbackTest.class,


        AuthServiceMockTest.class,
        AuthServiceTest.class,
        OAuth2MockTest.class,
        AccessTokenTest.class,

        FRAuthMockTest.class,
        FRAuthRegistrationMockTest.class,

        DefaultTokenManagerTest.class,
        DefaultSingleSignOnManagerTest.class,

        FRUserMockTest.class,
        FRSessionMockTest.class,
        RootDetectorTest.class,
        FRDeviceTest.class,

        PersistentCookieTest.class,
        PolicyAdviceTest.class,

        BrowserLoginTest.class,

        //WebAuthn
        WebAuthnFlowTest.class,
        WebAuthnRegistrationTest.class,
        WebAuthnAuthenticationTest.class,
        AccountDataRepositoryTest.class,
        SharedPreferencesDataRepositoryTest.class,
        WebAuthnDataRepositoryTest.class,

        //SocialLogin
        SocialLoginTest.class,

})
public class AuthTestSuite {
}
