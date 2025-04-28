/*
 * Copyright (c) 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.util;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

public class UrlUtils {

    private static final String UTF_8 = "UTF-8";

    /**
     * Extracts the base URL from the given URL.
     * @param url The URL to extract the base URL from.
     * @return The base URL.
     * @throws URISyntaxException If the URL is invalid.
     */
    public static String getBaseUrl(@NonNull String url) throws URISyntaxException {
        URI uri = new URI(url);
        return uri.getScheme() + "://" + uri.getHost();
    }

    /**
     * Extracts the query string from the given URL.
     * @param url The URL to extract the query string from.
     * @return The query string.
     * @throws URISyntaxException If the URL is invalid.
     */
    public static String getQueryString(@NonNull String url) throws URISyntaxException {
        URI uri = new URI(url);
        return uri.getQuery();
    }

    /**
     * Parses the query string into a map of key-value pairs.
     * @param query The query string to parse.
     * @return A map of key-value pairs.
     * @throws UnsupportedEncodingException If the query string is invalid.
     */
    public static Map<String, String> parseQueryParams(@NonNull String query)
            throws UnsupportedEncodingException {
        Map<String, String> queryParams = new HashMap<>();
        if (!query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                queryParams.put(URLDecoder.decode(pair.substring(0, idx), UTF_8),
                        URLDecoder.decode(pair.substring(idx + 1), UTF_8));
            }
        }
        return queryParams;
    }

    /**
     * Builds a query string from a map of key-value pairs.
     * @param queryParams The map of key-value pairs to build the query string from.
     * @return The built query string.
     * @throws UnsupportedEncodingException If the query string is invalid.
     */
    public static String buildQueryString(@NonNull Map<String, String> queryParams)
            throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }

            sb.append(URLEncoder.encode(entry.getKey(), UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), UTF_8));
        }
        return sb.toString();
    }

    /**
     * Updates the query parameters of the given URL.
     * @param url The URL to update the query parameters of.
     * @param queryParams The new query parameters to add to the URL.
     * @return The updated URL.
     * @throws UnsupportedEncodingException If the query string is invalid.
     * @throws URISyntaxException If the URL is invalid.
     */
    public static String updateQueryParams(@NonNull String url, @NonNull Map<String, String> queryParams)
            throws UnsupportedEncodingException, URISyntaxException {
        if (url.isEmpty()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }
        if (queryParams.isEmpty()) {
            throw new IllegalArgumentException("Query parameters cannot be empty");
        }
        String newQuery = buildQueryString(queryParams);
        URI uri = new URI(url);
        URI newUri = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), newQuery, uri.getFragment());
        return newUri.toString();
    }
}
