/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

import org.forgerock.android.authenticator.exception.MechanismParsingException;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class OathParserTest {

    private OathParser oathParser;

    @Before
    public void setUp() {
        oathParser = new OathParser();
    }

    @Test
    public void testShouldParseType() throws MechanismParsingException {
        Map<String, String> result = oathParser.map("otpauth://hotp/Forgerock:user@forgerock.com?secret=ABC&counter=0");
        assertEquals(result.get(OathParser.TYPE), "hotp");
    }

    @Test
    public void testShouldParseAccountName() throws MechanismParsingException {
        Map<String, String> result = oathParser.map("otpauth://totp/example?secret=ABC");
        assertEquals(result.get(OathParser.ACCOUNT_NAME), "example");
    }

    @Test
    public void testShouldParseIssuerFromPath() throws MechanismParsingException {
        Map<String, String> result = oathParser.map("otpauth://totp/Badger:ferret?secret=ABC");
        assertEquals(result.get(OathParser.ISSUER), "Badger");
    }

    @Test
    public void testShouldOverwriteIssuerFromParameters() throws MechanismParsingException {
        Map<String, String> result = oathParser.map("otpauth://totp/Badger:ferret?issuer=Stoat&secret=ABC");
        assertEquals(result.get(OathParser.ISSUER), "Stoat");
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
    public void testShouldValidateMissingCounterInHOTPMode() throws MechanismParsingException {
        oathParser.map("otpauth://hotp/Forgerock:user@forgerock.com?secret=ABC");
    }

    @Test (expected = MechanismParsingException.class)
    public void testShouldValidateIncorrectAuthorityType() throws MechanismParsingException {
        oathParser.map("otpauth://badger/Forgerock:user@forgerock.com?secret=ABC");
    }
    
}
