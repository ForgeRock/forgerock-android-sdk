/*
 * Copyright (c) 2020 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.exception.MechanismParsingException;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;

/**
 * Provides the ability to parse URI scheme into a convenient format
 * to use with configuring a {@link OathMechanism} to generate OTP codes.
 */
class OathParser extends MechanismParser {
    /** The secret used for generating the OTP */
    public static final String SECRET = "secret";

    /** The algorithm used for generating the OTP */
    public static final String ALGORITHM = "algorithm";

    /** The number of digits that the OTP should be */
    public static final String DIGITS = "digits";

    /** The counter used to keep track of how many codes have been generated using this mechanism */
    public static final String COUNTER = "counter";

    /** The frequency with which the OTP updates */
    public static final String PERIOD = "period";

    private static final String[] ALLOWED_TYPES = new String[]{"hotp", "totp"};

    @Override
    protected Map<String, String> postProcess(Map<String, String> values) throws MechanismParsingException {
        // Validate Type
        String type = values.get(TYPE);
        boolean validType = false;
        for (String allowedType : ALLOWED_TYPES) {
            if (allowedType.equalsIgnoreCase(type)) {
                validType = true;
                break;
            }
        }
        if (!validType) {
            throw new MechanismParsingException(MessageFormat.format("Type {0} was not valid", type));
        }

        // Secret is REQUIRED
        if (!containsNonEmpty(values, SECRET)) {
            throw new MechanismParsingException("Secret is required");
        }

        // Decode Issuer for MFAUTH schemes
        if (containsNonEmpty(values, ISSUER) && Objects.equals(values.get(SCHEME), Mechanism.MFAUTH)) {
            values.put(ISSUER, getBase64DecodedString(values.get(ISSUER)));
        }

        return values;
    }
}

