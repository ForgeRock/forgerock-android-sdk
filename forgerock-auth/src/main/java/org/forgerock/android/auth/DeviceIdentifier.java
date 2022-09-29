/*
 * Copyright (c) 2019 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.provider.Settings;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.cert.Certificate;

/**
 * Model of Device Identifier
 */
public class DeviceIdentifier {

    private String keyAlias;
    private KeyStoreManager keyStoreManager;

    /**
     * @param context
     * @param keyStoreManager
     */
    public DeviceIdentifier(@NotNull Context context, KeyStoreManager keyStoreManager) {

        this.keyAlias = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        this.keyStoreManager = keyStoreManager;
        if (keyStoreManager == null) {
            this.keyStoreManager = KeyStoreManager.builder().context(context).build();
        }

    }

    /**
     * Initiate the DeviceIdentifierBuilder class
     */
    public static DeviceIdentifierBuilder builder() {
        return new DeviceIdentifierBuilder();
    }

    /**
     * Retrieve the Device Identifier, the device identifier is stable except when:
     * <p>
     * * App is restored on a new device
     * <p>
     * * User uninstalls/reinstall the App
     * <p>
     * * User clears app data.
     *
     * @return The Device Identifier.
     */
    public String getIdentifier() {
        try {
            return keyAlias + "-" + toHexString(
                    MessageDigest.getInstance("SHA1").digest(keyStoreManager.getIdentifierKey(keyAlias).getEncoded()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve the certificate that used to generate the device ID. The Certificate is stored in the
     * Android KeyStore
     *
     * @return the certificate, or null if the given alias does not exist or does not contain a certificate.
     * @throws GeneralSecurityException General Security Exception when access the KeyStore
     * @throws IOException              IOException when access the KeyStore
     */
    public @Nullable
    Certificate getCertificate() throws GeneralSecurityException, IOException {
        KeyStoreManager keyStoreManager = KeyStoreManager.builder()
                .context(Config.getInstance().getContext()).build();
        return keyStoreManager.getCertificate(keyAlias);
    }

    /**
     * Persist the X509 certificate to the KeyStore, the certificate will be used to generate the
     * Device Key Identifier
     *
     * @param certificate the certificate that used to generate the device identifier.
     * @throws GeneralSecurityException General Security Exception when access the KeyStore
     * @throws IOException              IOException when access the KeyStore
     */
    public void persist(byte[] certificate) throws GeneralSecurityException, IOException {
        KeyStoreManager keyStoreManager = KeyStoreManager.builder()
                .context(Config.getInstance().getContext()).build();
        keyStoreManager.persist(keyAlias, certificate);
    }

    private String toHexString(byte[] byteArray) {
        StringBuilder result = new StringBuilder();
        for (byte b : byteArray) {
            int theByte = ((0x000000ff & b) | 0xffffff00);
            result.append(Integer.toHexString(theByte).substring(6));
        }
        return result.toString();
    }

    /**
     * Builder class to build the device identifier
     */
    public static class DeviceIdentifierBuilder {
        private Context context;
        private KeyStoreManager keyStoreManager;

        DeviceIdentifierBuilder() {
        }

        public DeviceIdentifierBuilder context(Context context) {
            this.context = context;
            return this;
        }

        public DeviceIdentifierBuilder keyStoreManager(KeyStoreManager keyStoreManager) {
            this.keyStoreManager = keyStoreManager;
            return this;
        }

        public DeviceIdentifier build() {
            return new DeviceIdentifier(context, keyStoreManager);
        }

        public String toString() {
            return "DeviceIdentifier.DeviceIdentifierBuilder(context=" + this.context + ", keyStoreManager=" + this.keyStoreManager + ")";
        }
    }
}
