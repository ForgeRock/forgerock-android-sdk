/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * This class is responsible to securely store and retrieve the SDO token.
 */
class SDOSecureStorage {

    private static final String TAG = SDOSecureStorage.class.getSimpleName();

    private static final String SHARED_PREFERENCES_SDO = "org.forgerock.android.authenticator.DATA.SDO";
    private static final String TOKEN_KEY = "token";

    private SharedPreferences sharedPreferences;

    /**
     * The Constructor.
     * @param context Application Context.
     */
    public SDOSecureStorage(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            sharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    SHARED_PREFERENCES_SDO,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Logger.warn(TAG, e, "Error on initializing SDO token storage");
        }
    }

    /**
     * Stores the SDO token.
     * @param token the SDO token as string.
     */
    public void setToken(String token) {
        sharedPreferences.edit()
                .putString(TOKEN_KEY, token)
                .apply();
    }

    /**
     * Retrieves the SDO token.
     * @return The SDO token as string. Returns {null} if no token is available.
     */
    public String getToken() {
        return sharedPreferences.getString(TOKEN_KEY, null);
    }

}
