/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

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
    public void testShouldParseVersionOne() throws Exception {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ=====&version=1";
        factory.createFromUri(uri, oathListenerFuture);
        OathMechanism oath = (OathMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.TOTP);
        assertEquals(oath.getAccountName(), "user1");
    }

    @Test
    public void testShouldHandleDefaultVersion() throws Exception {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ=====";
        factory.createFromUri(uri, oathListenerFuture);
        OathMechanism oath = (OathMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.TOTP);
        assertEquals(oath.getAccountName(), "user1");
    }

    @Test
    public void testShouldRejectInvalidVersion() {
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
    public void testShouldRejectInvalidVersionNotANumber() {
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
    public void testShouldHandleDefaultCounter() throws Exception {
        String uri = "otpauth://hotp/Forgerock:user1?secret=ONSWG4TFOQ=====";
        factory.createFromUri(uri, oathListenerFuture);
        HOTPMechanism oath = (HOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.HOTP);
        assertEquals(oath.getAccountName(), "user1");
        assertEquals(oath.getCounter(), 0);
    }

    @Test
    public void testShouldHandleCounter() throws Exception {
        String uri = "otpauth://hotp/Forgerock:user1?secret=ONSWG4TFOQ=====&counter=10";
        factory.createFromUri(uri, oathListenerFuture);
        HOTPMechanism oath = (HOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.HOTP);
        assertEquals(oath.getAccountName(), "user1");
        assertEquals(oath.getCounter(), 10);
    }

    @Test
    public void testShouldHandleCustomDigitsAndPeriod() throws Exception {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ=====&digits=8&period=60";
        factory.createFromUri(uri, oathListenerFuture);
        TOTPMechanism oath = (TOTPMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.TOTP);
        assertEquals(oath.getAccountName(), "user1");
        assertEquals(oath.getDigits(), 8);
        assertEquals(oath.getPeriod(), 60);
    }

    @Test
    public void testShouldFailInvalidSecret() {
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
    public void testShouldFailInvalidAlgorithm() {
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
    public void testFailToSaveAccount() {
        String uri = "otpauth://hotp/Forgerock:user1?secret=ONSWG4TFOQ";

        StorageClient storageClient = mock(DefaultStorageClient.class);
        given(storageClient.getAccount(any(String.class))).willReturn( null);
        given(storageClient.setAccount(any(Account.class))).willReturn(false);
        factory = new OathFactory(context, storageClient);

        try {
            factory.createFromUri(uri, oathListenerFuture);
            oathListenerFuture.get();
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("Error while storing a new Account"));
        }
    }

    @Test
    public void testFailToSaveMechanism() {
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
    public void testShouldFailDuplicateMechanism() throws Exception {
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

}

