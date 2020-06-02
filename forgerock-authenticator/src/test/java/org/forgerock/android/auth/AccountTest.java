/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class AccountTest extends FRABaseTest {

    @Test
    public void testCreateAccount() {
        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .build();

        assertEquals(account.getIssuer(), ISSUER);
        assertEquals(account.getAccountName(), ACCOUNT_NAME);
    }

    @Test
    public void testCreateAccountWithOptionalParameters() {

        String imageUrl = IMAGE_URL;
        String backgroundColor = BACKGROUND_COLOR;

        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .setImageURL(imageUrl)
                .setBackgroundColor(backgroundColor)
                .build();

        assertEquals(account.getIssuer(), ISSUER);
        assertEquals(account.getAccountName(), ACCOUNT_NAME);
        assertEquals(account.getImageURL(), imageUrl);
        assertEquals(account.getBackgroundColor(), backgroundColor);
    }

    @Test
    public void testShouldBeEqualEquivalentAccount() {
        Account account1 = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .build();
        Account account2 = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .build();

        assertEquals(account1, account2);
        assertTrue(account1.matches(account2));
        assertEquals(account1.hashCode(), account2.hashCode());
        assertEquals(0, account1.compareTo(account2));
    }

    @Test
    public void testShouldNotBeEqualDifferentAccountWithIssuer() {
        Account account1 = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .build();
        Account account2 = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(OTHER_ISSUER)
                .build();

        assertFalse(account1.equals(account2));
        assertFalse(account1.matches(account2));
        assertEquals(-1, account1.compareTo(account2));
    }

    @Test
    public void testShouldNotBeEqualDifferentAccountWithAccountName() {
        Account account1 = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .build();
        Account account2 = Account.builder()
                .setAccountName(OTHER_ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .build();

        assertFalse(account1.equals(account2));
        assertFalse(account1.matches(account2));
    }

    @Test
    public void testShouldHandleNullEquals() {
        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .build();

        assertFalse(account.equals(null));
        assertFalse(account.matches(null));
    }

    @Test
    public void testShouldParseToJsonSuccessfully() {
        String json = "{" +
                "\"id\":\"issuer1-user1\"," +
                "\"issuer\":\"issuer1\"," +
                "\"accountName\":\"user1\"," +
                "\"imageURL\":\"http:\\/\\/forgerock.com\\/logo.jpg\"," +
                "\"backgroundColor\":\"032b75\"" +
                "}";

        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .setImageURL(IMAGE_URL)
                .setBackgroundColor(BACKGROUND_COLOR)
                .build();

        String accountAsJson = account.toJson();

        assertNotNull(accountAsJson);
        assertEquals(json, accountAsJson);
    }

    @Test
    public void testShouldSerializeSuccessfully() {
        String json = "{" +
                "\"id\":\"issuer1-user1\"," +
                "\"issuer\":\"issuer1\"," +
                "\"accountName\":\"user1\"," +
                "\"imageURL\":\"http:\\/\\/forgerock.com\\/logo.jpg\"," +
                "\"backgroundColor\":\"032b75\"" +
                "}";

        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .setImageURL(IMAGE_URL)
                .setBackgroundColor(BACKGROUND_COLOR)
                .build();

        String accountAsJson = account.serialize();

        assertNotNull(accountAsJson);
        assertEquals(json, accountAsJson);
    }

    @Test
    public void testShouldDeserializeSuccessfully() {
        String json = "{" +
                "\"id\":\"issuer1-user1\"," +
                "\"issuer\":\"issuer1\"," +
                "\"accountName\":\"user1\"," +
                "\"imageURL\":\"http:\\/\\/forgerock.com\\/logo.jpg\"," +
                "\"backgroundColor\":\"032b75\"" +
                "}";

        Account account = Account.deserialize(json);

        assertNotNull(account);
        assertEquals(account.getIssuer(), ISSUER);
        assertEquals(account.getAccountName(), ACCOUNT_NAME);
        assertEquals(account.getImageURL(), IMAGE_URL);
        assertEquals(account.getBackgroundColor(), BACKGROUND_COLOR);
    }

}