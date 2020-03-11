package org.forgerock.android.authenticator;

import android.net.Uri;

import org.junit.Test;

import static org.junit.Assert.*;

import org.forgerock.android.authenticator.Account;

public class AccountTest {

    private final String ISSUER = "test.issuer";
    private final String OTHER_ISSUER = "test.issuer2";
    private final String ACCOUNT_NAME = "test.user";
    private final String OTHER_ACCOUNT_NAME = "test.user2";

    @Test
    public void createAccountSuccessfuly() {
        Account account = new Account(ISSUER, ACCOUNT_NAME);

        assertEquals(account.getIssuer(), ISSUER);
        assertEquals(account.getAccountName(), ACCOUNT_NAME);
    }

    @Test
    public void createAccountWithOptionalParametersSuccessfuly() {

        String imageUrl = "http://forgerock.com";
        String backgroundColor = "032b75";

        Account account = new Account(ISSUER, ACCOUNT_NAME, imageUrl, backgroundColor);
        assertEquals(account.getIssuer(), ISSUER);
        assertEquals(account.getAccountName(), ACCOUNT_NAME);
        assertEquals(account.getImageURL(), imageUrl);
        assertEquals(account.getBackgroundColor(), backgroundColor);
    }

    @Test
    public void shouldEqualEquivalentAccount() {
        Account account1 = new Account(ISSUER, ACCOUNT_NAME);
        Account account2 = new Account(ISSUER, ACCOUNT_NAME);

        assertEquals(account1, account2);
        assertEquals(account1.compareTo(account2), 0);
        assertEquals(account2.compareTo(account1), 0);
        assertEquals(account1.hashCode(), account2.hashCode());
    }

    @Test
    public void shouldNotEqualDifferentAccountWithIssuer() {
        Account account1 = new Account(ISSUER, ACCOUNT_NAME);
        Account account2 = new Account(OTHER_ISSUER, ACCOUNT_NAME);

        assertFalse(account1.equals(account2));
        assertEquals(account1.compareTo(account2), -1);
        assertEquals(account2.compareTo(account1), 1);
    }

    @Test
    public void shouldNotEqualDifferentAccountWithAccountName() {
        Account account1 = new Account(ISSUER, ACCOUNT_NAME);
        Account account2 = new Account(ISSUER, OTHER_ACCOUNT_NAME);

        assertFalse(account1.equals(account2));
        assertEquals(account1.compareTo(account2), -1);
        assertEquals(account2.compareTo(account1), 1);
    }

    @Test
    public void shouldHandleNullEquals() {
        Account account = new Account(ISSUER, ACCOUNT_NAME);

        assertFalse(account.equals(null));
        assertEquals(account.compareTo(null), -1);
    }

}