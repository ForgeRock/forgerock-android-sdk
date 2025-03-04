/*
 * Copyright (c) 2020 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.forgerock.android.auth.exception.ChallengeResponseException;
import org.forgerock.android.auth.exception.MechanismCreationException;
import org.forgerock.android.auth.util.DeviceUtils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * Responsible for generating instances of {@link PushMechanism}.
 *
 * Understands the concept of a version number associated with a Push mechanism
 * and will parse the URI according to this.
 */
class PushFactory extends MechanismFactory {

    private final PushParser parser = new PushParser();
    private final PushDeviceTokenManager pushDeviceTokenManager;

    private static final String TAG = PushFactory.class.getSimpleName();

    /**
     * Creates the MechanismFactory and loads the available mechanism information.
     *
     * @param context The application context
     * @param storageClient The storage system
     * @param pushDeviceTokenManager Manages the FCM device registration token an Android app needs to receive push messages
     */
    public PushFactory(Context context, StorageClient storageClient, PushDeviceTokenManager pushDeviceTokenManager) {
        super(context, storageClient);
        this.pushDeviceTokenManager = pushDeviceTokenManager;
    }

    @Override
    protected void createFromUriParameters(int version, @NonNull String mechanismUID, @NonNull Map<String,
            String> map, @NonNull FRAListener<Mechanism> listener) {

        // Check FCM device token
        String fcmToken = this.getDeviceTokenId();
        if(fcmToken == null || fcmToken.isEmpty()) {
            listener.onException(new MechanismCreationException("Invalid FCM token. SDK will not be able to register Push mechanisms."));
            return;
        }

        // Check if Google Play Services is enabled
        try {
            if (!checkGooglePlayServices()) {
                listener.onException(new MechanismCreationException("Google Play Services not enabled."));
                return;
            }
        } catch (IllegalStateException e) {
            listener.onException(new MechanismCreationException("Missing configuration for Google Play Services."));
            return;
        }

        if (version == 1) {
            this.buildPushMechanism(mechanismUID, map, listener);
        } else {
            Logger.warn(TAG, "Unknown version: %s", version);
            listener.onException(new MechanismCreationException("Unknown version: " + version));
        }
    }

    @Override
    protected MechanismParser getParser() {
        return this.parser;
    }


//    /**
//     * Sets the FCM device token.
//     * @param fcmToken The FCM device registration token
//     */
//    protected void setFcmToken(String fcmToken) {
//        this.fcmToken = fcmToken;
//    }

    private String getDeviceTokenId() {
        return pushDeviceTokenManager.getDeviceTokenId();
    }

    private void buildPushMechanism(String mechanismUID, Map<String,
            String> map, FRAListener<Mechanism> listener) {
        String issuer = map.get(MechanismParser.ISSUER);
        String accountName = map.get(MechanismParser.ACCOUNT_NAME);
        String registrationEndpoint = map.get(PushParser.REGISTRATION_ENDPOINT);
        String authenticationEndpoint = map.get(PushParser.AUTHENTICATION_ENDPOINT);
        String base64Secret = map.get(PushParser.SHARED_SECRET);
        String base64Challenge = map.get(PushParser.CHALLENGE);
        String amlbCookie = map.get(PushParser.AM_LOAD_BALANCER_COOKIE);
        String messageId = getFromMap(map, PushParser.MESSAGE_ID, null);
        String uid = getFromMap(map, MechanismParser.USER_ID, null);
        String resourceId = getFromMap(map, PushParser.PUSH_RESOURCE_ID, null);

        String challengeResponse;
        try {
            challengeResponse = PushResponder.getInstance().generateChallengeResponse(base64Secret, base64Challenge);
        } catch (ChallengeResponseException e) {
            listener.onException(new MechanismCreationException("Error processing Push mechanism data. "
                    + e.getLocalizedMessage(), e));
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId", this.getDeviceTokenId());
        payload.put("deviceName", DeviceUtils.getDeviceName(getContext()));
        payload.put("deviceType", "android");
        payload.put("communicationType", "gcm");
        payload.put("mechanismUid", mechanismUID);
        payload.put("response", challengeResponse);

        PushResponder.getInstance().registration(registrationEndpoint, amlbCookie, base64Secret,
                messageId, payload, new FRAListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                try {
                    Mechanism push = PushMechanism.builder()
                            .setMechanismUID(mechanismUID)
                            .setIssuer(issuer)
                            .setAccountName(accountName)
                            .setAuthenticationEndpoint(authenticationEndpoint)
                            .setRegistrationEndpoint(registrationEndpoint)
                            .setSecret(base64Secret)
                            .setTimeAdded(Calendar.getInstance(TimeZone.getTimeZone("UTC")))
                            .setResourceId(resourceId)
                            .setUid(uid)
                            .build();
                    listener.onSuccess(push);
                } catch (MechanismCreationException e) {
                    listener.onException(e);
                }
            }

            @Override
            public void onException(Exception e) {
                    listener.onException(new MechanismCreationException("Failed to register with server. "
                            + e.getLocalizedMessage(), e));
            }
        });

    }

    @VisibleForTesting
    boolean checkGooglePlayServices() throws IllegalStateException {
        Context context = getContext();
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.showErrorNotification(context, resultCode);
            } else {
                Logger.warn(TAG, "Error loading Google Play Services.");
            }
            return false;
        }
        return true;
    }

}
