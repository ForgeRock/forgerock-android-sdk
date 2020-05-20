/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.util.TimeKeeper;

/**
 * Represents a currently active OTP token.
 */
public class OathTokenCode {
    private final String code;
    private final long start;
    private final long until;
    private TimeKeeper timeKeeper;
    private OathMechanism.TokenType oathType;

    /**
     * Creates a OathTokenCode wrap with given data
     * @param timeKeeper class containing timekeeping functionality
     * @param code OTP code with 6 or 8 digits
     * @param start start timestamp for current Oath code
     * @param until expiration timestamp for current Oath code
     */
    protected OathTokenCode(TimeKeeper timeKeeper, String code, long start, long until, OathMechanism.TokenType tokenType) {
        this.timeKeeper = timeKeeper;
        this.code = code;
        this.start = start;
        this.until = until;
        this.oathType = tokenType;
    }

    /**
     * Gets the code which is currently active.
     * @return The currently active token.
     */
    public String getCurrentCode() {
        return code;
    }

    /**
     * Get the started timestamp of current Oath token.
     * @return The start time in milliseconds.
     */
    public long getStart() {
        return start;
    }

    /**
     * Get the expiration timestamp for TOTP tokens.
     * For HOTP, it returns {@code null} as it does not expires.
     * @return The expiration time in milliseconds.
     */
    public long getUntil() {
        return until;
    }

    /**
     * Indicates whether the current OathTokenCode is valid or not.
     * For HOTP, isValid always returns 'true'.
     * For TOTP, this property computes, start and end timestamp of OathTokenCode and determines
     * its validity.
     * @return {@code true} if the OathTokenCode is still valid, {@code false} otherwise.
     */
    public boolean isValid() {
        long cur = timeKeeper.getCurrentTimeMillis();

        return cur < until;
    }

    /**
     * Returns the token type (HOTP, TOTP)
     * @return The token type.
     */
    public OathMechanism.TokenType getOathType() {
        return oathType;
    }

}
