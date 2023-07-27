/*
 * Copyright (c) 2019 -2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.security.KeyPairGeneratorSpec;

import androidx.annotation.NonNull;

import javax.crypto.Cipher;
import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Calendar;
import java.util.Date;

/**
 * The class {@link AsymmetricEncryptor} provides methods
 * to encrypt and decrypt data using Asymmetric Keys PublicKey & PrivateKey,
 * the public key will be used to encrypt the data and PrivateKey will be used
 * to decrypt the data.
 */
class AsymmetricEncryptor implements Encryptor {

    private static final String RSA_ECB_PKCS1_PADDING = "RSA/ECB/PKCS1PADDING";
    public static final int KEY_SIZE = 2048;
    public static final String CN_FORGE_ROCK = "CN=ForgeRock";

    private final String keyAlias;
    private final Context context;

    AsymmetricEncryptor(Context applicationContext, @NonNull String keyAlias) {
        this.context = applicationContext.getApplicationContext();
        this.keyAlias = keyAlias;
    }

    @Override
    public byte[] encrypt(@NonNull byte[] data) {

        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance(RSA_ECB_PKCS1_PADDING);
            // ensure OAEP padding works with AndroidKeyStore
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    getPublicKey());

            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
    }

    /**
     * @param encryptedData : data to be decrypted
     * @return byte[] of decrypted data
     */
    @Override
    public byte[] decrypt(@NonNull byte[] encryptedData) {

        Cipher cipher;
        try {
            cipher = Cipher.getInstance(RSA_ECB_PKCS1_PADDING);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    getPrivateKey());

            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
    }

    @Override
    public void reset() throws GeneralSecurityException, IOException {
        KeyStore keyStore = getKeyStore();
        keyStore.deleteEntry(keyAlias);
    }

    private Key getPrivateKey() throws GeneralSecurityException, IOException, KeyUnavailableException {
        KeyStore keyStore = getKeyStore();
        if (!keyStore.containsAlias(keyAlias)) {
            throw new KeyUnavailableException("Private Key not found.");
        }
        return keyStore.getKey(keyAlias, null);

    }

    private Key getPublicKey() throws GeneralSecurityException, IOException {
        KeyPairGenerator keyPairGenerator = null;
        KeyStore keyStore = getKeyStore();
        if (!keyStore.containsAlias(keyAlias)) {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA", ANDROID_KEYSTORE);
            keyPairGenerator.initialize(getSpec(context, keyAlias));
            return keyPairGenerator.generateKeyPair().getPublic();
        } else {
            return keyStore.getCertificate(keyAlias).getPublicKey();
        }
    }

    private KeyStore getKeyStore()
            throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        return keyStore;
    }


    private KeyPairGeneratorSpec getSpec(Context context, String keyAlias) {

        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();
        cal.add(Calendar.YEAR, 10);
        Date end = cal.getTime();

        KeyPairGeneratorSpec.Builder builder = new KeyPairGeneratorSpec.Builder(context)
                .setAlias(keyAlias)
                .setSubject(new X500Principal(CN_FORGE_ROCK))
                .setKeySize(KEY_SIZE)
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(now)
                .setAlgorithmParameterSpec(new RSAKeyGenParameterSpec(KEY_SIZE, RSAKeyGenParameterSpec.F4))
                .setEndDate(end);

        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        Intent authIntent = km.createConfirmDeviceCredentialIntent(null, null);
        boolean keyguardEnabled = km.isKeyguardSecure() && authIntent != null;
        if (keyguardEnabled) {
            //key pair should be encrypted at rest
            builder.setEncryptionRequired();
        }
        return builder.build();
    }

    private class KeyUnavailableException extends Exception {
        KeyUnavailableException(String message) {
            super(message);
        }
    }
}
