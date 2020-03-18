/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

import org.forgerock.android.authenticator.util.SortedList;

import java.util.List;

/**
 * Represents an instance of a Push authentication mechanism. Associated with an Account.
 */
public class Push extends Mechanism {

    /** The endpoint used for registration */
    public static final String REG_ENDPOINT_KEY = "r";
    /** The endpoint used for authentication */
    public static final String AUTH_ENDPOINT_KEY = "a";
    /** The message id to use for response */
    public static final String MESSAGE_ID_KEY = "m";
    /** The shared secret used for signing */
    public static final String BASE_64_SHARED_SECRET_KEY = "s";
    /** The challenge to use for the response */
    public static final String BASE_64_CHALLENGE_KEY = "c";
    /** The challenge to use for the response */
    public static final String AM_LOAD_BALANCER_COOKIE_KEY = "l";


    /** The registration URL for Push mechanism */
    private String registrationEndpoint;
    /** The authentication URL for Push mechanism */
    private String authenticationEndpoint;
    /** Notifications objects associated with this push mechanism */
    private final List<Notification> notifications;

    private static final String TAG = Push.class.getSimpleName();

    /**
     * Creates a Push mechanism with given data
     * @param mechanismUID Mechanism UUID
     * @param issuer issuer of the Mechanism
     * @param accountName accountName of the Mechanism
     * @param type Type of Mechanism
     * @param registrationEndpoint registration URL for Push
     * @param authenticationEndpoint authentication URL for Push
     */
    public Push(String mechanismUID, String issuer, String accountName, String type,
                String registrationEndpoint, String authenticationEndpoint) {
        super(mechanismUID, issuer, accountName, type);
        this.registrationEndpoint = registrationEndpoint;
        this.authenticationEndpoint = authenticationEndpoint;
        this.notifications = new SortedList<>();
    }

    /**
     * The registration URL for Push mechanism
     * @return String representing the registration URL
     */
    public String getRegistrationEndpoint() {
        return registrationEndpoint;
    }

    /**
     * The authentication URL for Push mechanism
     * @return String representing the authentication URL
     */
    public String getAuthenticationEndpoint() {
        return authenticationEndpoint;
    }
}
