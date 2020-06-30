/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Represents an instance of a Push authentication mechanism. Associated with an Account.
 */
public class PushMechanism extends Mechanism {

    /** The registration URL for Push mechanism */
    private String registrationEndpoint;
    /** The authentication URL for Push mechanism */
    private String authenticationEndpoint;
    /** List of PushNotification objects associated with this mechanism **/
    private List<PushNotification> pushNotificationList;

    /**
     * Creates a Push mechanism with given data.
     * @param mechanismUID Mechanism UUID
     * @param issuer issuer of the Mechanism
     * @param accountName accountName of the Mechanism
     * @param type Type of Mechanism
     * @param secret String value of the shared secret
     * @param registrationEndpoint registration URL for Push
     * @param authenticationEndpoint authentication URL for Push
     * @param timeCreated Date and Time this Mechanism was stored
     */
    private PushMechanism(String mechanismUID, String issuer, String accountName, String type, String secret,
                          String registrationEndpoint, String authenticationEndpoint, Calendar timeCreated) {
        super(mechanismUID, issuer, accountName, type, secret, timeCreated);
        this.registrationEndpoint = registrationEndpoint;
        this.authenticationEndpoint = authenticationEndpoint;
    }

    /**
     * The registration URL for Push mechanism.
     * @return String representing the registration URL
     */
    String getRegistrationEndpoint() {
        return registrationEndpoint;
    }

    /**
     * The authentication URL for Push mechanism.
     * @return String representing the authentication URL
     */
    String getAuthenticationEndpoint() {
        return authenticationEndpoint;
    }

    /**
     * Get all of the notifications that belong to this Push mechanism.
     * @return The list of notifications.
     */
    public List<PushNotification> getAllNotifications() {
        return pushNotificationList;
    }

    /**
     * Get pending notifications that belong to this Push mechanism.
     * @return The list of notifications.
     */
    public List<PushNotification> getPendingNotifications() {
        List<PushNotification> pendingList = new ArrayList<>(pushNotificationList);
        for (PushNotification notification : pushNotificationList) {
            if (!notification.isPending()){
                pendingList.remove(notification);
            }
        }
        return pendingList;
    }

    void setPushNotificationList(List<PushNotification> pushNotificationList) {
        this.pushNotificationList = pushNotificationList;
    }

    @Override
    public String toJson() {
        return convertToJson(true);
    }

    @Override
    String serialize() {
        return convertToJson(false);
    }

    private String convertToJson(boolean excludeSensitiveData) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", getId());
            jsonObject.put("issuer", getIssuer());
            jsonObject.put("accountName", getAccountName());
            jsonObject.put("mechanismUID", getMechanismUID());
            jsonObject.put("secret", excludeSensitiveData ? "REMOVED" : getSecret());
            jsonObject.put("type", getType());
            jsonObject.put("registrationEndpoint", excludeSensitiveData ? "REMOVED" : getRegistrationEndpoint());
            jsonObject.put("authenticationEndpoint", excludeSensitiveData ? "REMOVED" : getAuthenticationEndpoint());
            jsonObject.put("timeCreated", getTimeAdded());
        } catch (JSONException e) {
            throw new RuntimeException("Error parsing PushMechanism object to JSON string representation.", e);
        }
        return jsonObject.toString();
    }

    /**
     * Deserializes the specified Json into an object of the {@link PushMechanism} object.
     * @param jsonString the json string representing the object to be deserialized
     * @return an {@link PushMechanism} object from the string. Returns {@code null} if {@code jsonString} is {@code null},
     * if {@code jsonString} is empty or not able to parse it.
     */
    static PushMechanism deserialize(String jsonString) {
        if (jsonString == null || jsonString.length() == 0) {
            return null;
        }
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            return PushMechanism.builder()
                    .setIssuer(jsonObject.getString("issuer"))
                    .setAccountName(jsonObject.getString("accountName"))
                    .setMechanismUID(jsonObject.getString("mechanismUID"))
                    .setSecret(jsonObject.getString("secret"))
                    .setRegistrationEndpoint(jsonObject.getString("registrationEndpoint"))
                    .setAuthenticationEndpoint(jsonObject.getString("authenticationEndpoint"))
                    .build();
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Returns a builder for creating the Push Mechanism.
     * @return The PushMechanism builder.
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
        private Calendar timeCreated;

        /**
         * Sets the mechanism unique Id.
         * @param mechanismUID the mechanism unique Id
         * @return The receiving Mechanism.
         */
        public PushBuilder setMechanismUID(String mechanismUID) {
            this.mechanismUID = mechanismUID;
            return this;
        }

        /**
         * Sets the name of the IDP that issued this account.
         * @param issuer The IDP name
         */
        public PushBuilder setIssuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        /**
         * Sets the name of the account.
         * @param accountName The account name
         */
        public PushBuilder setAccountName(String accountName) {
            this.accountName = accountName;
            return this;
        }

        /**
         * Set the endpoint that will be used for Push registration.
         * @param endpoint The endpoint to register the device for Push
         * @return This builder
         */
        public PushBuilder setRegistrationEndpoint(String endpoint) {
            this.registrationEndpoint = endpoint;
            return this;
        }

        /**
         * Set the endpoint that will be used for Push authentication
         * @param endpoint The endpoint to respond Push messages to.
         * @return This builder
         */
        public PushBuilder setAuthenticationEndpoint(String endpoint) {
            this.authenticationEndpoint = endpoint;
            return this;
        }

        /**
         * Set the secret that this mechanism shares with the server.
         * @param secret The shared secret
         * @return This builder
         */
        public PushBuilder setSecret(String secret) {
            this.secret = secret;
            return this;
        }

        /**
         * Sets the Date and Time this mechanism was stored.
         * @param timeCreated when this mechanism was stored.
         */
        public PushBuilder setTimeCreated(Calendar timeCreated) {
            this.timeCreated = timeCreated;
            return this;
        }

        /**
         * Produce the described Mechanism.
         * @return The built Token
         */
        protected PushMechanism build() {
            return new PushMechanism(mechanismUID, issuer, accountName, Mechanism.PUSH, secret,
                    registrationEndpoint, authenticationEndpoint, timeCreated);
        }

    }

}
