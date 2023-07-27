/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import androidx.annotation.NonNull;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

/**
 * The abstract class {@link AbstractSymmetricEncryptor} provides methods
 * to encrypt and decrypt data using Symmetric Key {@link SecretKey}
 */
abstract class AbstractSymmetricEncryptor implements Encryptor {

    private static final String AES_GCM_NO_PADDING = "AES/GCM/NOPADDING";
    private static final String HMAC_SHA256 = "HmacSHA256";
    static final int IV_LENGTH = 12;
    static final int KEY_SIZE = 256;
    final String keyAlias;

    AbstractSymmetricEncryptor(@NonNull String keyAlias) {
        this.keyAlias = keyAlias;
    }

    @Override
    public byte[] encrypt(@NonNull byte[] data) {
        byte[] encryptedData;
        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);

            byte[] iv = init(cipher);
            encryptedData = cipher.doFinal(data);
            byte[] mac = computeMac(keyAlias, encryptedData);
            encryptedData = concatArrays(mac, iv, encryptedData);
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
        return encryptedData;
    }

    @Override
    public byte[] decrypt(byte[] encryptedData) {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new EncryptionException("Error while getting an cipher instance", e);
        }

        int ivLength = IV_LENGTH;
        int macLength;
        try {
            macLength = Mac.getInstance(HMAC_SHA256).getMacLength();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error while instantiating MAC", e);
        }
        int encryptedDataLength = encryptedData.length - ivLength - macLength;
        byte[] macFromMessage = getArraySubset(encryptedData, 0, macLength);

        byte[] iv = getArraySubset(encryptedData, macLength, ivLength);
        encryptedData = getArraySubset(encryptedData, macLength + ivLength, encryptedDataLength);
        byte[] mac = computeMac(keyAlias, encryptedData);

        if (!Arrays.equals(mac, macFromMessage)) {
            throw new RuntimeException("MAC signature could not be verified");
        }

        AlgorithmParameterSpec ivParams;
        ivParams = new GCMParameterSpec(128, iv);

        try {
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), ivParams);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            throw new EncryptionException(e.getMessage(), e);
        }
    }

    private byte[] computeMac(String key, byte[] cipherText) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKey sk = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(sk);
            return mac.doFinal(cipherText);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new EncryptionException(e);
        }
    }

    private byte[] getArraySubset(byte[] array, int start, int length) {
        byte[] result = new byte[length];
        System.arraycopy(array, start, result, 0, length);
        return result;
    }

    private byte[] concatArrays(byte[] mac, byte[] iv, byte[] cipherText) {
        byte[] result = new byte[mac.length + iv.length + cipherText.length];
        System.arraycopy(mac, 0, result, 0, mac.length);
        System.arraycopy(iv, 0, result, mac.length, iv.length);
        System.arraycopy(cipherText, 0, result, mac.length + iv.length, cipherText.length);
        return result;
    }

    /**
     * Retrieve the SecretKey.
     *
     * <p>
     * If the SecretKey is found return it, if not found, generate a new SecretKey
     * and store it securely.
     *
     * @return The SecretKey
     */
    abstract SecretKey getSecretKey() throws GeneralSecurityException, IOException;

    abstract byte[] init(Cipher cipher) throws GeneralSecurityException, IOException;
}