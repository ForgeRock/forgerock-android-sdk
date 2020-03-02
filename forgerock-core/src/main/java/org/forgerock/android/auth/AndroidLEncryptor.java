/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import lombok.NonNull;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Provide data encryption and decryption for Android L device.
 */
class AndroidLEncryptor extends AbstractSymmetricEncryptor {

    private static final String AES = "AES";

    private final SecretKeyStore secretKeyStore;
    private final Context context;

    /**
     * @param context        The Application Context
     * @param keyAlias       The Alias to store the the SecretKey and Asymmetric Keys
     * @param secretKeyStore The SecretKeyStore to store the SecretKey
     */
    AndroidLEncryptor(Context context, @NonNull String keyAlias, SecretKeyStore secretKeyStore) {
        super(keyAlias);
        this.secretKeyStore = secretKeyStore;
        this.context = context;
    }

    /**
     * Retrieve the SecretKey.
     *
     * <p>
     * If the SecretKey is found return it, if not found, generate a new SecretKey,
     * encrypt the Secret Key with Asymmetric Key (public key), encode to base64 String and store it.
     *
     * @return The SecretKey
     */
    @Override
    protected SecretKey getSecretKey() throws GeneralSecurityException {
        String encryptedSecretKey = secretKeyStore.getEncryptedSecretKey();
        Encryptor encryptor = new AsymmetricEncryptor(context, keyAlias);
        if (encryptedSecretKey == null) {
            KeyGenerator kg = KeyGenerator.getInstance(AES);
            kg.init(KEY_SIZE);
            SecretKey secretKey = kg.generateKey();
            //Encrypt the SecretKey and persist it.
            secretKeyStore.persist(
                    Base64.encodeToString(encryptor.encrypt(secretKey.getEncoded()), Base64.DEFAULT));
            return secretKey;
        } else {
            return new SecretKeySpec(encryptor.decrypt(Base64.decode(encryptedSecretKey, Base64.DEFAULT))
                    , AES);

        }
    }

    @Override
    byte[] init(Cipher cipher) throws GeneralSecurityException {
        AlgorithmParameterSpec ivParams;
        byte[] iv = new byte[IV_LENGTH];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);
        ivParams = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), ivParams);
        return iv;
    }

    @Override
    public void reset() throws GeneralSecurityException, IOException {
        //Clear the stored encrypted SecretKey
        secretKeyStore.remove();
        //Reset the Public/Private Keys that used to encrypt the SecretKey
        new AsymmetricEncryptor(context, keyAlias).reset();

    }

}