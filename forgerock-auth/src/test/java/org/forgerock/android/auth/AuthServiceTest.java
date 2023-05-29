/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.junit.Test;

public class AuthServiceTest {

    @Test(expected = IllegalArgumentException.class)
    public void testMissingTreeName() {
        AuthService.builder()
                .build();

    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithoutServiceNameAndAdviceAndResumeURI() {
        AuthService.builder()
                .build();
    }
}