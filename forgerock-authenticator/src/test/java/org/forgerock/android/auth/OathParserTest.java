/*
 * Copyright (c) 2020 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.forgerock.android.auth.FRABaseTest.POLICIES;
import static org.junit.Assert.assertEquals;

import org.forgerock.android.auth.exception.MechanismParsingException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class OathParserTest {

    private OathParser oathParser;

    @Before
    public void setUp() {
        oathParser = new OathParser();
    }

    @Test
    public void testShouldParseHOTPType() throws MechanismParsingException {
        Map<String, String> result = oathParser.map("otpauth://hotp/Forgerock:user@forgerock.com?secret=ABC&counter=0");
        assertEquals(result.get(OathParser.TYPE), "hotp");
    }

    @Test
    public void testShouldParseTOTPType() throws MechanismParsingException {
        Map<String, String> result = oathParser.map("otpauth://totp/Forgerock:user@forgerock.com?secret=ABC&counter=0");
        assertEquals(result.get(OathParser.TYPE), "totp");
    }

    @Test (expected = MechanismParsingException.class)
    public void testShouldFailWrongType() throws MechanismParsingException {
        oathParser.map("pushauth://hotp/forgerock:user?a=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPWF1dGhlbnRpY2F0ZQ&b=519387&r=aHR0cDovL2Rldi5vcGVuYW0uZXhhbXBsZS5jb206ODA4MS9vcGVuYW0vanNvbi9kZXYvcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPXJlZ2lzdGVy&s=b3uYLkQ7dRPjBaIzV0t_aijoXRgMq-NP5AwVAvRfa_E&c=9giiBAdUHjqpo0XE4YdZ7pRlv0hrQYwDz8Z1wwLLbkg&l=YW1sYmNvb2tpZT0wMQ&m=REGISTER:8be951c6-af83-438d-8f74-421bd18650421570561063169&issuer=Rm9yZ2VSb2Nr\"");
    }

    @Test
    public void testShouldParseAccountName() throws MechanismParsingException {
        Map<String, String> result = oathParser.map("otpauth://totp/example?secret=ABC");
        assertEquals(result.get(OathParser.ACCOUNT_NAME), "example");
        assertEquals(result.get(OathParser.ISSUER), "example");
    }

    @Test (expected = MechanismParsingException.class)
    public void testShouldFailMissingAccountNameAndIssuer() throws MechanismParsingException {
        oathParser.map("otpauth://totp/?secret=ABC");
    }

    @Test
    public void testShouldParseIssuerFromPath() throws MechanismParsingException {
        Map<String, String> result = oathParser.map("otpauth://totp/Badger:ferret?secret=ABC");
        assertEquals(result.get(OathParser.ISSUER), "Badger");
    }

    @Test
    public void testShouldParseIssuerSinglePath() throws MechanismParsingException {
        Map<String, String> result = oathParser.map("otpauth://totp/Badger?secret=ABC");
        assertEquals(result.get(OathParser.ISSUER), "Badger");
        assertEquals(result.get(OathParser.ACCOUNT_NAME), "Badger");
    }

    @Test
    public void testShouldParseIssuerFromParameters() throws MechanismParsingException {
        Map<String, String> result = oathParser.map("otpauth://totp/?issuer=Stoat&secret=ABC");
        assertEquals(result.get(OathParser.ISSUER), "Stoat");
        assertEquals(result.get(OathParser.ACCOUNT_NAME), OathParser.UNTITLED);
    }

    @Test
    public void testShouldOverwriteIssuerFromParameters() throws MechanismParsingException {
        Map<String, String> result = oathParser.map("otpauth://totp/Badger:ferret?issuer=Stoat&secret=ABC");
        assertEquals(result.get(OathParser.ISSUER), "Stoat");
        assertEquals(result.get(OathParser.ACCOUNT_NAME), "ferret");
    }

    @Test
    public void testShouldKeepIssuerFromPathIfParameterIsEmpty() throws MechanismParsingException {
        Map<String, String> result = oathParser.map("otpauth://totp/Badger:ferret?issuer=&secret=ABC");
        assertEquals(result.get(OathParser.ISSUER), "Badger");
        assertEquals(result.get(OathParser.ACCOUNT_NAME), "ferret");
    }

    @Test
    public void testShouldHandleMissingQueryParameters() throws MechanismParsingException {
        Map<String, String> result = oathParser.map("otpauth://totp/Example:user@forgerock.com?secret=ABC");
        assertEquals(result.get("missing"), null);
    }

    @Test
    public void testShouldParseKnownQueryParameters() throws MechanismParsingException {
        Map<String, String> result = oathParser.map("otpauth://totp/Example:user@forgerock.com?secret=JBSWY3DPEHPK3PXP");
        assertEquals(result.get(OathParser.SECRET), "JBSWY3DPEHPK3PXP");
    }

    @Test
    public void testShouldParseUnspecifiedQueryParameters() throws MechanismParsingException {
        Map<String, String> result = oathParser.map("otpauth://totp/Example:user@forgerock.com?secret=JBSWY3DPEHPK3PXP&badger=ferret");
        assertEquals(result.get("badger"), "ferret");
    }

    @Test
    public void testShouldParseURLEncodedImagePathFromParameter() throws MechanismParsingException {
        Map<String, String> result = oathParser.map("otpauth://totp/Example:user@forgerock.com?secret=ABC&image=" +
                "http%3A%2F%2Fupload.wikimedia.org%2Fwikipedia%2Fcommons%2F1%2F10%2FBadger-badger.jpg");
        assertEquals(result.get("image"), "http://upload.wikimedia.org/wikipedia/commons/1/10/Badger-badger.jpg");
    }

    @Test (expected = MechanismParsingException.class)
    public void testShouldValidateMissingSecret() throws MechanismParsingException {
        oathParser.map("otpauth://totp/Forgerock:user@forgerock.com");
    }

    @Test (expected = MechanismParsingException.class)
    public void testShouldValidateIncorrectAuthorityType() throws MechanismParsingException {
        oathParser.map("otpauth://badger/Forgerock:user@forgerock.com?secret=ABC");
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
        Map<String, String> result = oathParser.map(uri);
        assertEquals(result.get(OathParser.ACCOUNT_NAME), "demo");
        assertEquals(result.get(OathParser.ISSUER), "Forgerock");
        assertEquals(result.get(OathParser.TYPE), "totp");
        assertEquals(result.get(OathParser.SECRET), "R2PYFZRISXA5L25NVSSYK2RQ6E======");
        assertEquals(result.get(OathParser.PERIOD), "30");
        assertEquals(result.get(OathParser.DIGITS), "6");
        assertEquals(result.get(OathParser.POLICIES), POLICIES);
    }
    
}
