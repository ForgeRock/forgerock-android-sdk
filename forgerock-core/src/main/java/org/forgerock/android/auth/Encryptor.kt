/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * The interface class {@link Encryptor} provides methods
 * to encrypt and decrypt data.
 */
public interface Encryptor {

    String ANDROID_KEYSTORE = "AndroidKeyStore";

    /**
     * Encrypts the data.
     *
     * @param clearText bytes to encrypt
     * @return encrypted data
     */
    byte[] encrypt(byte[] clearText);

    /**
     * Decrypts the data.
     *
     * @param encryptedData bytes to decrypt
     * @return decrypted data
     */
    byte[] decrypt(byte[] encryptedData);

    /**
     * Reset the Encryption Provider, remove all created keys
     */
    void reset() throws GeneralSecurityException, IOException;

    @SuppressLint("NewApi")
    static Encryptor getEncryptor(Context context, String keyAlias) {
        switch (Build.VERSION.SDK_INT) {
            case Build.VERSION_CODES.M:
                return new AndroidMEncryptor(keyAlias);
            case Build.VERSION_CODES.N:
            case Build.VERSION_CODES.N_MR1:
            case Build.VERSION_CODES.O:
            case Build.VERSION_CODES.O_MR1:
                return new AndroidNEncryptor(keyAlias);
            default:
                return new AndroidPEncryptor(context, keyAlias);
        }
    }


}
