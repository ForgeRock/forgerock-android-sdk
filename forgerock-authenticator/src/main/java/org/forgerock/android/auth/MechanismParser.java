/*
 * Copyright (c) 2020 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.exception.MechanismParsingException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for converting mechanism URIs to useful data. Extracts common information (scheme, type,
 * version, issuer and account name). All information stored as parameters are converted to a map.
 * Subclasses on this class must implement postProcess(), which verifies that all required information
 * is present, and transforms it as required.
 */
abstract class MechanismParser {
    /** The protocol of the URI */
    public static final String SCHEME = "scheme";
    /** The type of OTP (TOTP or HOTP) */
    public static final String TYPE = "authority"; // URI refers to this as the authority.
    /** The URI API Version */
    public static final String VERSION = "version";
    /** The IDP that issued the URI */
    public static final String ISSUER = "issuer";
    /** The identity account name */
    public static final String ACCOUNT_NAME = "accountname";
    /** The identity image */
    public static final String IMAGE = "image";
    /** The identity color */
    public static final String BG_COLOR = "b";
    /** Unknown identity **/
    public static final String UNTITLED = "Untitled";

    private static final String SLASH = "/";

    private static final String TAG = MechanismParser.class.getSimpleName();

    /**
     * Call through to {@link MechanismParser#map(URI)}
     *
     * @param uriScheme Non null.
     * @return Non null, possibly empty Map.
     * @throws MechanismParsingException If there was an unexpected error parsing.
     */
    public final Map<String, String> map(String uriScheme) throws MechanismParsingException {
        try {
            return postProcess(map(new URI(uriScheme)));
        } catch (URISyntaxException e) {
            Logger.warn(TAG, e,"Failed to parse URI: %s", uriScheme);
            throw new MechanismParsingException("Failed to parse URI", e);
        }
    }

    /**
     * Parse the URI into a more useful Map format with known keys.
     *
     * Makes use of the Java provided {@link URI} to simplify parsing.
     *
     * @param uri Non null URI to parse.
     * @return Non null possibly empty Map.
     * @throws MechanismParsingException If there was an unexpected error parsing.
     */
    private Map<String, String> map(URI uri) throws MechanismParsingException {
        Map<String, String> r = new HashMap<String, String>();
        r.put(SCHEME, uri.getScheme());
        r.put(TYPE, uri.getAuthority());

        // Label may contain Issuer and Account Name
        String path = stripSlash(uri.getPath());
        String[] pathParts = split(path, ":");
        if (pathParts == null) {
            r.put(ISSUER, path);
            r.put(ACCOUNT_NAME, path);
        } else {
            r.put(ISSUER, pathParts[0]);
            r.put(ACCOUNT_NAME, pathParts[1]);
        }

        Collection<String> queryParts = Collections.emptySet();
        if (uri.getQuery() != null) {
            queryParts = Arrays.asList(uri.getQuery().split("&"));
        }
        for (String query : queryParts) {
            String[] split = split(query, "=");
            if (split != null) {
                if (split[0].equals(ISSUER) && split[1].isEmpty()) {
                    continue;
                }
                r.put(split[0], split[1]);
            }
        }

        if (r.containsKey(BG_COLOR) && !r.get(BG_COLOR).startsWith("#")) {
            r.put(BG_COLOR, "#" + r.get(BG_COLOR));
        }

        // Check identity
        if (r.get(ISSUER).isEmpty() && r.get(ACCOUNT_NAME).isEmpty()) {
            throw new MechanismParsingException("No identity is associated with this MFA account. Missing account name and issuer.");
        } else if (r.get(ACCOUNT_NAME).isEmpty()) {
            r.put(ACCOUNT_NAME, UNTITLED);
        }

        return r;
    }

    /**
     * Validates the parsed URI values
     *
     * @param values The non null map of values stored in the parameters in the URI.
     * @return The same map of values, with a transform applied if required.
     * @throws MechanismParsingException If there were any validation errors.
     */
    protected abstract Map<String, String> postProcess(Map<String, String> values) throws MechanismParsingException;

    protected final boolean containsNonEmpty(Map<String, String> values, String key) {
        return values.containsKey(key) && !values.get(key).isEmpty();
    }

    private static String[] split(String s, String sep) {
        int index = s.indexOf(sep);
        if (index == -1) {
            return null;
        }
        return new String[]{
                s.substring(0, index),
                s.substring(index + sep.length(), s.length())};
    }

    private static String stripSlash(String s) {
        if (s.startsWith(SLASH)) {
            return stripSlash(s.substring(SLASH.length(), s.length()));
        }
        if (s.endsWith(SLASH)) {
            return stripSlash(s.substring(0, s.length() - SLASH.length()));
        }
        return s;
    }
}

