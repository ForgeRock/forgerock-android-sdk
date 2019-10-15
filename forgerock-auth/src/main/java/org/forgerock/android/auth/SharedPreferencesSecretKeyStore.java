/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.SharedPreferences;
import lombok.RequiredArgsConstructor;

/**
 * Store the encrypted SecretKey to SharedPreferences
 */
@RequiredArgsConstructor
class SharedPreferencesSecretKeyStore implements SecretKeyStore {

    private final String keyAlias;
    private final SharedPreferences sharedPreferences;

    public void persist(String encryptedSecretKey) {
        sharedPreferences.edit().putString(keyAlias, encryptedSecretKey).commit();
    }

    public String getEncryptedSecretKey() {
        return sharedPreferences.getString(keyAlias, null);
    }

    @Override
    public void remove() {
        sharedPreferences.edit().remove(keyAlias).commit();
    }


}
