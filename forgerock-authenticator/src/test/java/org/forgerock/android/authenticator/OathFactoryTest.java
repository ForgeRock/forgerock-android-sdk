/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

import android.content.Context;

import org.forgerock.android.auth.DefaultStorageClient;
import org.forgerock.android.authenticator.exception.MechanismCreationException;
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

    @Before
    public void setUp() {
        Context context = mock(Context.class);
        storageClient = mock(DefaultStorageClient.class);
        given(storageClient.setAccount(any(Account.class))).willReturn(true);
        given(storageClient.setMechanism(any(Oath.class))).willReturn(true);

        factory = new OathFactory(context, storageClient);
    }

    @Test
    public void testShouldParseVersionOne() throws Exception {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ=====&version=1";
        Oath oath = (Oath) factory.createFromUri(uri);
        assertEquals(oath.getOathType(), Oath.TokenType.TOTP);
        assertEquals(oath.getAccountName(), "user1");
    }

    @Test
    public void testShouldHandleDefaultVersion() throws Exception {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ=====";
        Oath oath = (Oath) factory.createFromUri(uri);
        assertEquals(oath.getOathType(), Oath.TokenType.TOTP);
        assertEquals(oath.getAccountName(), "user1");
    }

    @Test
    public void testShouldRejectInvalidVersion() throws Exception {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ=====&version=99999";
        try {
            factory.createFromUri(uri);
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e instanceof MechanismCreationException);
        }
    }

    @Test
    public void testShouldReflectDifferences() throws Exception {
        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ=====&version=1";
        String secondUri = "otpauth://totp/Forgerock:user1?secret=IOHEOSHIEF=====&version=1";
        Oath oath = (Oath) factory.createFromUri(uri);
        Oath secondOath = (Oath) factory.createFromUri(secondUri);

        assertNotEquals(secondOath, oath);
    }

}

