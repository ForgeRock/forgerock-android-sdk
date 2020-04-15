/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

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

    /**
     * Creates a Push mechanism with given data
     * @param mechanismUID Mechanism UUID
     * @param issuer issuer of the Mechanism
     * @param accountName accountName of the Mechanism
     * @param type Type of Mechanism
     * @param secret String value of the shared secret
     * @param registrationEndpoint registration URL for Push
     * @param authenticationEndpoint authentication URL for Push
     */
    private Push(String mechanismUID, String issuer, String accountName, String type, String secret,
                String registrationEndpoint, String authenticationEndpoint) {
        super(mechanismUID, issuer, accountName, type, secret);
        this.registrationEndpoint = registrationEndpoint;
        this.authenticationEndpoint = authenticationEndpoint;
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

    /**
     * Returns a builder for creating the Push Mechanism.
     * @return The Push builder.
     */
    public static PushBuilder builder() {
        return new PushBuilder();
    }

    /**
     * Builder class responsible for building a Push mechanism.
     */
    public static class PushBuilder {
        private String mechanismUID;
        private String issuer;
        private String accountName;
        private String registrationEndpoint;
        private String authenticationEndpoint;
        private String secret;

        /**
         * Sets the mechanism unique Id.
         * @param mechanismUID the mechanism unique Id.
         * @return The receiving Mechanism.
         */
        public PushBuilder setMechanismUID(String mechanismUID) {
            this.mechanismUID = mechanismUID;
            return this;
        }

        /**
         * Sets the name of the IDP that issued this account.
         * @param issuer The IDP name.
         */
        public PushBuilder setIssuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        /**
         * Sets the name of the account.
         * @param accountName The account name.
         */
        public PushBuilder setAccountName(String accountName) {
            this.accountName = accountName;
            return this;
        }

        /**
         * Set the endpoint that will be used for Push registration
         * @param endpoint The endpoint to register the device for Push.
         * @return This builder.
         */
        public PushBuilder setRegistrationEndpoint(String endpoint) {
            this.registrationEndpoint = endpoint;
            return this;
        }

        /**
         * Set the endpoint that will be used for Push authentication
         * @param endpoint The endpoint to respond Push messages to.
         * @return This builder.
         */
        public PushBuilder setAuthenticationEndpoint(String endpoint) {
            this.authenticationEndpoint = endpoint;
            return this;
        }

        /**
         * Set the secret that this mechanism shares with the server.
         * @param secret The shared secret.
         * @return This builder.
         */
        public PushBuilder setSecret(String secret) {
            this.secret = secret;
            return this;
        }

        /**
         * Produce the described Mechanism.
         * @return The built Token.
         */
        protected Push build() {

            return new Push(mechanismUID, issuer, accountName, Mechanism.PUSH, secret,
                    registrationEndpoint, authenticationEndpoint);
        }

    }

}
