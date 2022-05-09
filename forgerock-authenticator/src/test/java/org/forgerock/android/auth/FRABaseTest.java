/*
 * Copyright (c) 2020 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import android.os.Bundle;
import android.util.Base64;

import com.google.firebase.messaging.RemoteMessage;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.forgerock.android.auth.exception.InvalidNotificationException;
import org.forgerock.android.auth.exception.MechanismCreationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import okhttp3.mockwebserver.MockWebServer;

public abstract class FRABaseTest {

    public static final String ISSUER = "issuer1";
    public static final String OTHER_ISSUER = "issuer2";
    public static final String ACCOUNT_NAME = "user1";
    public static final String OTHER_ACCOUNT_NAME = "user2";
    public static final String IMAGE_URL = "http://forgerock.com/logo.jpg";
    public static final String BACKGROUND_COLOR = "032b75";
    public static final String MECHANISM_UID = "b162b325-ebb1-48e0-8ab7-b38cf341da95";
    public static final String OTHER_MECHANISM_UID = "013be51a-8c14-356d-b0fc-b3660cc8a101";
    public static final String SECRET = "JMEZ2W7D462P3JYBDG2HV7PFBM";
    public static final String CORRECT_SECRET = "2afd55692b492e60df7e9c0b4f55b0492afd55692b492e60df7e9c0b4f55b049";
    public static final String INCORRECT_SECRET = "INVALID-52e2563abe7d27f3476117ba2bc802a952e2563abe7d27f3476117ba2bc802a9";
    public static final String ALGORITHM = "sha1";
    public static final int DIGITS = 6;
    public static final int PERIOD = 30;
    public static final int COUNTER = 0;
    public static final String REGISTRATION_ENDPOINT = "http://openam.forgerock.com:8080/openam/json/push/sns/message?_action=register";
    public static final String OTHER_REGISTRATION_ENDPOINT = "http://develop.openam.forgerock.com:8080/openam/json/push/sns/message?_action=register";
    public static final String AUTHENTICATION_ENDPOINT = "http://openam.forgerock.com:8080/openam/json/push/sns/message?_action=authenticate";
    public static final String OTHER_AUTHENTICATION_ENDPOINT = "http://develop.openam.forgerock.com:8080/openam/json/push/sns/message?_action=authenticate";
    public static final String MESSAGE_ID = "AUTHENTICATE:63ca6f18-7cfb-4198-bcd0-ac5041fbbea01583798229441";
    public static final String MESSAGE = "Login attempt at ForgeRock";
    public static final String OTHER_MESSAGE_ID = "AUTHENTICATE:10ce6f20-1bfb-4198-eed1-aa5041fbbea0158123456789";
    public static final String CHALLENGE = "fZl8wu9JBxdRQ7miq3dE0fbF0Bcdd+gRETUbtl6qSuM=";
    public static final String AMLB_COOKIE = "ZnJfc3NvX2FtbGJfcHJvZD0wMQ==";
    public static final String CUSTOM_PAYLOAD = "{\"forgeRock.device.profile\":\"{ \\\"identifier\\\": \\\"772644096-668289665-4003237949\\\", \\\"metadata\\\": { \\\"hardware\\\": { \\\"cpuClass\\\": null, \\\"deviceMemory\\\": 8, \\\"hardwareConcurrency\\\": 10, \\\"maxTouchPoints\\\": 0, \\\"oscpu\\\": null, \\\"display\\\": { \\\"width\\\": 1920, \\\"height\\\": 1080, \\\"pixelDepth\\\": 24, \\\"angle\\\": 0 } }, \\\"browser\\\": { \\\"userAgent\\\": \\\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.75 Safari/537.36\\\", \\\"appName\\\": \\\"Netscape\\\", \\\"appCodeName\\\": \\\"Mozilla\\\", \\\"appVersion\\\": \\\"5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.75 Safari/537.36\\\", \\\"appMinorVersion\\\": null, \\\"buildID\\\": null, \\\"product\\\": \\\"Gecko\\\", \\\"productSub\\\": \\\"20030107\\\", \\\"vendor\\\": \\\"Google Inc.\\\", \\\"vendorSub\\\": \\\"\\\", \\\"browserLanguage\\\": null, \\\"plugins\\\": \\\"internal-pdf-viewer;internal-pdf-viewer;internal-pdf-viewer;internal-pdf-viewer;internal-pdf-viewer;\\\" }, \\\"platform\\\": { \\\"language\\\": \\\"en-US\\\", \\\"platform\\\": \\\"MacIntel\\\", \\\"userLanguage\\\": null, \\\"systemLanguage\\\": null, \\\"deviceName\\\": \\\"Mac (Browser)\\\", \\\"fonts\\\": \\\"cursive;monospace;sans-serif;fantasy;Arial;Arial Black;Arial Narrow;Arial Rounded MT Bold;Comic Sans MS;Courier;Courier New;Georgia;Impact;Papyrus;Tahoma;Trebuchet MS;Verdana;\\\", \\\"timezone\\\": 420 } }, \\\"location\\\": { \\\"latitude\\\": 49.2208569, \\\"longitude\\\": -123.1174431 } }\"}";
    public static final String LOCATION = "{\"latitude\":49.2208569,\"longitude\":-123.1174431}";
    public static final String CONTEXT_INFO = "{\"location\":{\"latitude\":49.2208569,\"longitude\":-123.1174431},\"userAgent\":\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36\",\"platform\":\"MacIntel\"}";
    public static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36";
    public static final String PLATFORM = "MacIntel";
    public static final String NUMBERS_CHALLENGE = "34,43,57";
    public static final long TTL = 120;
    public static final long TIME_ADDED = 1629261902660L;
    public static final long TIME_EXPIRED = 1629262022660L;
    public static final String TEST_SHARED_PREFERENCES_DATA_ACCOUNT = "test.DATA.ACCOUNT";
    public static final String TEST_SHARED_PREFERENCES_DATA_MECHANISM = "test.DATA.MECHANISM";
    public static final String TEST_SHARED_PREFERENCES_DATA_NOTIFICATIONS = "test.DATA.NOTIFICATIONS";

    @BeforeClass
    public static void setup() {
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
    }

    public static Map<String, String> generateBaseMessage() {
        Map<String, String> baseMessage;
        baseMessage = new HashMap<>();
        baseMessage.put(PushParser.MESSAGE_ID, MESSAGE_ID);
        baseMessage.put(PushParser.CHALLENGE, CHALLENGE);
        baseMessage.put(PushParser.MECHANISM_UID, MECHANISM_UID);
        baseMessage.put(PushParser.AM_LOAD_BALANCER_COOKIE, AMLB_COOKIE);
        baseMessage.put(PushParser.TTL, String.valueOf(TTL));
        return baseMessage;
    }

    public static Map<String, String> generateBaseMessageWithNewPayloadAttributes() {
        Map<String, String> baseMessage;
        baseMessage = new HashMap<>();
        baseMessage.put(PushParser.MESSAGE_ID, MESSAGE_ID);
        baseMessage.put(PushParser.CHALLENGE, CHALLENGE);
        baseMessage.put(PushParser.MECHANISM_UID, MECHANISM_UID);
        baseMessage.put(PushParser.AM_LOAD_BALANCER_COOKIE, AMLB_COOKIE);
        baseMessage.put(PushParser.TTL, String.valueOf(TTL));
        baseMessage.put(PushParser.TIME_INTERVAL, String.valueOf(1629261902660L));
        baseMessage.put(PushParser.CUSTOM_PAYLOAD, CUSTOM_PAYLOAD);
        return baseMessage;
    }
    public static PushMechanism mockPushMechanism(String mechanismUid) {
        final PushMechanism push = mock(PushMechanism.class);
        given(push.getAccountName()).willReturn(ACCOUNT_NAME);
        given(push.getIssuer()).willReturn(ISSUER);
        given(push.getType()).willReturn(Mechanism.PUSH);
        given(push.getMechanismUID()).willReturn(mechanismUid);
        given(push.getSecret()).willReturn(CORRECT_SECRET);
        given(push.getAuthenticationEndpoint()).willReturn(AUTHENTICATION_ENDPOINT);
        given(push.getRegistrationEndpoint()).willReturn(REGISTRATION_ENDPOINT);
        return push;
    }

    public static PushMechanism mockPushMechanism(String mechanismUid, String serverUrl) {
        final PushMechanism push = mock(PushMechanism.class);
        given(push.getAccountName()).willReturn(ACCOUNT_NAME);
        given(push.getIssuer()).willReturn(ISSUER);
        given(push.getType()).willReturn(Mechanism.PUSH);
        given(push.getMechanismUID()).willReturn(mechanismUid);
        given(push.getSecret()).willReturn(CORRECT_SECRET);
        given(push.getAuthenticationEndpoint()).willReturn(serverUrl+"authenticate");
        given(push.getRegistrationEndpoint()).willReturn(serverUrl+"register");
        return push;
    }

    public static Account createAccountWithoutAdditionalData(String issuer, String accountName){
        Account account = Account.builder()
                .setAccountName(accountName)
                .setIssuer(issuer)
                .build();

        return account;
    }

    public static Account createAccount(String accountName, String issuer) {
        Account account = Account.builder()
                .setAccountName(accountName)
                .setIssuer(issuer)
                .setImageURL(IMAGE_URL)
                .setBackgroundColor(BACKGROUND_COLOR)
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

    public static Mechanism createOathMechanism(String accountName, String issuer, String mechanismUid) {
        OathMechanism mechanism = null;
        try {
            mechanism = HOTPMechanism.builder()
                    .setMechanismUID(mechanismUid)
                    .setIssuer(issuer)
                    .setAccountName(accountName)
                    .setAlgorithm(ALGORITHM)
                    .setSecret(SECRET)
                    .setDigits(DIGITS)
                    .setCounter(COUNTER)
                    .build();
        } catch (MechanismCreationException e) {
            e.printStackTrace();
        }
        return mechanism;
    }

    public static OathMechanism createOathMechanism(String mechanismUID, String issuer, String accountName, OathMechanism.TokenType oathType,
                                                    String algorithm, String secret, int digits, long counter, int period) {
        OathMechanism oath = null;
        try {
            oath = HOTPMechanism.builder()
                    .setMechanismUID(mechanismUID)
                    .setIssuer(issuer)
                    .setAccountName(accountName)
                    .setAlgorithm(algorithm)
                    .setSecret(secret)
                    .setDigits(digits)
                    .setCounter(counter)
                    .build();
        } catch (MechanismCreationException e) {
            e.printStackTrace();
        }

        return oath;
    }

    public static PushMechanism createPushMechanism(String mechanismUID, String issuer, String accountName, String secret,
                                                    String registrationEndpoint, String authenticationEndpoint) {
        PushMechanism push = null;
        try {
            push = PushMechanism.builder()
                    .setMechanismUID(mechanismUID)
                    .setIssuer(issuer)
                    .setAccountName(accountName)
                    .setAuthenticationEndpoint(authenticationEndpoint)
                    .setRegistrationEndpoint(registrationEndpoint)
                    .setSecret(secret)
                    .build();
        } catch (MechanismCreationException e) {
            e.printStackTrace();
        }

        return push;
    }

    public static Mechanism createPushMechanism(String accountName, String issuer, String mechanismUid) {
        PushMechanism mechanism = null;
        try {
            mechanism = PushMechanism.builder()
                    .setMechanismUID(mechanismUid)
                    .setIssuer(issuer)
                    .setAccountName(accountName)
                    .setAuthenticationEndpoint(AUTHENTICATION_ENDPOINT)
                    .setRegistrationEndpoint(REGISTRATION_ENDPOINT)
                    .setSecret(SECRET)
                    .build();
        } catch (MechanismCreationException e) {
            e.printStackTrace();
        }
        return mechanism;
    }

    public static PushNotification createPushNotification(String messageId, Mechanism push) {
        Calendar timeAdded = Calendar.getInstance();
        timeAdded.setTimeInMillis(1629261902660L);
        PushNotification pushNotification = null;
        try {
            pushNotification = PushNotification.builder()
                    .setMechanismUID(MECHANISM_UID)
                    .setMessageId(messageId)
                    .setChallenge(CHALLENGE)
                    .setAmlbCookie(AMLB_COOKIE)
                    .setTimeAdded(timeAdded)
                    .setTtl(TTL)
                    .build();
        } catch (InvalidNotificationException e) {
            e.printStackTrace();
        }
        pushNotification.setPushMechanism(push);
        return pushNotification;
    }

    public static PushNotification createPushNotification(String mechanismUID, String messageId,
                                                      String challenge, String amlbCookie,
                                                      Calendar timeAdded, Calendar timeExpired,
                                                      long ttl, boolean approved,
                                                      boolean pending) {

        PushNotification pushNotification = null;
        try {
            pushNotification = PushNotification.builder()
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
        } catch (InvalidNotificationException e) {
            e.printStackTrace();
        }

        return pushNotification;
    }

    public static PushNotification createPushNotification(String mechanismUID, String messageId,
                                                      String challenge, String amlbCookie,
                                                      Calendar timeAdded, long ttl) {

        PushNotification pushNotification = null;
        try {
            pushNotification = PushNotification.builder()
                    .setMechanismUID(mechanismUID)
                    .setMessageId(messageId)
                    .setChallenge(challenge)
                    .setAmlbCookie(amlbCookie)
                    .setTimeAdded(timeAdded)
                    .setTtl(ttl)
                    .build();
        } catch (InvalidNotificationException e) {
            e.printStackTrace();
        }

        return pushNotification;
    }

    public static RemoteMessage generateMockRemoteMessage(String messageId, String base64Secret, Map<String, String> map) throws JSONException {
        Bundle mockBundle = mock(Bundle.class);
        if(base64Secret != null) {
            String jwt = generateJwt(base64Secret, map);
            given(mockBundle.get("message")).willReturn(jwt);
        } else {
            JSONObject message = new JSONObject();
            for (String key : map.keySet()) {
                message.put(key, map.get(key));
            }
            given(mockBundle.get("message")).willReturn(message.toString());
        }
        given(mockBundle.get("messageId")).willReturn(messageId);
        given(mockBundle.keySet()).willReturn(new HashSet<String>(Arrays.asList("message", "messageId")));

        return new RemoteMessage(mockBundle);
    }

    private static String generateJwt(String base64Secret, Map<String, String> data) {
        JWTClaimsSet.Builder claimBuilder = new JWTClaimsSet.Builder();
        for (String key : data.keySet()) {
            claimBuilder.claim(key, data.get(key));
        }
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                .type(JOSEObjectType.JWT)
                .build();
        SignedJWT signedJWT = new SignedJWT(header, claimBuilder.build());

        byte[] secret = Base64.decode(base64Secret, Base64.NO_WRAP);
        JWSSigner signer = null;
        try {
            signer = new MACSigner(secret);
            signedJWT.sign(signer);
        } catch (JOSEException e) {
            e.printStackTrace();
        }
        return signedJWT.serialize();
    }

    public String getBase64PushActionUrl(MockWebServer server, String actionType) {
        String baseUrl = server.url("/").toString() + "openam/json/push/sns/message?_action=" + actionType;
        String base64Url = Base64.encodeToString(baseUrl.getBytes(), Base64.NO_WRAP);
        return base64Url;
    }

}
