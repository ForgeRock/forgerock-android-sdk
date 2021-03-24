/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.idp;

import java.io.Serializable;
import java.util.List;

/**
 * Interface for Identity Provider Configuration
 */
public interface IdPClient extends Serializable {

    String getProvider();

    String getClientId();

    String getRedirectUri();

    List<String> getScopes();

    String getNonce();

    List<String> getAcrValues();

    String getRequest();

    String getRequestUri();
}
