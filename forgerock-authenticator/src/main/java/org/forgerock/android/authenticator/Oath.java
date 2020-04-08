/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

import androidx.annotation.VisibleForTesting;

/**
 * Represents an instance of a OATH authentication mechanism. Associated with an Account.
 */
public class Oath extends Mechanism {
    public enum TokenType {
        HOTP, TOTP
    }

    private static final String TOKEN_TYPE = "tokenType";
    private static final String ALGO = "algorithm";
    private static final String SECRET = "secret";
    private static final String DIGITS = "digits";
    private static final String COUNTER = "counter";
    private static final String PERIOD = "period";
    private static final int VERSION = 1;

    /** OATH type, must be either 'TOTP' or 'HOTP' */
    private TokenType oathType;
    /** Algorithm of HMAC-based OTP */
    private String algorithm;
    /** Digits as in Int for length of OTP credentials */
    private int digits;
    /** Counter as in Int for number of OTP credentials generated */
    private long counter;
    /** Unique identifier of the Mechanism */
    private int period;

    private static final String TAG = Oath.class.getSimpleName();

    public Oath(String mechanismUID, String issuer, String accountName, String type, TokenType oathType,
                String algorithm, String secret, int digits, long counter, int period) {
        super(mechanismUID, issuer, accountName, type, secret);
        this.oathType = oathType;
        this.algorithm = algorithm;
        this.digits = digits;
        this.counter = counter;
        this.period = period;
    }

    /**
     * Returns the number of digits that are in OTPs generated by this Token.
     * @return The OTP length.
     */
    @VisibleForTesting
    public int getDigits() {
        return digits;
    }

    /**
     * Returns the algorithm used by this Oath.
     */
    @VisibleForTesting
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns the value of the counter of this Oath.
     */
    @VisibleForTesting
    public long getCounter() {
        return counter;
    }

    /**
     * Returns the period of this Oath.
     * @return
     */
    @VisibleForTesting
    public long getPeriod() {
        return period;
    }

    /**
     * Returns the token type (HOTP, TOTP)
     * @return The token type.
     */
    public TokenType getOathType() {
        return oathType;
    }

}