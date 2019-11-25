/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.content.SharedPreferences;

import lombok.Builder;
import lombok.NonNull;

/**
 * Manage SSO Token with {@link SharedPreferences} as the storage.
 */
class SharedPreferencesSignOnManager implements SingleSignOnManager {

    //Alias to store keys
    private static final String ORG_FORGEROCK_V_1_KEYS = "org.forgerock.v1.KEYS";

    //File name to store tokens
    private static final String ORG_FORGEROCK_V_1_SSO_TOKENS = "org.forgerock.v1.SSO_TOKENS";

    private static final String SSO_TOKEN = "org.forgerock.v1.SSO_TOKEN";

    private SharedPreferences sharedPreferences;

    @Builder
    public SharedPreferencesSignOnManager(@NonNull Context context, SharedPreferences sharedPreferences) {
        Config config = Config.getInstance(context);
        this.sharedPreferences = config.applyDefaultIfNull(sharedPreferences, context, var -> new SecuredSharedPreferences(var
                , ORG_FORGEROCK_V_1_SSO_TOKENS, ORG_FORGEROCK_V_1_KEYS));
    }

    @Override
    public void persist(SSOToken token) {
        sharedPreferences.edit()
                .putString(SSO_TOKEN, token.getValue())
                .commit();
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
    public boolean hasToken() {
        return sharedPreferences.getString(SSO_TOKEN, null) != null;
    }

    @Override
    public void revoke(FRListener<Void> listener) {
        clear();
    }
}
