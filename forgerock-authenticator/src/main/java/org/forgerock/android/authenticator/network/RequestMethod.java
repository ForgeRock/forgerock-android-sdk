/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator.network;

/**
 * The enum of HTTP Request methods supported by the SDK.
 */
public enum RequestMethod {
    /**
     * The GET method retrieves whatever information is identified by the Request-URI.
     */
    GET,
    /**
     * The POST method is used to submit an entity to the specified resource identified by
     * the Request-URI.
     */
    POST
}
