/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

import java.util.Calendar;

/**
 * Base instrumented test, which will execute on an Android device.
 */
public class MockModelBuilder {

    public static Account createAccount(String issuer, String accountName){
        Account account = Account.builder()
                .setAccountName(accountName)
                .setIssuer(issuer)
                .build();

        return account;
    }

    public static Account createAccount(String issuer, String accountName, String imageUrl,
                                        String backgroundColor){
        Account account = Account.builder()
                .setAccountName(accountName)
                .setIssuer(issuer)
                .setImageURL(imageUrl)
                .setBackgroundColor(backgroundColor)
                .build();

        return account;
    }

    public static Oath createOath(String mechanismUID, String issuer, String accountName, Oath.TokenType oathType,
                                  String algorithm, String secret, int digits, long counter, int period) {
        Oath oath = Oath.builder()
                .setMechanismUID(mechanismUID)
                .setIssuer(issuer)
                .setAccountName(accountName)
                .setType(oathType)
                .setAlgorithm(algorithm)
                .setSecret(secret)
                .setDigits(digits)
                .setCounter(counter)
                .setPeriod(period)
                .build();

        return oath;
    }

    public static Push createPush(String mechanismUID, String issuer, String accountName, String secret,
                                  String registrationEndpoint, String authenticationEndpoint) {
        Push push = Push.builder()
                .setMechanismUID(mechanismUID)
                .setIssuer(issuer)
                .setAccountName(accountName)
                .setAuthenticationEndpoint(authenticationEndpoint)
                .setRegistrationEndpoint(registrationEndpoint)
                .setSecret(secret)
                .build();

        return push;
    }

    public static Notification createNotification(String mechanismUID, String messageId,
                                                  String challenge, String amlbCookie,
                                                  Calendar timeAdded, Calendar timeExpired,
                                                  long ttl, boolean approved,
                                                  boolean pending) {

        Notification notification = Notification.builder()
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

        return notification;
    }

    public static Notification createNotification(String mechanismUID, String messageId,
                                                  String challenge, String amlbCookie,
                                                  Calendar timeAdded, long ttl) {

        Notification notification = Notification.builder()
                .setMechanismUID(mechanismUID)
                .setMessageId(messageId)
                .setChallenge(challenge)
                .setAmlbCookie(amlbCookie)
                .setTimeAdded(timeAdded)
                .setTtl(ttl)
                .build();

        return notification;
    }
}
