/*
 * Copyright (c) 2020 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.forgerock.android.auth.FRABaseTest.POLICIES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.forgerock.android.auth.exception.MechanismParsingException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class PushParserTest {

    private PushParser pushParser;

    @Before
    public void setUp() {
        pushParser = new PushParser();
    }

    @Test
    public void testShouldNotParseInvalidType() throws MechanismParsingException {
        Map<String, String> result = pushParser.map("pushauth://hotp/forgerock:user?a=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPWF1dGhlbnRpY2F0ZQ&b=519387&r=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPXJlZ2lzdGVy&s=b3uYLkQ7dRPjBaIzV0t_aijoXRgMq-NP5AwVAvRfa_E&c=9giiBAdUHjqpo0XE4YdZ7pRlv0hrQYwDz8Z1wwLLbkg&l=YW1sYmNvb2tpZT0wMQ&m=REGISTER:8be951c6-af83-438d-8f74-421bd18650421570561063169&issuer=Rm9yZ2VSb2Nr");
        assertNotEquals(result.get(MechanismParser.TYPE), "push");
    }

    @Test
    public void testShouldParseAccountName() throws MechanismParsingException {
        Map<String, String> result = pushParser.map("pushauth://push/forgerock:user?a=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPWF1dGhlbnRpY2F0ZQ&b=519387&r=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPXJlZ2lzdGVy&s=b3uYLkQ7dRPjBaIzV0t_aijoXRgMq-NP5AwVAvRfa_E&c=9giiBAdUHjqpo0XE4YdZ7pRlv0hrQYwDz8Z1wwLLbkg&l=YW1sYmNvb2tpZT0wMQ&m=REGISTER:8be951c6-af83-438d-8f74-421bd18650421570561063169&issuer=Rm9yZ2VSb2Nr");
        assertEquals(result.get(PushParser.ACCOUNT_NAME), "user");
    }

    @Test
    public void testShouldParseIssuer() throws MechanismParsingException {
        Map<String, String> result = pushParser.map("pushauth://push/forgerock:user?a=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPWF1dGhlbnRpY2F0ZQ&b=519387&r=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPXJlZ2lzdGVy&s=b3uYLkQ7dRPjBaIzV0t_aijoXRgMq-NP5AwVAvRfa_E&c=9giiBAdUHjqpo0XE4YdZ7pRlv0hrQYwDz8Z1wwLLbkg&l=YW1sYmNvb2tpZT0wMQ&m=REGISTER:8be951c6-af83-438d-8f74-421bd18650421570561063169&issuer=Rm9yZ2VSb2Nr");
        assertEquals(result.get(PushParser.ISSUER), "ForgeRock");
    }

    @Test
    public void testShouldParseAuthenticationEndpoint() throws MechanismParsingException {
        Map<String, String> result = pushParser.map("pushauth://push/forgerock:user?a=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPWF1dGhlbnRpY2F0ZQ&b=519387&r=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPXJlZ2lzdGVy&s=b3uYLkQ7dRPjBaIzV0t_aijoXRgMq-NP5AwVAvRfa_E&c=9giiBAdUHjqpo0XE4YdZ7pRlv0hrQYwDz8Z1wwLLbkg&l=YW1sYmNvb2tpZT0wMQ&m=REGISTER:8be951c6-af83-438d-8f74-421bd18650421570561063169&issuer=Rm9yZ2VSb2Nr");
        assertEquals(result.get(PushParser.AUTHENTICATION_ENDPOINT), "http://dev.openam.example.com:8081/openam/json/dev/push/sns/message?_action=authenticate");
    }

    @Test
    public void testShouldParseRegistrationEndpoint() throws MechanismParsingException {
        Map<String, String> result = pushParser.map("pushauth://push/forgerock:user?a=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPWF1dGhlbnRpY2F0ZQ&b=519387&r=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPXJlZ2lzdGVy&s=b3uYLkQ7dRPjBaIzV0t_aijoXRgMq-NP5AwVAvRfa_E&c=9giiBAdUHjqpo0XE4YdZ7pRlv0hrQYwDz8Z1wwLLbkg&l=YW1sYmNvb2tpZT0wMQ&m=REGISTER:8be951c6-af83-438d-8f74-421bd18650421570561063169&issuer=Rm9yZ2VSb2Nr");
        assertEquals(result.get(PushParser.REGISTRATION_ENDPOINT), "http://dev.openam.example.com:8081/openam/json/dev/push/sns/message?_action=register");
    }

    @Test
    public void testShouldParseSecret() throws MechanismParsingException {
        Map<String, String> result = pushParser.map("pushauth://push/forgerock:user?a=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPWF1dGhlbnRpY2F0ZQ&b=519387&r=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPXJlZ2lzdGVy&s=b3uYLkQ7dRPjBaIzV0t_aijoXRgMq-NP5AwVAvRfa_E&c=9giiBAdUHjqpo0XE4YdZ7pRlv0hrQYwDz8Z1wwLLbkg&l=YW1sYmNvb2tpZT0wMQ&m=REGISTER:8be951c6-af83-438d-8f74-421bd18650421570561063169&issuer=Rm9yZ2VSb2Nr");
        assertEquals(result.get(PushParser.SHARED_SECRET), "b3uYLkQ7dRPjBaIzV0t/aijoXRgMq+NP5AwVAvRfa/E=");
    }

    @Test
    public void testShouldParseChallenge() throws MechanismParsingException {
        Map<String, String> result = pushParser.map("pushauth://push/forgerock:user?a=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPWF1dGhlbnRpY2F0ZQ&b=519387&r=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPXJlZ2lzdGVy&s=b3uYLkQ7dRPjBaIzV0t_aijoXRgMq-NP5AwVAvRfa_E&c=9giiBAdUHjqpo0XE4YdZ7pRlv0hrQYwDz8Z1wwLLbkg&l=YW1sYmNvb2tpZT0wMQ&m=REGISTER:8be951c6-af83-438d-8f74-421bd18650421570561063169&issuer=Rm9yZ2VSb2Nr");
        assertEquals(result.get(PushParser.CHALLENGE), "9giiBAdUHjqpo0XE4YdZ7pRlv0hrQYwDz8Z1wwLLbkg=");
    }

    @Test
    public void testShouldParseAmlbCookie() throws MechanismParsingException {
        Map<String, String> result = pushParser.map("pushauth://push/forgerock:user?a=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPWF1dGhlbnRpY2F0ZQ&b=519387&r=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPXJlZ2lzdGVy&s=b3uYLkQ7dRPjBaIzV0t_aijoXRgMq-NP5AwVAvRfa_E&c=9giiBAdUHjqpo0XE4YdZ7pRlv0hrQYwDz8Z1wwLLbkg&l=YW1sYmNvb2tpZT0wMQ&m=REGISTER:8be951c6-af83-438d-8f74-421bd18650421570561063169&issuer=Rm9yZ2VSb2Nr");
        assertEquals(result.get(PushParser.AM_LOAD_BALANCER_COOKIE), "amlbcookie=01");
    }

    @Test
    public void testShouldParseMessageId() throws MechanismParsingException {
        Map<String, String> result = pushParser.map("pushauth://push/forgerock:user?a=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPWF1dGhlbnRpY2F0ZQ&b=519387&r=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPXJlZ2lzdGVy&s=b3uYLkQ7dRPjBaIzV0t_aijoXRgMq-NP5AwVAvRfa_E&c=9giiBAdUHjqpo0XE4YdZ7pRlv0hrQYwDz8Z1wwLLbkg&l=YW1sYmNvb2tpZT0wMQ&m=REGISTER:8be951c6-af83-438d-8f74-421bd18650421570561063169&issuer=Rm9yZ2VSb2Nr");
        assertEquals(result.get(PushParser.MESSAGE_ID), "REGISTER:8be951c6-af83-438d-8f74-421bd18650421570561063169");
    }

    @Test
    public void testShouldParseBackgroundColor() throws MechanismParsingException {
        Map<String, String> result = pushParser.map("pushauth://push/forgerock:user?a=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPWF1dGhlbnRpY2F0ZQ&b=519387&r=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPXJlZ2lzdGVy&s=b3uYLkQ7dRPjBaIzV0t_aijoXRgMq-NP5AwVAvRfa_E&c=9giiBAdUHjqpo0XE4YdZ7pRlv0hrQYwDz8Z1wwLLbkg&l=YW1sYmNvb2tpZT0wMQ&m=REGISTER:8be951c6-af83-438d-8f74-421bd18650421570561063169&issuer=Rm9yZ2VSb2Nr");
        assertEquals(result.get(PushParser.BG_COLOR), "#519387");
    }

    @Test
    public void testShouldParseImageUrl() throws MechanismParsingException {
        Map<String, String> result = pushParser.map("pushauth://push/forgerock:user?a=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPWF1dGhlbnRpY2F0ZQ&b=519387&image=aHR0cDovL2Zvcmdlcm9jay5jb20vbG9nby5qcGc&r=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPXJlZ2lzdGVy&s=b3uYLkQ7dRPjBaIzV0t_aijoXRgMq-NP5AwVAvRfa_E&c=9giiBAdUHjqpo0XE4YdZ7pRlv0hrQYwDz8Z1wwLLbkg&l=YW1sYmNvb2tpZT0wMQ&m=REGISTER:8be951c6-af83-438d-8f74-421bd18650421570561063169&issuer=Rm9yZ2VSb2Nr");
        assertEquals(result.get(PushParser.IMAGE), "http://forgerock.com/logo.jpg");
    }

    @Test
    public void testShouldParseMfaUri() throws MechanismParsingException {
        String uri = "mfauth://totp/Forgerock:demo?" +
                "a=aHR0cHM6Ly9mb3JnZXJvY2suZXhhbXBsZS5jb20vb3BlbmFtL2pzb24vcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPWF1dGhlbnRpY2F0ZQ&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=aHR0cHM6Ly9mb3JnZXJvY2suZXhhbXBsZS5jb20vb3BlbmFtL2pzb24vcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPXJlZ2lzdGVy&" +
                "s=ryJkqNRjXYd_nX523672AX_oKdVXrKExq-VjVeRKKTc&" +
                "c=Daf8vrc8onKu-dcptwCRS9UHmdui5u16vAdG2HMU4w0&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "policies=eyJiaW9tZXRyaWNBdmFpbGFibGUiOiB7IH0sImRldmljZVRhbXBlcmluZyI6IHsic2NvcmUiOiAwLjh9fQ&" +
                "digits=6&" +
                "secret=R2PYFZRISXA5L25NVSSYK2RQ6E======&" +
                "period=30&";
        Map<String, String> result = pushParser.map(uri);
        assertEquals(result.get(PushParser.ACCOUNT_NAME), "demo");
        assertEquals(result.get(PushParser.ISSUER), "Forgerock");
        assertEquals(result.get(PushParser.MESSAGE_ID), "9326d19c-4d08-4538-8151-f8558e71475f1464361288472");
        assertEquals(result.get(PushParser.AM_LOAD_BALANCER_COOKIE), "amlbcookie=01");
        assertEquals(result.get(PushParser.AUTHENTICATION_ENDPOINT), "https://forgerock.example.com/openam/json/push/sns/message?_action=authenticate");
        assertEquals(result.get(PushParser.REGISTRATION_ENDPOINT), "https://forgerock.example.com/openam/json/push/sns/message?_action=register");
        assertEquals(result.get(PushParser.CHALLENGE), "Daf8vrc8onKu+dcptwCRS9UHmdui5u16vAdG2HMU4w0=");
        assertEquals(result.get(PushParser.POLICIES), POLICIES);
    }

}
