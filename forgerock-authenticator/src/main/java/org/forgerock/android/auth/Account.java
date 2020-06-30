/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.List;

/**
 * Account model represents an identity for the user with an issuer. It is possible for a user to
 * have multiple accounts provided by a single issuer, however it is not possible to have multiple
 * accounts from with same issuer for the same account name.
 */
public class Account extends ModelObject<Account> {

    /** Unique identifier of Account */
    private final String id;
    /** Issuer of the account */
    private final String issuer;
    /** AccountName, or Username of the account for the issuer */
    private final String accountName;
    /** URL of Account's logo image */
    private final String imageURL;
    /** HEX Color code in String for Account */
    private final String backgroundColor;
    /** Date this object was stored */
    private final Calendar timeAdded;
    /** List of Mechanism objects associated with this account **/
    private List<Mechanism> mechanismList;

    /**
     * Creates Account object with given information.
     * @param issuer String value of issuer
     * @param accountName String value of accountName or username
     * @param imageURL URL of account's logo image (optional)
     * @param backgroundColor String HEX code of account's background color (optional)
     * @param timeAdded Date and Time this Account was stored
     */
    private Account(String issuer, String accountName, String imageURL, String backgroundColor,
                    Calendar timeAdded) {
        this.id = issuer + "-" + accountName;
        this.issuer = issuer;
        this.accountName = accountName;
        this.imageURL = imageURL;
        this.backgroundColor = backgroundColor;
        this.timeAdded = timeAdded;
    }

    /**
     * Returns a builder for creating an Account.
     * @return The Account builder.
     */
    public static AccountBuilder builder() {
        return new AccountBuilder();
    }

    /**
     * Gets the unique identifier for the Account.
     * @return The unique identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the name of the IDP that issued this account.
     * @return The name of the IDP.
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Returns the name of this account.
     * @return The account name.
     */
    public String getAccountName() {
        return accountName;
    }

    /**
     * Gets the image URL for the IDP that issued this account.
     * @return String representing the path to the image, or null if not assigned.
     */
    public String getImageURL() {
        return imageURL;
    }

    /**
     * Gets the background color for the IDP that issued this account.
     * @return A hex string including a prepending # symbol, representing the color (e.g. #aabbcc)
     */
    public String getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Get the Date and Time this Account was stored.
     * @return when this account was stored.
     */
    public Calendar getTimeAdded() {
        return timeAdded;
    }

    /**
     * Get the list of mechanisms associates with this account.
     * @return List<Mechanism> list of mechanisms
     */
    public List<Mechanism> getMechanisms() {
        return mechanismList;
    }

    void setMechanismList(List<Mechanism> mechanismList) {
        this.mechanismList = mechanismList;
    }

    @Override
    String serialize() {
        return this.toJson();
    }

    @Override
    public String toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", getId());
            jsonObject.put("issuer", getIssuer());
            jsonObject.put("accountName", getAccountName());
            jsonObject.put("imageURL", getImageURL());
            jsonObject.put("backgroundColor", getBackgroundColor());
            jsonObject.put("timeCreated", getTimeAdded());
        } catch (JSONException e) {
            throw new RuntimeException("Error parsing Account object to JSON string representation.", e);
        }
        return jsonObject.toString();
    }

    /**
     * Deserializes the specified Json into an object of the {@link Account} object.
     * @param jsonString the json string representing the object to be deserialized
     * @return an {@link Account} object from the string. Returns {@code null} if {@code jsonString} is {@code null},
     * if {@code jsonString} is empty or not able to parse it.
     */
    static Account deserialize(String jsonString) {
        if (jsonString == null || jsonString.length() == 0) {
            return null;
        }
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            return Account.builder()
                    .setIssuer(jsonObject.getString("issuer"))
                    .setAccountName(jsonObject.getString("accountName"))
                    .setImageURL(jsonObject.has("imageURL") ? jsonObject.getString("imageURL"): null)
                    .setBackgroundColor(jsonObject.has("backgroundColor") ? jsonObject.getString("backgroundColor"): null)
                    .setTimeCreated(jsonObject.has("timeCreated") ? getDate(jsonObject.optLong("timeCreated")) : null)
                    .build();
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public boolean matches(Account other) {
        if (other == null) {
            return false;
        }
        return other.issuer.equals(issuer) && other.accountName.equals(accountName);
    }

    @Override
    public int compareTo(Account another) {
        if (another == null) {
            return -1;
        }
        int compareIssuer = issuer.compareTo(another.issuer);
        if (compareIssuer == 0) {
            return accountName.compareTo(another.accountName);
        }
        return compareIssuer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Account account = (Account) o;

        if (!issuer.equals(account.issuer)) return false;
        if (!accountName.equals(account.accountName)) return false;
        if (imageURL != null ? !imageURL.equals(account.imageURL) : account.imageURL != null)
            return false;
        return backgroundColor != null ? backgroundColor.equals(account.backgroundColor) : account.backgroundColor == null;
    }

    @Override
    public int hashCode() {
        int result = issuer.hashCode();
        result = 31 * result + accountName.hashCode();
        result = 31 * result + (imageURL != null ? imageURL.hashCode() : 0);
        result = 31 * result + (backgroundColor != null ? backgroundColor.hashCode() : 0);
        return result;
    }

    /**
     * Builder class responsible for producing Accounts.
     */
    public static class AccountBuilder {
        private String issuer = "";
        private String accountName = "";
        private String imageURL;
        private String backgroundColor;
        private Calendar timeCreated;

        /**
         * Sets the name of the IDP that issued this account.
         * @param issuer The IDP name.
         */
        public AccountBuilder setIssuer(String issuer) {
            this.issuer = issuer != null ? issuer : "";
            return this;
        }

        /**
         * Sets the name of the account.
         * @param accountName The account name.
         */
        public AccountBuilder setAccountName(String accountName) {
            this.accountName = accountName != null ? accountName : "";
            return this;
        }

        /**
         * Sets the imageURL for the IDP that issued this account.
         * @param imageURL A string that represents the image URI.
         */
        public AccountBuilder setImageURL(String imageURL) {
            this.imageURL = imageURL;
            return this;
        }

        /**
         * Sets the background color for the IDP that issued this account.
         * @param color A hex string including a prepending # symbol, representing the color (e.g. #aabbcc).
         */
        public AccountBuilder setBackgroundColor(String color) {
            backgroundColor = color;
            return this;
        }

        /**
         * Sets the Date and Time this Account was stored.
         * @param timeCreated when this account was stored.
         */
        public AccountBuilder setTimeCreated(Calendar timeCreated) {
            this.timeCreated = timeCreated;
            return this;
        }

        /**
         * Produces the Account object that was being constructed.
         * @return The account.
         */
        protected Account build() {
            return new Account(issuer, accountName, imageURL, backgroundColor, timeCreated);
        }
    }
}
