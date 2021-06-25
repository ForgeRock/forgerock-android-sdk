/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.idp;

import androidx.fragment.app.Fragment;

import org.forgerock.android.auth.FRListener;

/**
 * Identity Provider Handler to handle sign in with provided {@link IdPClient}
 */
public interface IdPHandler {

    String ID_TOKEN = "id_token";
    String AUTHORIZATION_CODE = "authorization_code";
    String ACCESS_TOKEN = "access_token";
    String IDP_CLIENT = "IDP_CLIENT";

    /**
     * Retrieve the result token type (access_token, id_token, authorization_code)
     *
     * @return The Token Type
     */
    String getTokenType();

    /**
     * Perform the Identity Provider sign in with the current active
     * {@link androidx.fragment.app.FragmentActivity}
     *
     * @param idPClient The Idp configuration.
     * @param listener  Listener to listen for the result.
     */
    void signIn(IdPClient idPClient, FRListener<IdPResult> listener);

    /**
     * Perform the Identity Provider sign in with the current active Fragment
     *
     * @param fragment  The Active Fragment
     * @param idPClient The Idp configuration.
     * @param listener  Listener to listen for the result.
     */
    void signIn(Fragment fragment, IdPClient idPClient, FRListener<IdPResult> listener);
}
