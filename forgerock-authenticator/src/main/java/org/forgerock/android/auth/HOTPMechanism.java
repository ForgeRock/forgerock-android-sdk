/*
 * Copyright (c) 2020 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.exception.AccountLockException;
import org.forgerock.android.auth.exception.MechanismCreationException;
import org.forgerock.android.auth.exception.OathMechanismException;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * Represents HMAC-based OTP authentication mechanism and is responsible for its related operation.
 */
public class HOTPMechanism extends OathMechanism {

    /** Counter as in Int for number of OTP credentials generated */
    protected long counter;

    private HOTPMechanism(String mechanismUID, String issuer, String accountName, String type, TokenType oathType,
                          String algorithm, String secret, int digits, long counter, Calendar timeAdded) {
        super(mechanismUID, issuer, accountName, type, oathType, algorithm,
                secret, digits, timeAdded);
        this.counter = counter;
    }

    /**
     * Returns the value of the counter of this OathMechanism.
     * @return counter as long value
     */
    long getCounter() {
        return counter;
    }

    void incrementCounter() {
        counter++;
    }

    @Override
    public OathTokenCode getOathTokenCode() throws OathMechanismException, AccountLockException {
        return OathCodeGenerator.getInstance().generateNextCode(this, timeKeeper);
    }

    @Override
    public String toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", getId());
            jsonObject.put("issuer", getIssuer());
            jsonObject.put("accountName", getAccountName());
            jsonObject.put("mechanismUID", getMechanismUID());
            jsonObject.put("secret", getSecret());
            jsonObject.put("type", getType());
            jsonObject.put("oathType", getOathType());
            jsonObject.put("algorithm", getAlgorithm());
            jsonObject.put("digits", getDigits());
            jsonObject.put("counter", getCounter());
            jsonObject.put("timeAdded", getTimeAdded() != null ? getTimeAdded().getTimeInMillis() : null);
        } catch (JSONException e) {
            throw new RuntimeException("Error parsing PushMechanism object to JSON string representation.", e);
        }
        return jsonObject.toString();
    }

    @Override
    String serialize() {
        return this.toJson();
    }

    /**
     * Deserializes the specified Json into an object of the {@link HOTPMechanism} object.
     * @param jsonString the json string representing the object to be deserialized
     * @return an {@link HOTPMechanism} object from the string. Returns {@code null} if {@code jsonString} is {@code null},
     * if {@code jsonString} is empty or not able to parse it.
     */
    public static HOTPMechanism deserialize(String jsonString) {
        if (jsonString == null || jsonString.length() == 0) {
            return null;
        }
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            return (HOTPMechanism) HOTPMechanism.builder()
                    .setIssuer(jsonObject.getString("issuer"))
                    .setAccountName(jsonObject.getString("accountName"))
                    .setMechanismUID(jsonObject.getString("mechanismUID"))
                    .setSecret(jsonObject.getString("secret"))
                    .setAlgorithm(jsonObject.getString("algorithm"))
                    .setDigits(jsonObject.getInt("digits"))
                    .setCounter(jsonObject.getLong("counter"))
                    .setTimeAdded(jsonObject.has("timeAdded") ? getDate(jsonObject.optLong("timeAdded")) : null)
                    .build();
        } catch (JSONException | MechanismCreationException e) {
            return null;
        }
    }



    /**
     * Returns a builder for creating a HOTPMechanism Mechanism.
     * @return The OathMechanism builder.
     */
    public static HOTPBuilder builder() {
        return new HOTPBuilder();
    }

    /**
     * Builder class responsible for producing a TOTPMechanism Token.
     */
    public static class HOTPBuilder extends OathBuilder<HOTPBuilder>{
        private long counter;

        @Override
        protected HOTPBuilder getThis() {
            return this;
        }

        /**
         * Sets the counter for the OTP. Only useful for HOTPMechanism.
         * @param counter counter as an long number.
         * @return The current builder.
         */
        public HOTPBuilder setCounter(long counter) {
            this.counter = counter;
            return this;
        }

        /**
         * Produce the described OathMechanism Token.
         * @return The built Token.
         */
        @Override
        HOTPMechanism buildOath() {
            return new HOTPMechanism(mechanismUID, issuer, accountName, Mechanism.OATH, TokenType.HOTP, algorithm,
                    secret, digits, counter, timeAdded);
        }

    }

}
