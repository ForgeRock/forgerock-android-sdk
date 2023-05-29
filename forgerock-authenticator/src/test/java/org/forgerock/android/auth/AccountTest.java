/*
 * Copyright (c) 2020 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.policy.DeviceTamperingPolicy;
import org.forgerock.android.auth.policy.FRAPolicy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.TimeZone;

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
    public void testShouldReturnDefaultAccountNameAndIssuer() {
        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .build();

        assertEquals(account.getDisplayIssuer(), ISSUER);
        assertEquals(account.getDisplayAccountName(), ACCOUNT_NAME);
    }

    @Test
    public void testShouldReturnAlternativeAccountNameAndIssuer() {
        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .build();

        account.setDisplayAccountName(OTHER_ACCOUNT_NAME);
        account.setDisplayIssuer(OTHER_ISSUER);

        assertEquals(account.getDisplayIssuer(), OTHER_ISSUER);
        assertEquals(account.getDisplayAccountName(), OTHER_ACCOUNT_NAME);
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
                "\"backgroundColor\":\"032b75\"," +
                "\"timeAdded\":1629261902660," +
                "\"policies\":\"{\\\"biometricAvailable\\\": { },\\\"deviceTampering\\\": {\\\"score\\\": 0.8}}\"," +
                "\"lock\":false" +
                "}";

        Calendar timeAdded = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        timeAdded.setTimeInMillis(1629261902660L);

        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .setImageURL(IMAGE_URL)
                .setBackgroundColor(BACKGROUND_COLOR)
                .setTimeAdded(timeAdded)
                .setPolicies(POLICIES)
                .setLock(false)
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
                "\"backgroundColor\":\"032b75\"," +
                "\"timeAdded\":1629261902660," +
                "\"policies\":\"{\\\"biometricAvailable\\\": { },\\\"deviceTampering\\\": {\\\"score\\\": 0.8}}\"," +
                "\"lock\":false" +
                "}";

        Calendar timeAdded = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        timeAdded.setTimeInMillis(1629261902660L);

        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .setImageURL(IMAGE_URL)
                .setBackgroundColor(BACKGROUND_COLOR)
                .setTimeAdded(timeAdded)
                .setPolicies(POLICIES)
                .setLock(false)
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
                "\"backgroundColor\":\"032b75\"," +
                "\"timeAdded\":1629261902660" +
                "}";

        Account account = Account.deserialize(json);

        assertNotNull(account);
        assertEquals(account.getIssuer(), ISSUER);
        assertEquals(account.getAccountName(), ACCOUNT_NAME);
        assertEquals(account.getImageURL(), IMAGE_URL);
        assertEquals(account.getBackgroundColor(), BACKGROUND_COLOR);
    }

    @Test
    public void testShouldDeserializeWithNullableAttributesSuccessfully() {
        String json = "{" +
                "\"id\":\"issuer1-user1\"," +
                "\"issuer\":\"issuer1\"," +
                "\"displayIssuer\":null," +
                "\"accountName\":\"user1\"," +
                "\"displayAccountName\":null," +
                "\"imageURL\":null," +
                "\"backgroundColor\":\"032b75\"," +
                "\"timeAdded\":1629261902660," +
                "\"policies\":null," +
                "\"lockingPolicy\":null," +
                "\"lock\":false" +
                "}";

        Account account = Account.deserialize(json);

        assertNotNull(account);
        assertEquals(account.getIssuer(), ISSUER);
        assertEquals(account.getDisplayIssuer(), ISSUER);
        assertEquals(account.getAccountName(), ACCOUNT_NAME);
        assertEquals(account.getDisplayAccountName(), ACCOUNT_NAME);
        assertNull(account.getImageURL());
        assertEquals(account.getBackgroundColor(), BACKGROUND_COLOR);
        assertNull(account.getPolicies());
        assertNull(account.getLockingPolicy());
        assertFalse(account.isLocked());
    }

    @Test
    public void testShouldUnlockAccount() {
        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .setPolicies(POLICIES)
                .setLockingPolicy("deviceTampering")
                .setLock(true)
                .build();

        account.unlock();

        assertFalse(account.isLocked());
        assertNull(account.getLockingPolicy());
    }

    @Test
    public void testShouldLockAccount() {
        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .setPolicies(POLICIES)
                .build();

        FRAPolicy policy = new DeviceTamperingPolicy();
        account.lock(policy);

        assertTrue(account.isLocked());
        assertEquals(account.getLockingPolicy(), policy.getName());
    }

}