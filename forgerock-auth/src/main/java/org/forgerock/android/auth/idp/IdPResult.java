/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.idp;

import java.util.Map;

import lombok.Getter;

/**
 * SignIn Result from the {@link IdPHandler}
 */
@Getter
public class IdPResult {

    private final String token;
    private final Map<String, String> additionalParameters;

    public IdPResult(String token, Map<String, String> additionalParameters) {
        this.token = token;
        this.additionalParameters = additionalParameters;
    }

    public IdPResult(String token) {
        this.token = token;
        this.additionalParameters = null;
    }

}
