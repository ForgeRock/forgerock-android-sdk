/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

/**
 * Provide data encryption and decryption for Android M device.
 */
class AndroidMEncryptor extends AbstractSymmetricEncryptor {

    final KeyGenParameterSpec.Builder specBuilder;

    /**
     * @param keyAlias The key alias to store the key
     */
    AndroidMEncryptor(@NonNull String keyAlias) {
        super(keyAlias);
        specBuilder = new KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(false)
                .setKeySize(KEY_SIZE);
    }

    @Override
    protected SecretKey getSecretKey() throws GeneralSecurityException, IOException {
        KeyStore keyStore = getKeyStore();
        if (!keyStore.containsAlias(keyAlias)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            keyGenerator.init(specBuilder.build());
            return keyGenerator.generateKey();
        } else {
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(keyAlias, null)).getSecretKey();
        }
    }

    @Override
    byte[] init(Cipher cipher) throws GeneralSecurityException, IOException {
        //Generate a random IV See KeyGenParameterSpec.Builder.setRandomizedEncryptionRequired
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
        return cipher.getIV();
    }

    /**
     * Retrieve and load the Android KeyStore
     *
     * @return The AndroidKeyStore
     */
    private KeyStore getKeyStore()
            throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        return keyStore;
    }

    @Override
    public void reset() throws GeneralSecurityException, IOException {
        getKeyStore().deleteEntry(keyAlias);
    }
}