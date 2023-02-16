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
 * Represents Time-based OTP authentication mechanism and is responsible for its related operation.
 */
public class TOTPMechanism extends OathMechanism {

    /** The frequency with which the OTP changes */
    private int period;

    private TOTPMechanism(String mechanismUID, String issuer, String accountName, String type, TokenType oathType,
                          String algorithm, String secret, int digits, int period, Calendar timeAdded) {
        super(mechanismUID, issuer, accountName, type, oathType, algorithm,
                secret, digits, timeAdded);
        this.period = period;
    }

    /**
     * Returns the period of this OathMechanism. The frequency with which the OTP changes in seconds
     * @return period as long value
     */
    public long getPeriod() {
        return period;
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
            jsonObject.put("period", getPeriod());
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
     * Deserializes the specified Json into an object of the {@link TOTPMechanism} object.
     * @param jsonString the json string representing the object to be deserialized
     * @return an {@link TOTPMechanism} object from the string. Returns {@code null} if {@code jsonString} is {@code null},
     * if {@code jsonString} is empty or not able to parse it.
     */
    public static TOTPMechanism deserialize(String jsonString) {
        if (jsonString == null || jsonString.length() == 0) {
            return null;
        }
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            return (TOTPMechanism) TOTPMechanism.builder()
                    .setIssuer(jsonObject.getString("issuer"))
                    .setAccountName(jsonObject.getString("accountName"))
                    .setMechanismUID(jsonObject.getString("mechanismUID"))
                    .setSecret(jsonObject.getString("secret"))
                    .setAlgorithm(jsonObject.getString("algorithm"))
                    .setDigits(jsonObject.getInt("digits"))
                    .setPeriod(jsonObject.getInt("period"))
                    .setTimeAdded(jsonObject.has("timeAdded") ? getDate(jsonObject.optLong("timeAdded")) : null)
                    .build();
        } catch (JSONException | MechanismCreationException e) {
            return null;
        }
    }

    /**
     * Returns a builder for creating a TOTPMechanism Mechanism.
     * @return The OathMechanism builder.
     */
    public static TOTPBuilder builder() {
        return new TOTPBuilder();
    }

    /**
     * Builder class responsible for producing a TOTPMechanism Token.
     */
    public static class TOTPBuilder extends OathBuilder<TOTPBuilder>{
        private int period;

        @Override
        protected TOTPBuilder getThis() {
            return this;
        }

        /**
         * Sets the frequency with which the OTP changes.
         * @param period Non null period in seconds.
         * @return The current builder.
         */
        public TOTPBuilder setPeriod(int period) {
            this.period = period;
            return this;
        }

        /**
         * Produce the described OathMechanism Token.
         * @return The built Token.
         */
        @Override
        TOTPMechanism buildOath() {
            return new TOTPMechanism(mechanismUID, issuer, accountName, Mechanism.OATH, TokenType.TOTP, algorithm,
                    secret, digits, period, timeAdded);
        }

    }

}
