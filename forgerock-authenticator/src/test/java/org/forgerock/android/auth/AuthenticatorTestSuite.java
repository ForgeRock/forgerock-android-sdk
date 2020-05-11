/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Categories.class)
@Suite.SuiteClasses({
        AccountTest.class,
        AuthenticatorManagerTest.class,
        FRAClientTest.class,
        PushNotificationTest.class,
        OathCodeGeneratorTest.class,
        OathParserTest.class,
        OathTest.class,
        OathTokenCodeTest.class,
        OathFactoryTest.class,
        PushFactoryTest.class,
        NotificationFactoryTest.class,
        PushParserTest.class,
        PushResponderTest.class,
        PushTest.class,
        SortedListTest.class
})
public class AuthenticatorTestSuite {
}
