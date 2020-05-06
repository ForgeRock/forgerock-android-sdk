/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.nimbusds.jose.JOSEException;

import org.forgerock.android.auth.Logger;
import org.forgerock.android.authenticator.exception.MechanismCreationException;
import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for generating instances of {@link Push}.
 *
 * Understands the concept of a version number associated with a Push mechanism
 * and will parse the URI according to this.
 */
class PushFactory extends MechanismFactory {

    /* The FCM device's registration token used for Push notifications */
    private String fcmToken;

    private final PushParser parser = new PushParser();

    private static final String TAG = PushFactory.class.getSimpleName();

    /**
     * Creates the MechanismFactory and loads the available mechanism information.
     *
     * @param context The application context
     * @param storageClient The storage system
     * @param fcmToken The FCM device registration token an Android app needs to receive push messages
     */
    public PushFactory(Context context, StorageClient storageClient, String fcmToken) {
        super(context, storageClient);
        this.fcmToken = fcmToken;
    }

    @Override
    protected Mechanism createFromUriParameters(int version, String mechanismUID, Map<String,
            String> map) throws MechanismCreationException {

        // Check FCM device token
        if(this.fcmToken == null) {
            throw new MechanismCreationException("Invalid FCM token.");
        }

        // Check if Google Play Services is enabled
        if (!checkGooglePlayServices()) {
            throw new MechanismCreationException("Google Play Services not enabled.");
        }

        if (version == 1) {
            Mechanism push = this.buildPushMechanism(mechanismUID, map);
            return push;
        } else {
            Logger.warn(TAG, "Unknown version: %s", version);
            throw new MechanismCreationException("Unknown version: " + version);
        }
    }

    @Override
    protected MechanismParser getParser() {
        return this.parser;
    }

    private Push buildPushMechanism(String mechanismUID, Map<String,
            String> map) throws MechanismCreationException {

        String issuer = map.get(MechanismParser.ISSUER);
        String accountName = map.get(MechanismParser.ACCOUNT_NAME);
        String registrationEndpoint = map.get(PushParser.REGISTRATION_ENDPOINT);
        String authenticationEndpoint = map.get(PushParser.AUTHENTICATION_ENDPOINT);
        String base64Secret = map.get(PushParser.SHARED_SECRET);
        String base64Challenge = map.get(PushParser.CHALLENGE);
        String amlbCookie = map.get(PushParser.AM_LOAD_BALANCER_COOKIE);
        String messageId = getFromMap(map, PushParser.MESSAGE_ID, null);

        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId", this.fcmToken);
        payload.put("deviceType", "android");
        payload.put("communicationType", "gcm");
        payload.put("mechanismUid", mechanismUID);
        payload.put("response", PushResponder.generateChallengeResponse(base64Secret, base64Challenge));

        try {
            int returnCode = performPushRegistration(registrationEndpoint, amlbCookie, base64Secret, messageId, payload);
            if (returnCode != 200) {
                throw new MechanismCreationException("Communication with server returned " +
                        returnCode + " code.");
            }
        } catch (IOException | JSONException | JOSEException e) {
            throw new MechanismCreationException("Failed to register with server. " + e.getLocalizedMessage(), e);
        }

        Push push = Push.builder()
                .setMechanismUID(mechanismUID)
                .setIssuer(issuer)
                .setAccountName(accountName)
                .setAuthenticationEndpoint(authenticationEndpoint)
                .setRegistrationEndpoint(registrationEndpoint)
                .setSecret(base64Secret)
                .build();

        return push;
    }

    @VisibleForTesting
    boolean checkGooglePlayServices() {
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

    @VisibleForTesting
    int performPushRegistration(String registrationEndpoint, String amlbCookie, String base64Secret,
                                    String messageId, Map<String, Object> payload) throws IOException, JSONException, JOSEException {
        return PushResponder.respond(registrationEndpoint, amlbCookie, base64Secret, messageId, payload);
    }

}
