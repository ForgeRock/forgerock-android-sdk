/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class AccountTest extends BaseTest {

    @Test
    public void testCreateAccount() {
        Account account = new Account(ISSUER, ACCOUNT_NAME);

        assertEquals(account.getIssuer(), ISSUER);
        assertEquals(account.getAccountName(), ACCOUNT_NAME);
    }

    @Test
    public void testCreateAccountWithOptionalParameters() {

        String imageUrl = "http://forgerock.com";
        String backgroundColor = "032b75";

        Account account = new Account(ISSUER, ACCOUNT_NAME, imageUrl, backgroundColor);
        assertEquals(account.getIssuer(), ISSUER);
        assertEquals(account.getAccountName(), ACCOUNT_NAME);
        assertEquals(account.getImageURL(), imageUrl);
        assertEquals(account.getBackgroundColor(), backgroundColor);
    }

    @Test
    public void testShouldBeEqualEquivalentAccount() {
        Account account1 = new Account(ISSUER, ACCOUNT_NAME);
        Account account2 = new Account(ISSUER, ACCOUNT_NAME);

        assertEquals(account1, account2);
        assertEquals(account1.compareTo(account2), 0);
        assertEquals(account2.compareTo(account1), 0);
        assertEquals(account1.hashCode(), account2.hashCode());
    }

    @Test
    public void testShouldNotBeEqualDifferentAccountWithIssuer() {
        Account account1 = new Account(ISSUER, ACCOUNT_NAME);
        Account account2 = new Account(OTHER_ISSUER, ACCOUNT_NAME);

        assertFalse(account1.equals(account2));
        assertEquals(account1.compareTo(account2), -1);
        assertEquals(account2.compareTo(account1), 1);
    }

    @Test
    public void testShouldNotBeEqualDifferentAccountWithAccountName() {
        Account account1 = new Account(ISSUER, ACCOUNT_NAME);
        Account account2 = new Account(ISSUER, OTHER_ACCOUNT_NAME);

        assertFalse(account1.equals(account2));
        assertEquals(account1.compareTo(account2), -1);
        assertEquals(account2.compareTo(account1), 1);
    }

    @Test
    public void testShouldHandleNullEquals() {
        Account account = new Account(ISSUER, ACCOUNT_NAME);

        assertFalse(account.equals(null));
        assertEquals(account.compareTo(null), -1);
    }

}