/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import androidx.annotation.NonNull;

import org.forgerock.android.auth.exception.MechanismCreationException;
import org.forgerock.android.auth.util.Base32String;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Responsible for generating instances of {@link OathMechanism}.
 *
 * Understands the concept of a version number associated with a Token
 * and will parse the URI according to this.
 */
class OathFactory extends MechanismFactory {

    private final OathParser parser = new OathParser();

    private static final String TAG = OathFactory.class.getSimpleName();

    /**
     * Creates the MechanismFactory and loads the available mechanism information.
     *
     * @param context The application context
     * @param storageClient The storage system
     */
    OathFactory(Context context, StorageClient storageClient) {
        super(context, storageClient);
    }

    @Override
    protected void createFromUriParameters(int version, @NonNull String mechanismUID, @NonNull Map<String, String> map,
                                           @NonNull FRAListener<Mechanism> listener) {
        if (version == 1) {
            this.buildOathMechanism(mechanismUID, map, listener);
        } else {
            Logger.warn(TAG, "Unknown version: %s", version);
            listener.onException(new MechanismCreationException("Unknown version: " + version));
        }
    }

    private void buildOathMechanism(String mechanismUID, Map<String, String> map,
                                    FRAListener<Mechanism> listener) {
        String issuer = map.get(MechanismParser.ISSUER);
        String accountName = map.get(MechanismParser.ACCOUNT_NAME);

        // Validate OTP type
        OathMechanism.TokenType otpType = null;
        try {
            otpType = OathMechanism.TokenType.valueOf(map.get(OathParser.TYPE).toUpperCase());
        } catch (IllegalArgumentException e) {
            Logger.warn(TAG, "Invalid type: %s", otpType);
            listener.onException(new MechanismCreationException("Invalid type: " + otpType));
            return;
        }

        // Validate algorithm and secret, algorithm name is valid if a corresponding algorithm
        // can be loaded
        String algorithmStr = getFromMap(map, OathParser.ALGORITHM, "sha1");
        String secretStr = getFromMap(map, OathParser.SECRET, "");
        String algorithm = null;
        byte[] secret = null;
        try {
            algorithm = algorithmStr.toUpperCase(Locale.US);
            secret = Base32String.decode(secretStr);

            //validate algo and secret pair
            Mac mac = Mac.getInstance("Hmac" + algorithm);
            if (secret != null) {
                mac.init(new SecretKeySpec(secret, "Hmac" + algorithm));
            }
        } catch (NoSuchAlgorithmException e) {
            Logger.warn(TAG, e, "Invalid algorithm (%s).", "Hmac" + algorithm);
            listener.onException(new MechanismCreationException("Invalid algorithm: " + algorithm));
            return;
        } catch (InvalidKeyException e) {
            Logger.warn(TAG, e,"Invalid secret for this algorithm.");
            listener.onException(new MechanismCreationException("Invalid secret for this algorithm."));
            return;
        } catch (Base32String.DecodingException e) {
            Logger.warn(TAG, "Unexpected error decoding the secret.");
            listener.onException(new MechanismCreationException("Could not decode secret.", e));
            return;
        } catch (NullPointerException e) {
            Logger.warn(TAG, e,"Unexpected null whilst parsing secret.");
            listener.onException(new MechanismCreationException("Unexpected null whilst parsing secret.", e));
            return;
        }

        // Validates the length of the OTP to generate, either 6 or 8
        String digitStr = getFromMap(map, OathParser.DIGITS, "6");
        int digits = Integer.parseInt(digitStr);
        if (digits != 6 && digits != 8) {
            listener.onException(new MechanismCreationException("Digits must be 6 or 8: " + digitStr));
            return;
        }

        // Validates the period
        String periodStr = getFromMap(map, OathParser.PERIOD, "30");
        int period = 30;
        try {
            period = Integer.parseInt(periodStr);
            if (period <= 0) {
                Logger.warn(TAG, "OathMechanism refresh period (%s) was not a positive integer", periodStr);
                listener.onException(new MechanismCreationException("OathMechanism refresh period was not a positive integer"));
                return;
            }
        } catch (NumberFormatException e) {
            Logger.warn(TAG, e, "OathMechanism refresh period (%s) was not a number", periodStr);
            listener.onException(new MechanismCreationException("OathMechanism refresh period was not a number: " + periodStr));
            return;
        }

        // Validates the counter. Only useful for HOTPMechanism
        String counterStr = getFromMap(map, OathParser.COUNTER, "0");
        long counter = 0;
        try {
            counter = Long.parseLong(counterStr);
        } catch (NumberFormatException e) {
            Logger.warn(TAG, e, "Failed to parse counter (%s).", counterStr);
            listener.onException(new MechanismCreationException("Failed to parse counter: " + counterStr, e));
            return;
        }

        Mechanism oath;
        switch (otpType) {
            case HOTP:
                oath = HOTPMechanism.builder()
                        .setMechanismUID(mechanismUID)
                        .setIssuer(issuer)
                        .setAccountName(accountName)
                        .setAlgorithm(algorithm)
                        .setSecret(secretStr)
                        .setDigits(digits)
                        .setCounter(counter)
                        .setTimeCreated(Calendar.getInstance(TimeZone.getTimeZone("UTC")))
                        .build();
                break;
            case TOTP:
                oath = TOTPMechanism.builder()
                        .setMechanismUID(mechanismUID)
                        .setIssuer(issuer)
                        .setAccountName(accountName)
                        .setAlgorithm(algorithm)
                        .setSecret(secretStr)
                        .setDigits(digits)
                        .setPeriod(period)
                        .setTimeCreated(Calendar.getInstance(TimeZone.getTimeZone("UTC")))
                        .build();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + otpType);
        }

        listener.onSuccess(oath);
    }

    @Override
    protected MechanismParser getParser() {
        return this.parser;
    }

}
