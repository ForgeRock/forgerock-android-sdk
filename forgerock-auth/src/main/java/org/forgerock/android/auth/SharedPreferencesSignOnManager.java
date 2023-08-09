/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import lombok.Builder;

import static java.util.Collections.emptySet;

import androidx.annotation.NonNull;

/**
 * Manage SSO Token with {@link SharedPreferences} as the storage.
 */
class SharedPreferencesSignOnManager implements SingleSignOnManager {

    //Alias to store keys
    private static final String ORG_FORGEROCK_V_1_KEYS = "org.forgerock.v1.KEYS";

    //File name to store tokens
    private static final String ORG_FORGEROCK_V_1_SSO_TOKENS = "org.forgerock.v1.SSO_TOKENS";

    private static final String SSO_TOKEN = "org.forgerock.v1.SSO_TOKEN";
    private static final String COOKIES = "org.forgerock.v1.COOKIES";

    //The SharedPreferences to store the token
    private SharedPreferences sharedPreferences;

    @Builder
    public SharedPreferencesSignOnManager(@NonNull Context context, SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences == null ?
                new SecuredSharedPreferences(context, ORG_FORGEROCK_V_1_SSO_TOKENS, ORG_FORGEROCK_V_1_KEYS) : sharedPreferences;
    }

    @Override
    public void persist(SSOToken token) {
       sharedPreferences.edit()
                .putString(SSO_TOKEN, token.getValue())
                .commit();
    }

    @Override
    public void persist(Collection<String> cookies) {
        if (cookies.isEmpty()) {
            sharedPreferences.edit().remove(COOKIES).commit();
        } else {
            Set<String> set = new HashSet<>(cookies);
            sharedPreferences.edit()
                    .putStringSet(COOKIES, set)
                    .commit();
        }
    }

    @Override
    public void clear() {
        sharedPreferences.edit().clear().commit();
    }

    @Override
    public SSOToken getToken() {
        String token = sharedPreferences.getString(SSO_TOKEN, null);
        if (token != null) {
            return new SSOToken(token);
        }
        return null;
    }

    @Override
    public Collection<String> getCookies() {
        return sharedPreferences.getStringSet(COOKIES, emptySet());
    }

    @Override
    public boolean hasToken() {
        return sharedPreferences.getString(SSO_TOKEN, null) != null;
    }

    @Override
    public void revoke(FRListener<Void> listener) {
        clear();
    }
}
