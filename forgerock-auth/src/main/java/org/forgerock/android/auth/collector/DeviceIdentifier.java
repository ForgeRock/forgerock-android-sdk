/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.collector;

import android.content.Context;
import android.provider.Settings;
import android.security.KeyPairGeneratorSpec;
import android.util.Base64;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import static org.forgerock.android.auth.Encryptor.ANDROID_KEYSTORE;

/**
 * Model of Device Identifier
 */
class DeviceIdentifier {

    private String keyAlias;
    private Context context;
    private static final String CN_FORGE_ROCK = "CN=ForgeRock";
    private static final int KEY_SIZE = 1024;

    DeviceIdentifier(@NotNull Context context) {

        this.keyAlias = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        this.context = context.getApplicationContext();
    }

    /**
     * Retrieve the Device Identifier, the device identifier is stable except when:
     * <p>
     *     * App is restored on a new device
     * <p>
     *     * User uninstalls/reinstall the App
     * <p>
     *     * User clears app data.
     *
     * @return The Device Identifier.
     */
    String getIdentifier() {
        try {
            return keyAlias + "-" + Base64.encodeToString(MessageDigest.getInstance("SHA1").digest(getPublicKey().getEncoded()), Base64.DEFAULT);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Key getPublicKey() throws GeneralSecurityException, IOException {
        KeyPairGenerator keyPairGenerator = null;
        KeyStore keyStore = getKeyStore();
        if (!keyStore.containsAlias(keyAlias)) {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA", ANDROID_KEYSTORE);
            keyPairGenerator.initialize(getSpec(context));
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


    private KeyPairGeneratorSpec getSpec(Context context) {

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
