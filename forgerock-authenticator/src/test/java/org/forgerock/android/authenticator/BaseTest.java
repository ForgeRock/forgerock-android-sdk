/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

public abstract class BaseTest {

    public static final String ISSUER = "issuer1";
    public static final String OTHER_ISSUER = "issuer2";
    public static final String ACCOUNT_NAME = "user1";
    public static final String OTHER_ACCOUNT_NAME = "user2";
    public static final String MECHANISM_UID = "b162b325-ebb1-48e0-8ab7-b38cf341da95";
    public static final String OTHER_MECHANISM_UID = "013be51a-8c14-356d-b0fc-b3660cc8a101";
    public static final String SECRET = "JMEZ2W7D462P3JYBDG2HV7PFBM";
    public static final String ALGORITHM = "sha1";
    public static final int DIGITS = 6;
    public static final int PERIOD = 30;
    public static final int COUNTER = 0;
    public static final String REGISTRATION_ENDPOINT = "http://openam.forgerock.com:8080/openam/json/push/sns/message?_action=register";
    public static final String OTHER_REGISTRATION_ENDPOINT = "http://develop.openam.forgerock.com:8080/openam/json/push/sns/message?_action=register";
    public static final String AUTHENTICATION_ENDPOINT = "http://openam.forgerock.com:8080/openam/json/push/sns/message?_action=authenticate";
    public static final String OTHER_AUTHENTICATION_ENDPOINT = "http://develop.openam.forgerock.com:8080/openam/json/push/sns/message?_action=authenticate";
    public final String MESSAGE_ID = "AUTHENTICATE:63ca6f18-7cfb-4198-bcd0-ac5041fbbea01583798229441";
    public final String CHALLENGE = "fZl8wu9JBxdRQ7miq3dE0fbF0Bcdd+gRETUbtl6qSuM=";
    public final String AMLB_COOKIE = "ZnJfc3NvX2FtbGJfcHJvZD0wMQ==";
    public final long TTL = 120;

    public void setUp() {

    }

    public void cleanUp() {

    }
}
