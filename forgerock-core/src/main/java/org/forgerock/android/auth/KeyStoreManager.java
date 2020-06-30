/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.security.KeyPairGeneratorSpec;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import javax.security.auth.x500.X500Principal;

import lombok.Builder;

import static org.forgerock.android.auth.Encryptor.ANDROID_KEYSTORE;

public class KeyStoreManager {

    private Context context;

    private static final String CN_FORGE_ROCK = "CN=ForgeRock";
    private static final int KEY_SIZE = 1024;

    @Builder
    public KeyStoreManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

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
