/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import org.forgerock.android.auth.Account;
import org.forgerock.android.auth.DefaultStorageClient;
import org.forgerock.android.auth.Oath;
import org.forgerock.android.auth.OathFactory;
import org.forgerock.android.auth.exception.MechanismCreationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class OathFactoryTest {

    private OathFactory factory;
    private DefaultStorageClient storageClient;
    private FRAListenerFuture oathListenerFuture;

    @Before
    public void setUp() {
        Context context = mock(Context.class);
        storageClient = mock(DefaultStorageClient.class);
        given(storageClient.setAccount(any(Account.class))).willReturn(true);
        given(storageClient.setMechanism(any(Oath.class))).willReturn(true);

        factory = new OathFactory(context, storageClient);
        oathListenerFuture = new FRAListenerFuture<Mechanism>();
    }

    @Test
    public void testShouldParseVersionOne() throws Exception {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ=====&version=1";
        factory.createFromUri(uri, oathListenerFuture);
        Oath oath = (Oath) oathListenerFuture.get();
        assertEquals(oath.getOathType(), Oath.TokenType.TOTP);
        assertEquals(oath.getAccountName(), "user1");
    }

    @Test
    public void testShouldHandleDefaultVersion() throws Exception {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ=====";
        factory.createFromUri(uri, oathListenerFuture);
        Oath oath = (Oath) oathListenerFuture.get();
        assertEquals(oath.getOathType(), Oath.TokenType.TOTP);
        assertEquals(oath.getAccountName(), "user1");
    }

    @Test
    public void testShouldRejectInvalidVersion() throws Exception {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ=====&version=99999";
        try {
            factory.createFromUri(uri, oathListenerFuture);
            oathListenerFuture.get();
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getLocalizedMessage().contains("Unknown version:"));
        }
    }

    @Test
    public void testShouldReflectDifferences() throws Exception {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ=====&version=1";
        String secondUri = "otpauth://totp/Forgerock:user1?secret=IOHEOSHIEF=====&version=1";
        factory.createFromUri(uri, oathListenerFuture);
        Oath oath = (Oath) oathListenerFuture.get();
        factory.createFromUri(secondUri, oathListenerFuture);
        Oath secondOath = (Oath) oathListenerFuture.get();

        assertNotEquals(secondOath, oath);
    }

}

