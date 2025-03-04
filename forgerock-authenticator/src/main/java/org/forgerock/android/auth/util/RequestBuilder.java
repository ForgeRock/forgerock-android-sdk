/*
 * Copyright (c) 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.util;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

import org.forgerock.android.auth.Action;
import org.forgerock.android.auth.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import android.util.Base64;

/**
 * Utility class for building HTTP requests.
 */
public class RequestBuilder {

    private static final String TAG = RequestBuilder.class.getSimpleName();

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CHARSET_UTF_8 = "charset=utf-8";
    private static final String ACCEPT_API_VERSION = "resource=1.0, protocol=1.0";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_ACCEPT_API_VERSION = "Accept-API-Version";
    private static final String HEADER_COOKIE = "Cookie";
    private static final String KEY_JWT = "jwt";

    private RequestBuilder() {
        // Private constructor to prevent direct instantiation.
    }

    /**
     * Builder class for constructing HTTP requests.
     */
    public static class Builder {
        private URL url;
        private String amlbCookie;
        private String base64Secret;
        private Map<String, String> keys = new HashMap<>();
        private Map<String, Object> data;
        private String action;

        public Builder() {
        }

        /**
         * Sets the URL for the request.
         * @param url The URL to set.
         * @return The Builder instance for method chaining.
         */
        public Builder url(URL url) {
            this.url = Objects.requireNonNull(url, "URL cannot be null");
            return this;
        }

        /**
         * Sets the base64Secret for the request.
         * @param base64Secret The base64Secret to set.
         * @return The Builder instance for method chaining.
         */
        public Builder base64Secret(String base64Secret) {
            this.base64Secret = Objects.requireNonNull(base64Secret, "base64Secret cannot be null");
            return this;
        }

        /**
         * Sets the data for the request.
         * @param data The data to set.
         * @return The Builder instance for method chaining.
         */
        public Builder data(Map<String, Object> data) {
            this.data = Objects.requireNonNull(data, "data cannot be null");
            return this;
        }

        /**
         * Sets the action for the request.
         * @param action The action to set.
         * @return The Builder instance for method chaining.
         */
        public Builder action(String action) {
            this.action = Objects.requireNonNull(action, "action cannot be null");
            return this;
        }

        /**
         * Sets the AMLB cookie for the request.
         * @param amlbCookie The AMLB cookie to set.
         * @return The Builder instance for method chaining.
         */
        public Builder amlbCookie(String amlbCookie) {
            this.amlbCookie = amlbCookie;
            return this;
        }

        /**
         * Sets the keys for the request.
         * @param keys The keys to set.
         * @return The Builder instance for method chaining.
         */
        public Builder keys(Map<String, String> keys) {
            this.keys = Objects.requireNonNull(keys, "keys cannot be null");
            return this;
        }

        /**
         * Builds the request object.
         * @return The built request object.
         */
        public Request build() throws JOSEException, JSONException {
            if (keys.isEmpty()) {
                throw new IllegalStateException("keys cannot be empty.");
            }

            Request.Builder requestBuilder = createBaseRequestBuilder(url);

            if (amlbCookie != null) {
                requestBuilder.addHeader(HEADER_COOKIE, amlbCookie);
            }

            JSONObject message = new JSONObject();
            for (Map.Entry<String, String> entry : keys.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                message.put(key, value);
            }
            String jwt = generateJwt(base64Secret, data);
            message.put(KEY_JWT, jwt);

            return buildPostRequest(requestBuilder, message, action);
        }

        /**
         * Creates a base request builder with common headers.
         *
         * @param url The endpoint URL.
         * @return The base request builder.
         */
        private Request.Builder createBaseRequestBuilder(URL url) {
            Request.Builder requestBuilder = new Request.Builder();
            requestBuilder.url(url.toString());
            requestBuilder.addHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
            requestBuilder.addHeader(HEADER_ACCEPT_API_VERSION, ACCEPT_API_VERSION);
            return requestBuilder;
        }

        /**
         * Builds a POST request with the given JSON message and action.
         *
         * @param requestBuilder The request builder.
         * @param message        The JSON message to include in the request body.
         * @param action         The action associated with the request.
         * @return The built request object.
         */
        private Request buildPostRequest(Request.Builder requestBuilder, JSONObject message, String action) {
            MediaType mediaType = MediaType.parse(CONTENT_TYPE_JSON + "; " + CHARSET_UTF_8);
            RequestBody body = RequestBody.create(message.toString(), mediaType);
            requestBuilder.post(body);
            requestBuilder.tag(new Action(action));
            return requestBuilder.build();
        }

        /**
         * Sign the payload with JWT.
         *
         * @param base64Secret The shared secret.
         * @param data The data to sign.
         * @return The signed JWT.
         * @throws IllegalArgumentException If the secret is null or empty.
         */
        private static String generateJwt(String base64Secret, Map<String, Object> data)
                throws IllegalArgumentException, JOSEException {
            // Check shared secret
            if (base64Secret == null || base64Secret.isEmpty()) {
                Logger.debug(TAG, "Error generating JWT data. Secret is empty or null.");
                throw new IllegalArgumentException("Passed empty secret");
            }

            // Prepare JWT with claims
            JWTClaimsSet.Builder claimBuilder = new JWTClaimsSet.Builder();
            for (String key : data.keySet()) {
                claimBuilder.claim(key, data.get(key));
            }

            // Apply the HMAC protection
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                    .type(JOSEObjectType.JWT)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimBuilder.build());

            // Create HMAC signer
            byte[] secret = Base64.decode(base64Secret, Base64.NO_WRAP);
            JWSSigner signer = new MACSigner(secret);

            // Sign JWT
            signedJWT.sign(signer);
            return signedJWT.serialize();
        }
    }
}
