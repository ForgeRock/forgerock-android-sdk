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

    private static final int MAX_VALUE = 1000;

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
     * For HOTP, it returns {@code 0} as it does not expires.
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

    /**
     * Get the current percent progress of the code for {@link TOTPMechanism}. This is a number
     * between 0 and 1000, and represents the amount of time that has passed between the start and
     * end times of the code.
     * For {@link HOTPMechanism}, progress always returns 0.
     * @return The percentage progress, a number between 0 and 1000.
     */
    public int getProgress() {
        long cur = timeKeeper.getCurrentTimeMillis();
        long total = until - start;
        long state = cur - start;
        int progress = (int) (state * MAX_VALUE / total);
        return progress < MAX_VALUE ? progress : MAX_VALUE;
    }

}
