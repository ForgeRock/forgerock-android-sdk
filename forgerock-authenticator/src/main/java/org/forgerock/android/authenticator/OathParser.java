/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

import org.forgerock.android.authenticator.exception.URIMappingException;

import java.text.MessageFormat;
import java.util.Map;

/**
 * Provides the ability to parse URI scheme into a convenient format
 * to use with configuring a {@link Oath} to generate OTP codes.
 */
public class OathParser extends UriParser {
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
    protected Map<String, String> postProcess(Map<String, String> values) throws URIMappingException {
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
            throw new URIMappingException(MessageFormat.format("Type {0} was not valid", type));
        }

        // Secret is REQUIRED
        if (!containsNonEmpty(values, SECRET)) {
            throw new URIMappingException("Secret is required");
        }

        // Counter is REQUIRED if the algorithm is HOTP
        if (type.equalsIgnoreCase("hotp") && !containsNonEmpty(values, COUNTER)) {
            throw new URIMappingException("Counter is required when in hotp mode");
        }

        return values;
    }
}

