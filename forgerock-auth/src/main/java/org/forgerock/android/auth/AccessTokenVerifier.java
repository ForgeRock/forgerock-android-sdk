/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

/**
 * Verifier to verify the Access Token is valid or not.
 */
public interface AccessTokenVerifier {

    boolean isValid(AccessToken accessToken);
}
