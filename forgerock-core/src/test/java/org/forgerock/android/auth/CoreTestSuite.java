/*
 * Copyright (c) 2019 - 2022 ForgeRock. All rights reserved.
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

        LoggerTest.class,
        KeyStoreManagerTest.class,
        RequestInterceptorTest.class,
        NetworkConfigTest.class,

})
public class CoreTestSuite {
}
