/*
 * Copyright (c) 2019 - 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;
import static org.forgerock.android.auth.Encryptor.ANDROID_KEYSTORE;

import android.content.Context;
import android.security.KeyPairGeneratorSpec;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import javax.security.auth.x500.X500Principal;

import lombok.Builder;

/**
 * Manage Device Identifier Keys in the KeyStore.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class KeyStoreManager {

    private Context context;

    private static final String CN_FORGE_ROCK = "CN=ForgeRock";
    private static final int KEY_SIZE = 1024;

    @Builder
    public KeyStoreManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    @VisibleForTesting
    public Key getIdentifierKey(String keyAlias) throws GeneralSecurityException, IOException {
        KeyPairGenerator keyPairGenerator = null;
        KeyStore keyStore = getKeyStore();
        if (!keyStore.containsAlias(keyAlias)) {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA", ANDROID_KEYSTORE);
            keyPairGenerator.initialize(getSpec(keyAlias));
            return keyPairGenerator.generateKeyPair().getPublic();
        } else {
            return keyStore.getCertificate(keyAlias).getPublicKey();
        }
    }

    Certificate getCertificate(String keyAlias) throws GeneralSecurityException, IOException {
        KeyStore keyStore = getKeyStore();
        return keyStore.getCertificate(keyAlias);
    }

    void persist(String keyAlias, byte[] certificate) throws GeneralSecurityException, IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(certificate);
        Certificate cert = CertificateFactory.getInstance("X.509").generateCertificate(byteArrayInputStream);
        getKeyStore().setCertificateEntry(keyAlias, cert);
    }

    private KeyStore getKeyStore()
            throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        return keyStore;
    }

    private KeyPairGeneratorSpec getSpec(String keyAlias) {

        Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();
        cal.add(Calendar.YEAR, 10);
        Date end = cal.getTime();

        BigInteger serialNumber = new BigInteger(16, new Random());

        KeyPairGeneratorSpec.Builder builder = new KeyPairGeneratorSpec.Builder(context)
                .setAlias(keyAlias)
                .setSubject(new X500Principal(CN_FORGE_ROCK))
                .setKeySize(KEY_SIZE)
                .setSerialNumber(serialNumber)
                .setStartDate(now)
                .setAlgorithmParameterSpec(new RSAKeyGenParameterSpec(KEY_SIZE, RSAKeyGenParameterSpec.F4))
                .setEndDate(end);

        return builder.build();
    }

}
