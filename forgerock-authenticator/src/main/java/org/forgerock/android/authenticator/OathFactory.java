/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

import android.content.Context;

import org.forgerock.android.auth.Logger;
import org.forgerock.android.authenticator.exception.MechanismCreationException;
import org.forgerock.android.authenticator.util.Base32String;
import org.forgerock.android.authenticator.util.MapUtil;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Responsible for generating instances of {@link Oath}.
 *
 * Understands the concept of a version number associated with a Token
 * and will parse the URI according to this.
 */
public class OathFactory extends MechanismFactory {

    private final OathParser parser = new OathParser();

    private static final String TAG = OathFactory.class.getSimpleName();

    /**
     * Creates the MechanismFactory and loads the available mechanism information.
     *
     * @param context
     * @param storageClient
     */
    public OathFactory(Context context, StorageClient storageClient) {
        super(context, storageClient);
    }

    @Override
    protected Mechanism createFromUriParameters(
            int version, String mechanismUID, Map<String, String> map) throws MechanismCreationException {
        if (version == 1) {
            Mechanism oath = this.buildOathMechanism(mechanismUID, map);
            return oath;
        } else {
            Logger.warn(TAG, "Unknown version: %s", version);
            throw new MechanismCreationException("Unknown version: " + version);
        }
    }

    private Oath buildOathMechanism(String mechanismUID, Map<String, String> map) throws MechanismCreationException {
        String issuer = map.get(MechanismParser.ISSUER);
        String accountName = map.get(MechanismParser.ACCOUNT_NAME);

        // Validate OTP type
        Oath.TokenType otpType = null;
        try {
            otpType = Oath.TokenType.valueOf(map.get(OathParser.TYPE));
        } catch (IllegalArgumentException e) {
            Logger.warn(TAG, "Invalid type: %s", otpType);
            throw new MechanismCreationException("Invalid type: " + otpType);
        }

        // Validate algorithm and secret, algorithm name is valid if a corresponding algorithm
        // can be loaded
        String algorithmStr = MapUtil.get(map, OathParser.ALGORITHM, "sha1");
        String secretStr = MapUtil.get(map, OathParser.SECRET, "");
        String algorithm = null;
        byte[] secret = null;
        try {
            algorithm = algorithmStr.toUpperCase(Locale.US);
            secret = Base32String.decode(secretStr);
            validateAlgoSecretPair(algorithm, secret);

        } catch (Base32String.DecodingException e) {
            Logger.warn(TAG, "Unexpected error decoding the secret: %s", secretStr);
            throw new MechanismCreationException("Could not decode secret:  " + secretStr, e);
        } catch (NullPointerException e) {
            Logger.warn(TAG, e,"Unexpected null whilst parsing secret: %s", secretStr);
            throw new MechanismCreationException("Unexpected null whilst parsing secret: " + secretStr, e);
        }

        // Validates the length of the OTP to generate, either 6 or 8
        String digitStr = MapUtil.get(map, OathParser.DIGITS, "6");
        int digits = Integer.parseInt(digitStr);
        if (digits != 6 && digits != 8) {
            throw new MechanismCreationException("Digits must be 6 or 8: " + digitStr);
        }

        // Validates the period
        String periodStr = MapUtil.get(map, OathParser.PERIOD, "30");
        int period;
        try {
            period = Integer.parseInt(periodStr);
            if (period <= 0) {
                Logger.warn(TAG, "Oath refresh period (%s) was not a positive integer", periodStr);
                throw new MechanismCreationException("Oath refresh period was not a positive integer");
            }
        } catch (NumberFormatException e) {
            Logger.warn(TAG, e, "Oath refresh period (%s) was not a number", periodStr);
            throw new MechanismCreationException("Oath refresh period was not a number: " + periodStr);
        }

        // Validates the counter. Only useful for HOTP
        String counterStr = MapUtil.get(map, OathParser.COUNTER, "0");
        long counter;
        try {
            counter = Long.parseLong(counterStr);
        } catch (NumberFormatException e) {
            Logger.warn(TAG, e, "Failed to parse counter (%s).", counterStr);
            throw new MechanismCreationException("Failed to parse counter: " + counterStr, e);
        }

        Oath oath = Oath.builder()
                .setMechanismUID(mechanismUID)
                .setIssuer(issuer)
                .setAccountName(accountName)
                .setType(otpType)
                .setAlgorithm(algorithm)
                .setSecret(secretStr)
                .setDigits(digits)
                .setCounter(counter)
                .setPeriod(period)
                .build();

        return oath;
    }

    private void validateAlgoSecretPair(String algo, byte[] secret) throws MechanismCreationException {
        try {
            Mac mac = Mac.getInstance("Hmac" + algo);
            if (secret != null) {
                mac.init(new SecretKeySpec(secret, "Hmac" + algo));
            }
        } catch (NoSuchAlgorithmException e) {
            Logger.warn(TAG, e, "Invalid algorithm (%s).", "Hmac" + algo);
            throw new MechanismCreationException("Invalid algorithm: " + algo);
        } catch (InvalidKeyException e) {
            Logger.warn(TAG, e,"Invalid secret for this algorithm.");
            throw new MechanismCreationException("Invalid secret for this algorithm.");
        }

    }

    @Override
    protected MechanismParser getParser() {
        return parser;
    }

}
