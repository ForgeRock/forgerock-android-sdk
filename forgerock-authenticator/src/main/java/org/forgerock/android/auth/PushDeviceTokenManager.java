/*
 * Copyright (c) 2025 Ping Identity. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import org.forgerock.android.auth.util.DeviceUtils;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * The PushDeviceTokenManager is used to manage the FCM device token. It is responsible for
 * keeping track of the current device token and updating it when necessary.
 */
class PushDeviceTokenManager {

    private static final String TAG = PushDeviceTokenManager.class.getSimpleName();

    private final Context context;
    private final StorageClient storageClient;
    private String deviceToken; // Keep track of the current token

    /**
     * Constructor
     *
     * @param context The Application Context
     * @param storageClient The Storage client
     * @param deviceToken The FCM device token
     */
    PushDeviceTokenManager(Context context, StorageClient storageClient, String deviceToken) {
        this.context = context;
        this.storageClient = storageClient;
        this.deviceToken = deviceToken;
    }

    /**
     * Get the current Push device token
     * @return The current Push device token object
     */
    PushDeviceToken getPushDeviceToken() {
        return storageClient.getPushDeviceToken();
    }

    /**
     * Get the current device token ID. If the device token has not been set, it will return null
     * @return The current device token ID as a String
     */
    String getDeviceTokenId() {
        return this.deviceToken;
    }

    /**
     * Set the push device token. If the token has changed, it will be stored in the storage client.
     * @param deviceToken The device token
     */
    void setDeviceToken(String deviceToken) {
        // Compare deviceToken with the current stored token
        if (shouldUpdateToken(deviceToken)) {
            Logger.debug(TAG, "Previous stored FCM token: %s", this.deviceToken);

            // Update device token in storage
            this.updateLocalToken(deviceToken);
        } else {
            Logger.debug(TAG, "FCM device token has not changed.");
        }
    }

    /**
     * Update the device token on the server. If the token has changed, it will be stored in the
     * storage client.
     * @param newDeviceToken The new device token
     * @param pushMechanism The push mechanism
     * @param listener The listener
     */
    void updateDeviceToken(String newDeviceToken, PushMechanism pushMechanism, FRAListener<Void> listener) {
        // Update device token in storage, if necessary
        this.setDeviceToken(newDeviceToken);

        // Update device token on the server
        this.updateDeviceTokenOnServer(newDeviceToken, pushMechanism, listener);
    }

    /**
     * Check if the device token has changed.
     * @param token The new device token
     * @return True if the device token has changed, false otherwise
     */
    boolean shouldUpdateToken(String token) {
        if (this.deviceToken == null) {
            PushDeviceToken currentToken = storageClient.getPushDeviceToken();
            if (currentToken == null) {
                return true;
            } else {
                this.deviceToken = currentToken.getTokenId();
                return !currentToken.getTokenId().equals(token);
            }
        } else {
            return !this.deviceToken.equals(token);
        }
    }

    private void updateLocalToken(String newDeviceToken) {
        // Update device token in storage
        if (!setPushDeviceToken(newDeviceToken)) {
            Logger.warn(TAG, "Error storing FCM device token.");
        }
        this.deviceToken = newDeviceToken; // Update the current token
    }

    private void updateDeviceTokenOnServer(String newDeviceToken, PushMechanism pushMechanism, FRAListener<Void> listener) {
        String deviceName = DeviceUtils.getDeviceName(context);
        PushResponder.getInstance(storageClient).updateDeviceToken(pushMechanism, newDeviceToken, deviceName, listener);
    }

    private boolean setPushDeviceToken(String newDeviceToken) {
        Logger.debug(TAG, "Storing FCM device token: %s", newDeviceToken);
        Calendar timeReceived = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        PushDeviceToken deviceToken = new PushDeviceToken(newDeviceToken, timeReceived);
        return storageClient.setPushDeviceToken(deviceToken);
    }

}
