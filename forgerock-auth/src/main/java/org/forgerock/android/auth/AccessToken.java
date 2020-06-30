/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import lombok.Getter;
import lombok.Setter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.*;

/**
 * Models an OAuth2 access token.
 */
@Getter
public class AccessToken extends Token implements Serializable {

    private long expiresIn;
    private String refreshToken;
    private String idToken;
    private String tokenType;
    private Scope scope;
    private Date expiration;
    private SSOToken sessionToken;
    @Setter
    private boolean persisted;

    @lombok.Builder
    public AccessToken(String value, long expiresIn, Date expiration, String refreshToken, String idToken, String tokenType, Scope scope, SSOToken sessionToken) {
        super(value);
        this.expiresIn = expiresIn;
        if (expiration == null) {
            this.expiration = new Date(System.currentTimeMillis() + (expiresIn * 1000L));
        } else {
            this.expiration = expiration;
        }
        this.refreshToken = refreshToken;
        this.idToken = idToken;
        this.tokenType = tokenType;
        this.scope = scope;
        this.sessionToken = sessionToken;
    }

    /**
     * Convenience method for checking expiration
     *
     * @return true if the expiration is before the current time
     */
    public boolean isExpired() {
        return isExpired(0);
    }

    /**
     * Convenience method for checking expiration
     *
     * @param threshold Threshold in Seconds
     * @return true if the expiration is before the current time
     */
    public boolean isExpired(long threshold) {
        Date now = new Date(System.currentTimeMillis() + (threshold * 1000L));
        return expiration != null && expiration.before(now);
    }


    /**
     * Authorization Scope
     */
    public static class Scope extends HashSet<String> {

        public Scope(Set<String> stringSet) {
            super(stringSet);
        }

        public Scope() {
            super();
        }

        JSONArray toJsonArray() {
            JSONArray result = new JSONArray();
            for (String s : this) {
                result.put(s);
            }
            return result;
        }

        static Scope fromJsonArray(JSONArray array) throws JSONException {
            if (array == null) {
                return null;
            }
            Scope s = new Scope();
            for (int i = 0; i < array.length(); i++) {
                s.add(array.getString(i));
            }
            return s;
        }

        /**
         * Parses a scope from the specified string representation
         *
         * @param s The scope string
         * @return The scope.
         */
        public static Scope parse(final String s) {

            if (s == null)
                return null;

            Scope scope = new Scope();

            if (s.trim().isEmpty())
                return scope;

            StringTokenizer st = new StringTokenizer(s, " ");

            while (st.hasMoreTokens()) {
                scope.add(st.nextToken());
            }

            return scope;
        }
    }

    public String toJson() {
        JSONObject result = new JSONObject();
        try {
            result.put("value", getValue());
            result.put("expiresIn", getExpiresIn());
            result.put("refreshToken", getRefreshToken());
            result.put("idToken", getIdToken());
            result.put("tokenType", getTokenType());
            result.put("scope", getScope() == null ? null : getScope().toJsonArray());
            result.put("expiration", getExpiration().getTime());
            result.put("sessionToken", getSessionToken() == null ? null : getSessionToken().getValue());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return result.toString();
    }

    public static AccessToken fromJson(String str) {
        try {
            JSONObject result = new JSONObject(str);
            return AccessToken.builder()
                    .value(result.getString("value"))
                    .expiresIn(result.optLong("expiresIn", -1))
                    .refreshToken(result.has("refreshToken") ? result.getString("refreshToken"): null)
                    .idToken(result.has("idToken") ? result.getString("idToken"): null)
                    .tokenType(result.has("tokenType") ? result.getString("tokenType"): null)
                    .scope(Scope.fromJsonArray(result.optJSONArray("scope")))
                    .expiration(expiration(result.optLong("expiration", -1)))
                    .sessionToken(result.has("sessionToken") ? new SSOToken(result.optString("sessionToken")) : null)
                    .build();
        } catch (JSONException e) {
            return null;
        }
    }

    private static Date expiration(long expiration) {
        if (expiration == -1) {
            return null;
        }
        return new Date(expiration);
    }


}
