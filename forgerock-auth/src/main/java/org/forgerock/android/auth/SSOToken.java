/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import lombok.Getter;

@Getter
public class SSOToken extends Token {

    public SSOToken(String value) {
        super(value);
    }
}
