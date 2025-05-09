/*
 * Copyright (c) 2020 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * The Mechanism model represents the two-factor way used for authentication.
 * Encapsulates the related settings, as well as an owning Account.
 */
public abstract class Mechanism extends ModelObject<Mechanism> {

    /** Unique identifier of the Mechanism */
    private String id;
    /** Uniquely identifiable UUID for current mechanism on the client */
    private final String mechanismUID;
    /** Issuer of the account */
    private final String issuer;
    /** AccountName, or Username of the account for the issuer */
    private final String accountName;
    /** The type of the Mechanism */
    private final String type;
    /** The shared secret of the Mechanism */
    private final String secret;
    /** The unique identifier of this mechanism on the server */
    private final String resourceId;
    /** Date this object was stored */
    private final Calendar timeAdded;
    /** The unique identifier of the user associated with this mechanism */
    private String uid;
    /** The Account associated with this mechanism **/
    private Account account;


    /** URI scheme for PUSH registration */
    public static final String PUSH = "pushauth";
    /** URI scheme for OATH registration */
    public static final String OATH = "otpauth";
    /** URI scheme for combined registration OATH and PUSH at once */
    public static final String MFAUTH = "mfauth";

    /**
     * Base constructor which encapsulates common elements of all Mechanisms.
     * @param mechanismUID The ID used to identify the Mechanism to external systems
     * @param issuer String value of issuer
     * @param accountName String value of accountName or username
     * @param type String value of the mechanism type
     * @param secret String value of the shared secret
     * @param uid The unique identifier of the user associated with this mechanism
     * @param resourceId The unique identifier of this mechanism on the server
     * @param timeAdded Date and Time this Mechanism was stored
     */
    protected Mechanism(String mechanismUID, String issuer, String accountName, String type,
                        String secret, String uid, String resourceId, Calendar timeAdded) {
        this.id = issuer + "-" + accountName + "-" + type;
        this.mechanismUID = mechanismUID;
        this.issuer = issuer;
        this.accountName = accountName;
        this.type = type;
        this.secret = secret;
        this.uid = uid;
        this.resourceId = resourceId;
        this.timeAdded = timeAdded;
    }

    /**
     * Gets the storage id of the Mechanism.
     * @return The unique identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the storage Account id associated with this Mechanism.
     * @return The Account unique identifier.
     */
    public String getAccountId() {
        return issuer + "-" + accountName;
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
     * Get the Date and Time this mechanism was stored.
     * @return when this mechanism was stored.
     */
    public Calendar getTimeAdded() {
        return timeAdded;
    }

    /**
     * Get the string used to represent the shared secret of the mechanism.
     * @return The shared secret as string.
     */
    String getSecret() {
        return secret;
    }

    /**
     * Get the unique identifier of the user associated with this mechanism.
     * @return The unique identifier of the user.
     */
    String getUid() {
        return uid;
    }

    /**
     * Sets the unique identifier of the user associated with this mechanism.
     * @param uid The unique identifier of the user.
     */
    void setUid(String uid) {
        this.uid = uid;
    }

    /**
     * Get the unique identifier of this mechanism on the server.
     * @return The unique identifier of this mechanism on the server.
     */
    String getResourceId() {
        return resourceId;
    }

    /**
     * Gets the account object associated with the mechanism.
     * @return the Account object.
     */
    Account getAccount() {
        return account;
    }

    /**
     * Sets the account object associated with the mechanism.
     * @param account the Account object.
     */
    void setAccount(Account account) {
        this.account = account;
    }

    @Override
    public abstract String toJson();

    @Override
    abstract String serialize();

    /**
     * Deserializes the specified Json into an object of the {@link Mechanism} object.
     * @param jsonString the json string representing the object to be deserialized
     * @return an {@link Mechanism} object from the string. Returns {@code null} if {@code jsonString} is {@code null},
     * if {@code jsonString} is empty or not able to parse it.
     */
    public static Mechanism deserialize(String jsonString) {
        Mechanism mechanism = null;
        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            String type = jsonObject.getString("type");
            if(type.equals(PUSH)) {
                mechanism = PushMechanism.deserialize(jsonString);
            } else if(type.equals(OATH)) {
                mechanism = OathMechanism.deserialize(jsonString);
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
    public int compareTo(Mechanism another) {
        return getType().compareTo(another.getType());
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
