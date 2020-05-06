/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The Mechanism model represents the two-factor way used for authentication.
 * Encapsulates the related settings, as well as an owning Account.
 */
public abstract class Mechanism extends ModelObject<Mechanism> {

    /** Unique identifier of the Mechanism */
    private String id;
    /** Uniquely identifiable UUID for current mechanism */
    private final String mechanismUID;
    /** Issuer of the account */
    private final String issuer;
    /** AccountName, or Username of the account for the issuer */
    private final String accountName;
    /** The type of the Mechanism */
    private final String type;
    /** The shared secret of the Mechanism */
    private final String secret;

    public static final String PUSH = "pushauth";
    public static final String OATH = "otpauth";

    /**
     * Base constructor which encapsulates common elements of all Mechanisms.
     * @param mechanismUID The ID used to identify the Mechanism to external systems.
     * @param issuer String value of issuer.
     * @param accountName String value of accountName or username.
     * @param type String value of the mechanism type.
     * @param secret String value of the shared secret.
     */
    protected Mechanism(String mechanismUID, String issuer, String accountName, String type,
                     String secret) {
        this.id = issuer + "-" + accountName + "-" + type;
        this.mechanismUID = mechanismUID;
        this.issuer = issuer;
        this.accountName = accountName;
        this.type = type;
        this.secret = secret;
    }

    /**
     * Gets the storage id of the Mechanism.
     * @return The unique identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * Get the mechanism unique Id.
     * @return The receiving Mechanism.
     */
    public String getMechanismUID() {
        return mechanismUID;
    }

    /**
     * Gets the name of the IDP that issued this account.
     * @return The name of the IDP.
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Returns the name of this Account.
     * @return The account name.
     */
    public String getAccountName() {
        return accountName;
    }

    /**
     * Get the string used to represent this type of mechanism.
     * @return The mechanism type as string.
     */
    public String getType() {
        return type;
    }

    /**
     * Get the string used to represent the shared secret of the mechanism.
     * @return The shared secret as string.
     */
    public String getSecret() {
        return secret;
    }

    @Override
    public abstract String toJson();

    /**
     * Deserializes the specified Json into an object of the {@link Mechanism} object.
     * @param jsonString the json string representing the object to be deserialized
     * @return an {@link Mechanism} object from the string. Returns {@code null} if {@code jsonString} is {@code null},
     * if {@code jsonString} is empty or not able to parse it.
     */
    public static Mechanism fromJson(String jsonString) {
        Mechanism mechanism = null;
        if (jsonString == null || jsonString.length() == 0) {
            return null;
        }
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            String type = jsonObject.getString("type");
            if(type.equals(PUSH)) {
                mechanism = Push.fromJson(jsonString);
            } else if(type.equals(OATH)) {
                mechanism = Oath.fromJson(jsonString);
            }
        } catch (JSONException e) {
            return null;
        }
        return mechanism;
    }

    @Override
    public final boolean matches(Mechanism other) {
        if (other == null) {
            return false;
        }
        return other.issuer.equals(issuer) && other.accountName.equals(accountName) && other.type.equals(type);
    }

    @Override
    public int compareTo(Mechanism other) {
        return type.compareTo(other.type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Mechanism mechanism = (Mechanism) o;

        if (!mechanismUID.equals(mechanism.mechanismUID)) return false;
        if (!issuer.equals(mechanism.issuer)) return false;
        if (!accountName.equals(mechanism.accountName)) return false;
        return type.equals(mechanism.type);
    }

    @Override
    public int hashCode() {
        int result = mechanismUID.hashCode();
        result = 31 * result + issuer.hashCode();
        result = 31 * result + accountName.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

}
