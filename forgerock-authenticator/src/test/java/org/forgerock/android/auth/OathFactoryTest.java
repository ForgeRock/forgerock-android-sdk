/*
 * Copyright (c) 2020 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.forgerock.android.auth.exception.DuplicateMechanismException;
import org.forgerock.android.auth.exception.MechanismCreationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

@RunWith(RobolectricTestRunner.class)
public class OathFactoryTest extends FRABaseTest {

    private Context context = ApplicationProvider.getApplicationContext();
    private OathFactory factory;
    private DefaultStorageClient storageClient;
    private FRAListenerFuture oathListenerFuture;

    private static final boolean CLEAN_UP_DATA = false;

    @After
    public void cleanUp() {
        if(CLEAN_UP_DATA) {
            storageClient.removeAll();
        }
    }

    @Before
    public void setUp() {
        storageClient = new DefaultStorageClient(context);
        storageClient.setAccountData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE));
        storageClient.setMechanismData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_MECHANISM, Context.MODE_PRIVATE));
        storageClient.setNotificationData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_NOTIFICATIONS, Context.MODE_PRIVATE));

        factory = new OathFactory(context, storageClient);
        oathListenerFuture = new FRAListenerFuture<Mechanism>();
    }

    @Test
    public void testShouldParseTotpVersionOne() throws Exception {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ=====&version=1";
        factory.createFromUri(uri, oathListenerFuture);
        OathMechanism oath = (OathMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.TOTP);
        assertEquals(oath.getAccountName(), "user1");
    }

    @Test
    public void testShouldParseHotpVersionOne() throws Exception {
        String uri = "otpauth://hotp/Forgerock:user1?secret=ONSWG4TFOQ=====&version=1";
        factory.createFromUri(uri, oathListenerFuture);
        OathMechanism oath = (OathMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.HOTP);
        assertEquals(oath.getAccountName(), "user1");
    }

    @Test
    public void testShouldParseTotpDefaultVersion() throws Exception {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ=====";
        factory.createFromUri(uri, oathListenerFuture);
        OathMechanism oath = (OathMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.TOTP);
        assertEquals(oath.getAccountName(), "user1");
    }

    @Test
    public void testShouldParseHotpDefaultVersion() throws Exception {
        String uri = "otpauth://hotp/Forgerock:user1?secret=ONSWG4TFOQ=====";
        factory.createFromUri(uri, oathListenerFuture);
        OathMechanism oath = (OathMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.HOTP);
        assertEquals(oath.getAccountName(), "user1");
    }

    @Test
    public void testShouldRejectTotpInvalidVersion() {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ=====&version=99999";
        try {
            factory.createFromUri(uri, oathListenerFuture);
            oathListenerFuture.get();
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("Unknown version:"));
        }
    }

    @Test
    public void testShouldRejectHotpInvalidVersion() {
        String uri = "otpauth://hotp/Forgerock:user1?secret=ONSWG4TFOQ=====&version=99999";
        try {
            factory.createFromUri(uri, oathListenerFuture);
            oathListenerFuture.get();
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("Unknown version:"));
        }
    }

    @Test
    public void testShouldRejectTotpInvalidVersionNotANumber() {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ=====&version=a";
        try {
            factory.createFromUri(uri, oathListenerFuture);
            oathListenerFuture.get();
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("Expected valid integer"));
        }
    }

    @Test
    public void testShouldRejectHotpInvalidVersionNotANumber() {
        String uri = "otpauth://hotp/Forgerock:user1?secret=ONSWG4TFOQ=====&version=a";
        try {
            factory.createFromUri(uri, oathListenerFuture);
            oathListenerFuture.get();
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("Expected valid integer"));
        }
    }

    @Test
    public void testShouldHandleHotpDefaultCounter() throws Exception {
        String uri = "otpauth://hotp/Forgerock:user1?secret=ONSWG4TFOQ=====";
        factory.createFromUri(uri, oathListenerFuture);
        HOTPMechanism oath = (HOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.HOTP);
        assertEquals(oath.getAccountName(), "user1");
        assertEquals(oath.getCounter(), 0);
    }

    @Test
    public void testShouldHandleHotpCounter() throws Exception {
        String uri = "otpauth://hotp/Forgerock:user1?secret=ONSWG4TFOQ=====&counter=10";
        factory.createFromUri(uri, oathListenerFuture);
        HOTPMechanism oath = (HOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.HOTP);
        assertEquals(oath.getAccountName(), "user1");
        assertEquals(oath.getCounter(), 10);
    }

    @Test
    public void testShouldHandleTotpCustomDigits() throws Exception {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ=====&digits=8";
        factory.createFromUri(uri, oathListenerFuture);
        TOTPMechanism oath = (TOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.TOTP);
        assertEquals(oath.getAccountName(), "user1");
        assertEquals(oath.getDigits(), 8);
    }

    @Test
    public void testShouldHandleHotpCustomDigits() throws Exception {
        String uri = "otpauth://hotp/Forgerock:user1?secret=ONSWG4TFOQ=====&digits=8";
        factory.createFromUri(uri, oathListenerFuture);
        HOTPMechanism oath = (HOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.HOTP);
        assertEquals(oath.getAccountName(), "user1");
        assertEquals(oath.getDigits(), 8);
    }

    @Test
    public void testShouldHandleTotpCustomDigitsAndPeriod() throws Exception {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ=====&digits=8&period=60";
        factory.createFromUri(uri, oathListenerFuture);
        TOTPMechanism oath = (TOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.TOTP);
        assertEquals(oath.getAccountName(), "user1");
        assertEquals(oath.getDigits(), 8);
        assertEquals(oath.getPeriod(), 60);
    }

    @Test
    public void testShouldFailTotpInvalidSecret() {
        String uri = "otpauth://totp/Forgerock:user1?secret=00018977=====";
        try {
            factory.createFromUri(uri, oathListenerFuture);
            oathListenerFuture.get();
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("Could not decode secret"));
        }
    }

    @Test
    public void testShouldFailHotpInvalidSecret() {
        String uri = "otpauth://hotp/Forgerock:user1?secret=00018977=====";
        try {
            factory.createFromUri(uri, oathListenerFuture);
            oathListenerFuture.get();
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("Could not decode secret"));
        }
    }

    @Test
    public void testShouldFailHotpInvalidAlgorithm() {
        String uri = "otpauth://hotp/Forgerock:user1?secret=ONSWG4TFOQ&algorithm=INVALID!";
        try {
            factory.createFromUri(uri, oathListenerFuture);
            oathListenerFuture.get();
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("Invalid algorithm"));
        }
    }

    @Test
    public void testShouldPassTotpSHA1Algorithm()
            throws ExecutionException, InterruptedException {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ&algorithm=sha1";
        factory.createFromUri(uri, oathListenerFuture);
        TOTPMechanism oath = (TOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.TOTP);
        assertEquals(oath.getAccountName(), "user1");
        assertEquals(oath.getIssuer(), "Forgerock");
        assertEquals(oath.getDigits(), 6);
        assertEquals(oath.getPeriod(), 30);
    }

    @Test
    public void testShouldPassHotpSHA1Algorithm()
            throws ExecutionException, InterruptedException {
        String uri = "otpauth://hotp/Forgerock:user1?secret=ONSWG4TFOQ&algorithm=sha1";
        factory.createFromUri(uri, oathListenerFuture);
        HOTPMechanism oath = (HOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.HOTP);
        assertEquals(oath.getAccountName(), "user1");
        assertEquals(oath.getIssuer(), "Forgerock");
        assertEquals(oath.getDigits(), 6);
        assertEquals(oath.getCounter(), 0);
    }

    @Test
    public void testShouldPassTotpSHA224Algorithm()
            throws ExecutionException, InterruptedException {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ&algorithm=sha224";
        factory.createFromUri(uri, oathListenerFuture);
        TOTPMechanism oath = (TOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.TOTP);
        assertEquals(oath.getAccountName(), "user1");
        assertEquals(oath.getIssuer(), "Forgerock");
        assertEquals(oath.getDigits(), 6);
        assertEquals(oath.getPeriod(), 30);
    }

    @Test
    public void testShouldPassHotpSHA224Algorithm()
            throws ExecutionException, InterruptedException {
        String uri = "otpauth://hotp/Forgerock:user1?secret=ONSWG4TFOQ&algorithm=sha224";
        factory.createFromUri(uri, oathListenerFuture);
        HOTPMechanism oath = (HOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.HOTP);
        assertEquals(oath.getAccountName(), "user1");
        assertEquals(oath.getIssuer(), "Forgerock");
        assertEquals(oath.getDigits(), 6);
        assertEquals(oath.getCounter(), 0);
    }

    @Test
    public void testShouldPassTotpSHA256Algorithm()
            throws ExecutionException, InterruptedException {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ&algorithm=sha256";
        factory.createFromUri(uri, oathListenerFuture);
        TOTPMechanism oath = (TOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.TOTP);
        assertEquals(oath.getAccountName(), "user1");
        assertEquals(oath.getIssuer(), "Forgerock");
        assertEquals(oath.getDigits(), 6);
        assertEquals(oath.getPeriod(), 30);
    }

    @Test
    public void testShouldPassHotpSHA256Algorithm()
            throws ExecutionException, InterruptedException {
        String uri = "otpauth://hotp/Forgerock:user1?secret=ONSWG4TFOQ&algorithm=sha256";
        factory.createFromUri(uri, oathListenerFuture);
        HOTPMechanism oath = (HOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.HOTP);
        assertEquals(oath.getAccountName(), "user1");
        assertEquals(oath.getIssuer(), "Forgerock");
        assertEquals(oath.getDigits(), 6);
        assertEquals(oath.getCounter(), 0);
    }

    @Test
    public void testShouldPassTotpSHA384Algorithm()
            throws ExecutionException, InterruptedException {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ&algorithm=sha384";
        factory.createFromUri(uri, oathListenerFuture);
        TOTPMechanism oath = (TOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.TOTP);
        assertEquals(oath.getAccountName(), "user1");
        assertEquals(oath.getIssuer(), "Forgerock");
        assertEquals(oath.getDigits(), 6);
        assertEquals(oath.getPeriod(), 30);
    }

    @Test
    public void testShouldPassHotpSHA384Algorithm()
            throws ExecutionException, InterruptedException {
        String uri = "otpauth://hotp/Forgerock:user1?secret=ONSWG4TFOQ&algorithm=sha384";
        factory.createFromUri(uri, oathListenerFuture);
        HOTPMechanism oath = (HOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.HOTP);
        assertEquals(oath.getAccountName(), "user1");
        assertEquals(oath.getIssuer(), "Forgerock");
        assertEquals(oath.getDigits(), 6);
        assertEquals(oath.getCounter(), 0);
    }

    @Test
    public void testShouldPassTotpSHA512Algorithm()
            throws ExecutionException, InterruptedException {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ&algorithm=sha512";
        factory.createFromUri(uri, oathListenerFuture);
        TOTPMechanism oath = (TOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.TOTP);
        assertEquals(oath.getAccountName(), "user1");
        assertEquals(oath.getIssuer(), "Forgerock");
        assertEquals(oath.getDigits(), 6);
        assertEquals(oath.getPeriod(), 30);
    }

    @Test
    public void testShouldPassHotpSHA512Algorithm()
            throws ExecutionException, InterruptedException {
        String uri = "otpauth://hotp/Forgerock:user1?secret=ONSWG4TFOQ&algorithm=sha512";
        factory.createFromUri(uri, oathListenerFuture);
        HOTPMechanism oath = (HOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.HOTP);
        assertEquals(oath.getAccountName(), "user1");
        assertEquals(oath.getIssuer(), "Forgerock");
        assertEquals(oath.getDigits(), 6);
        assertEquals(oath.getCounter(), 0);
    }

    @Test
    public void testShouldPassTotpMD5Algorithm()
            throws ExecutionException, InterruptedException {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ&algorithm=md5";
        factory.createFromUri(uri, oathListenerFuture);
        TOTPMechanism oath = (TOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.TOTP);
        assertEquals(oath.getAccountName(), "user1");
        assertEquals(oath.getIssuer(), "Forgerock");
        assertEquals(oath.getDigits(), 6);
        assertEquals(oath.getPeriod(), 30);
    }

    @Test
    public void testShouldPassHotpMD5Algorithm()
            throws ExecutionException, InterruptedException {
        String uri = "otpauth://hotp/Forgerock:user1?secret=ONSWG4TFOQ&algorithm=md5";
        factory.createFromUri(uri, oathListenerFuture);
        HOTPMechanism oath = (HOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.HOTP);
        assertEquals(oath.getAccountName(), "user1");
        assertEquals(oath.getIssuer(), "Forgerock");
        assertEquals(oath.getDigits(), 6);
        assertEquals(oath.getCounter(), 0);
    }

    @Test
    public void testFailToSaveAccount() {
        String uri = "otpauth://hotp/Forgerock:user1?secret=ONSWG4TFOQ";

        StorageClient storageClient = mock(DefaultStorageClient.class);
        given(storageClient.getAccount(any(String.class))).willReturn( null);
        given(storageClient.setAccount(any(Account.class))).willReturn(false);
        given(storageClient.getMechanismsForAccount(any(Account.class))).willReturn(Collections.emptyList());
        given(storageClient.setMechanism(any(Mechanism.class))).willReturn(true);
        factory = new OathFactory(context, storageClient);

        try {
            factory.createFromUri(uri, oathListenerFuture);
            oathListenerFuture.get();
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("Error while storing the Account"));
        }
    }

    @Test
    public void testFailToSaveTotpMechanism() {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ";

        StorageClient storageClient = mock(DefaultStorageClient.class);
        given(storageClient.getAccount(any(String.class))).willReturn(null);
        given(storageClient.setAccount(any(Account.class))).willReturn(true);
        given(storageClient.setMechanism(any(Mechanism.class))).willReturn(false);
        factory = new OathFactory(context, storageClient);

        try {
            factory.createFromUri(uri, oathListenerFuture);
            oathListenerFuture.get();
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("Error storing the mechanism"));
        }
    }

    @Test
    public void testFailToSaveHotpMechanism() {
        String uri = "otpauth://hotp/Forgerock:user1?secret=ONSWG4TFOQ";

        StorageClient storageClient = mock(DefaultStorageClient.class);
        given(storageClient.getAccount(any(String.class))).willReturn(null);
        given(storageClient.setAccount(any(Account.class))).willReturn(true);
        given(storageClient.setMechanism(any(Mechanism.class))).willReturn(false);
        factory = new OathFactory(context, storageClient);

        try {
            factory.createFromUri(uri, oathListenerFuture);
            oathListenerFuture.get();
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("Error storing the mechanism"));
        }
    }

    @Test
    public void testShouldFailDuplicateTotpMechanism() throws Exception {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ=====";
        factory.createFromUri(uri, oathListenerFuture);
        TOTPMechanism oath = (TOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.TOTP);
        assertEquals(oath.getAccountName(), "user1");
        assertEquals(oath.getPeriod(), 30);

        FRAListenerFuture oathListenerFuture2 = new FRAListenerFuture<Mechanism>();
        try {
            factory.createFromUri(uri, oathListenerFuture2);
            oathListenerFuture2.get();
            Assert.fail("Should throw DuplicateMechanismException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof DuplicateMechanismException);
            assertTrue(e.getLocalizedMessage().contains("Matching mechanism already exists"));
            assertTrue(((DuplicateMechanismException) e.getCause()).getCausingMechanism() instanceof TOTPMechanism);
        }
    }

    @Test
    public void testShouldFailDuplicateHotpMechanism() throws Exception {
        String uri = "otpauth://hotp/Forgerock:user1?secret=ONSWG4TFOQ=====";
        factory.createFromUri(uri, oathListenerFuture);
        HOTPMechanism oath = (HOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.HOTP);
        assertEquals(oath.getAccountName(), "user1");
        assertEquals(oath.getCounter(), 0);

        FRAListenerFuture oathListenerFuture2 = new FRAListenerFuture<Mechanism>();
        try {
            factory.createFromUri(uri, oathListenerFuture2);
            oathListenerFuture2.get();
            Assert.fail("Should throw DuplicateMechanismException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof DuplicateMechanismException);
            assertTrue(e.getLocalizedMessage().contains("Matching mechanism already exists"));
            assertTrue(((DuplicateMechanismException) e.getCause()).getCausingMechanism() instanceof HOTPMechanism);
        }
    }

    @Test
    public void testShouldReflectDifferences() throws Exception {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ=====&version=1";
        String secondUri = "otpauth://totp/Forgerock:user2?secret=IOHEOSHIEF=====&version=1";
        factory.createFromUri(uri, oathListenerFuture);
        OathMechanism oath = (OathMechanism) oathListenerFuture.get();
        factory.createFromUri(secondUri, oathListenerFuture);
        OathMechanism secondOath = (OathMechanism) oathListenerFuture.get();

        assertNotEquals(secondOath, oath);
    }

    @Test
    public void testShouldHandleCombinedURIAndRegisterOathMechanism() throws Exception {
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
                "period=30&" +
                "issuer=Rm9yZ2VSb2Nr";

        factory.createFromUri(uri, oathListenerFuture);
        TOTPMechanism oath = (TOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.TOTP);
        assertEquals(oath.getAccountName(), "demo");
        assertEquals(oath.getIssuer(), "ForgeRock");
        assertEquals(oath.getDigits(), 6);
        assertEquals(oath.getPeriod(), 30);
    }

}

