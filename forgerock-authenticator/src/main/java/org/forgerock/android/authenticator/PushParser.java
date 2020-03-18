/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

import org.forgerock.android.authenticator.exception.URIMappingException;
import org.forgerock.util.encode.Base64;
import org.forgerock.util.encode.Base64url;

import java.util.Map;

/**
 * Provides the ability to parse URI scheme into a convenient format
 * to use with configuring a {@link Push} to receive push notifications.
 */
public class PushParser extends UriParser {

    private static final String BASE_64_URL_SHARED_SECRET = "s";
    private static final String BASE_64_URL_CHALLENGE = "c";
    private static final String BASE_64_URL_IMAGE = "image";
    private static final String BASE_64_URL_REG_ENDPOINT = "r";
    private static final String BASE_64_URL_AUTH_ENDPOINT = "a";
    private static final String BASE_64_AM_LOAD_BALANCER_COOKIE_KEY = "l";

    @Override
    protected Map<String, String> postProcess(Map<String, String> values) throws URIMappingException {

        if (!containsNonEmpty(values, Push.MESSAGE_ID_KEY)) {
            throw new URIMappingException("Message ID is required");
        }

        if (containsNonEmpty(values, BASE_64_URL_IMAGE)) {
            byte[] imageBytes = Base64url.decode(values.get(BASE_64_URL_IMAGE));
            if (imageBytes != null) {
                values.put(IMAGE, new String(imageBytes));
            }
        }

        if (containsNonEmpty(values, BASE_64_AM_LOAD_BALANCER_COOKIE_KEY)) {
            values.put(Push.AM_LOAD_BALANCER_COOKIE_KEY, recodeBase64UrlValueToStringWithValidation(values, BASE_64_AM_LOAD_BALANCER_COOKIE_KEY));
        }

        values.put(Push.REG_ENDPOINT_KEY, recodeBase64UrlValueToStringWithValidation(values, BASE_64_URL_REG_ENDPOINT));
        values.put(ISSUER_KEY, recodeBase64UrlValueToStringWithValidation(values, ISSUER_KEY));
        values.put(Push.AUTH_ENDPOINT_KEY, recodeBase64UrlValueToStringWithValidation(values, BASE_64_URL_AUTH_ENDPOINT));
        values.put(Push.BASE_64_SHARED_SECRET_KEY, recodeBase64UrlValueToBase64WithValidation(values, BASE_64_URL_SHARED_SECRET));
        values.put(Push.BASE_64_CHALLENGE_KEY, recodeBase64UrlValueToBase64WithValidation(values, BASE_64_URL_CHALLENGE));

        return values;
    }

    byte[] decodeValueWithValidation(Map<String, String> data, String key) throws URIMappingException{
        if (!containsNonEmpty(data, key)) {
            throw new URIMappingException(key + " must not be empty");
        }
        byte[] bytes = Base64url.decode(data.get(key));

        if (bytes == null) {
            throw new URIMappingException("Failed to decode value in " + key);
        }
        return bytes;
    }

    String recodeBase64UrlValueToBase64WithValidation(Map<String, String> data, String key) throws URIMappingException{
        return Base64.encode(decodeValueWithValidation(data, key));
    }

    String recodeBase64UrlValueToStringWithValidation(Map<String, String> data, String key) throws URIMappingException{
        return new String(decodeValueWithValidation(data, key));
    }
}
