/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.exception.InvalidNotificationException;
import org.forgerock.android.auth.exception.MechanismCreationException;

import java.util.Calendar;

/**
 * Base instrumented test, which will execute on an Android device.
 */
public class MockModelBuilder {

    public static Account createAccount(String issuer, String accountName) {
        Account account = Account.builder()
                .setAccountName(accountName)
                .setIssuer(issuer)
                .build();

        return account;
    }

    public static Account createAccount(String issuer, String accountName, String imageUrl,
                                        String backgroundColor) {
        Account account = Account.builder()
                .setAccountName(accountName)
                .setIssuer(issuer)
                .setImageURL(imageUrl)
                .setBackgroundColor(backgroundColor)
                .build();

        return account;
    }

    public static OathMechanism createOath(String mechanismUID, String issuer, String accountName, OathMechanism.TokenType oathType,
                                           String algorithm, String secret, int digits, long counter, int period) throws MechanismCreationException {

        switch (oathType) {
            case TOTP:
                return TOTPMechanism.builder()
                        .setMechanismUID(mechanismUID)
                        .setIssuer(issuer)
                        .setAccountName(accountName)
                        .setAlgorithm(algorithm)
                        .setSecret(secret)
                        .setDigits(digits)
                        .setPeriod(period)
                        .build();
            case HOTP:
                return HOTPMechanism.builder()
                        .setMechanismUID(mechanismUID)
                        .setIssuer(issuer)
                        .setAccountName(accountName)
                        .setAlgorithm(algorithm)
                        .setSecret(secret)
                        .setDigits(digits)
                        .setCounter(counter)
                        .build();
            default:
                throw new IllegalArgumentException();
        }
    }

    public static PushMechanism createPush(String mechanismUID, String issuer, String accountName, String secret,
                                           String registrationEndpoint, String authenticationEndpoint) throws MechanismCreationException {
        PushMechanism push = PushMechanism.builder()
                .setMechanismUID(mechanismUID)
                .setIssuer(issuer)
                .setAccountName(accountName)
                .setAuthenticationEndpoint(authenticationEndpoint)
                .setRegistrationEndpoint(registrationEndpoint)
                .setSecret(secret)
                .build();

        return push;
    }

    public static PushNotification createNotification(String mechanismUID, String messageId,
                                                      String challenge, String amlbCookie,
                                                      Calendar timeAdded, Calendar timeExpired,
                                                      long ttl, boolean approved,
                                                      boolean pending) throws InvalidNotificationException {

        PushNotification pushNotification = PushNotification.builder()
                .setMechanismUID(mechanismUID)
                .setMessageId(messageId)
                .setChallenge(challenge)
                .setAmlbCookie(amlbCookie)
                .setTimeAdded(timeAdded)
                .setTimeExpired(timeExpired)
                .setTtl(ttl)
                .setApproved(approved)
                .setPending(pending)
                .build();

        return pushNotification;
    }

    public static PushNotification createNotification(String mechanismUID, String messageId,
                                                      String challenge, String amlbCookie,
                                                      Calendar timeAdded, long ttl) throws InvalidNotificationException {

        PushNotification pushNotification = PushNotification.builder()
                .setMechanismUID(mechanismUID)
                .setMessageId(messageId)
                .setChallenge(challenge)
                .setAmlbCookie(amlbCookie)
                .setTimeAdded(timeAdded)
                .setTtl(ttl)
                .build();

        return pushNotification;
    }
}
