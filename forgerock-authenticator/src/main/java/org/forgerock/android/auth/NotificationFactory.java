/*
 * Copyright (c) 2020 - 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.util.Base64;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.RemoteMessage;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;

import org.forgerock.android.auth.exception.InvalidNotificationException;

import java.text.ParseException;
import java.util.Calendar;
import java.util.TimeZone;

import static org.forgerock.android.auth.PushParser.AM_LOAD_BALANCER_COOKIE;
import static org.forgerock.android.auth.PushParser.CHALLENGE;
import static org.forgerock.android.auth.PushParser.MECHANISM_UID;
import static org.forgerock.android.auth.PushParser.TTL;

/**
 * Responsible for generating instances of {@link PushNotification}.
 **/
class NotificationFactory {

    static final String MESSAGE = "message";
    static final String MESSAGE_ID = "messageId";

    private static final int DEFAULT_TTL_SECONDS = 120;

    private static final String TAG = NotificationFactory.class.getSimpleName();

    private StorageClient storageClient;

    /**
     * Creates the NotificationFactory
     */
    NotificationFactory(StorageClient storageClient) {
        this.storageClient = storageClient;
    }

    /**
     * Process a FCM remote message into a {@link PushNotification} object
     */
    PushNotification handleMessage(@NonNull RemoteMessage message)
            throws InvalidNotificationException {
        return this.handleMessage(message.getData().get(MESSAGE_ID), message.getData().get(MESSAGE));
    }

    /**
     * Process the parameters from a FCM remote message into a {@link PushNotification} object
     */
    PushNotification handleMessage(@NonNull String messageId, @NonNull String message)
            throws InvalidNotificationException {
        Logger.debug(TAG, "Processing FCM remote message with messageId: %s.", messageId);

        PushNotification pushNotification;
        SignedJWT signedJwt;
        String mechanismUid;
        String base64Challenge;
        String base64amlbCookie;
        String amlbCookie = null;
        String ttlString;

        // Reconstruct JWT
        try {
            signedJwt = SignedJWT.parse(message);

            mechanismUid = (String) signedJwt.getJWTClaimsSet().getClaim(MECHANISM_UID);
            base64Challenge = (String) signedJwt.getJWTClaimsSet().getClaim(CHALLENGE);
            base64amlbCookie = (String) signedJwt.getJWTClaimsSet().getClaim(AM_LOAD_BALANCER_COOKIE);
            if(base64amlbCookie != null)
                amlbCookie = new String(Base64.decode(base64amlbCookie, Base64.NO_WRAP));
            ttlString = (String) signedJwt.getJWTClaimsSet().getClaim(TTL);
        } catch (ParseException e) {
            Logger.warn(TAG, e, "Failed to reconstruct JWT for message: %s", messageId);
            throw new InvalidNotificationException("Failed to reconstruct JWT for the remote message.");
        }

        // Parse TTL
        int ttl = DEFAULT_TTL_SECONDS;
        if (ttlString != null) {
            try {
                ttl = Integer.parseInt(ttlString);
            } catch (NumberFormatException e) {
                Logger.warn(TAG, e, "Failed to reconstruct JWT for message: %s. TTL was not a number.", messageId);
                throw new InvalidNotificationException("Failed to reconstruct JWT for the remote message. TTL was not a number.");
            }
        }

        // Check required fields
        if (messageId == null || mechanismUid == null || base64Challenge == null) {
            Logger.warn(TAG, "Remote message did not contain required fields.");
            throw new InvalidNotificationException("Remote message did not contain required fields.");
        }

        // Check push mechanism associated with the message
        Logger.debug(TAG, "Lookup Push mechanism associated with the message (%s).", messageId);
        PushMechanism push = (PushMechanism) storageClient.getMechanismByUUID(mechanismUid);
        if (push == null) {
            Logger.warn(TAG, "Could not retrieve the PUSH mechanism associated with the notification.");
            throw new InvalidNotificationException("Could not retrieve the PUSH mechanism associated with this remote message.");
        }

        // Verify the JWT signature
        try {
            Logger.debug(TAG, "Verifying JWT signature for message with messageId: %s.", messageId);
            if (!verify(push.getSecret(), signedJwt)) {
                throw new InvalidNotificationException("Failed to validate jwt within the remote message.");
            }
        } catch (JOSEException e) {
            Logger.warn(TAG, e,"Failed to validate jwt.");
        }

        // Create Push Notification and persist it
        pushNotification = generateNotification(mechanismUid, messageId, base64Challenge, amlbCookie, ttl);
        if (pushNotification != null) {
            if(storageClient.setNotification(pushNotification)) {
                Logger.debug(TAG, "PushNotification object with messageId %s stored into StorageClient.", messageId);
                pushNotification.setPushMechanism(push);
            } else {
                Logger.debug(TAG,"Failed to store PushNotification object with messageId %s into StorageClient.", messageId);
                throw new InvalidNotificationException("Unable to store Push Notification on the target stored system.");
            }
        }

        return pushNotification;
    }

    private boolean verify(String base64Secret, SignedJWT signedJwt) throws JOSEException {
        byte[] secret = Base64.decode(base64Secret, Base64.NO_WRAP);
        JWSVerifier verifier = new MACVerifier(secret);
        return signedJwt.verify(verifier);
    }

    private PushNotification generateNotification(String mechanismUid, String messageId,
                                                  String base64Challenge, String amlbCookie, int ttl) throws InvalidNotificationException {
        Calendar timeReceived = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Calendar timeExpired = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        timeExpired.add(Calendar.SECOND, ttl);

        return PushNotification.builder()
                        .setMechanismUID(mechanismUid)
                        .setTimeAdded(timeReceived)
                        .setTimeExpired(timeExpired)
                        .setMessageId(messageId)
                        .setChallenge(base64Challenge)
                        .setAmlbCookie(amlbCookie)
                        .setTtl(ttl)
                        .build();
    }

}
