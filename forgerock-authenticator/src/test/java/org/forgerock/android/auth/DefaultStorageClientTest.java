/*
 * Copyright (c) 2020 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class DefaultStorageClientTest extends FRABaseTest {

    private Context context = ApplicationProvider.getApplicationContext();

    private static final boolean CLEAN_UP_DATA = false;

    @After
    public void cleanUp() {
        if(CLEAN_UP_DATA) {
            context.deleteSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT);
            context.deleteSharedPreferences(TEST_SHARED_PREFERENCES_DATA_MECHANISM);
            context.deleteSharedPreferences(TEST_SHARED_PREFERENCES_DATA_NOTIFICATIONS);
            context.deleteSharedPreferences(TEST_SHARED_PREFERENCES_DATA_DEVICE_TOKEN);
        }
    }

    @Test
    public void testInitialization() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);

        assertNotNull(defaultStorage);
    }

    @Test
    public void testStoreAccount() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        defaultStorage.setAccountData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE));

        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);

        defaultStorage.setAccount(account);

        Account accountFromStorage = defaultStorage.getAccount(account.getId());

        assertNotNull(accountFromStorage);
        assertEquals(account.getIssuer(), accountFromStorage.getIssuer());
        assertEquals(account.getAccountName(), accountFromStorage.getAccountName());
    }

    @Test
    public void testStoreMultipleAccounts() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        defaultStorage.setAccountData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE));

        Account account1 = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Account account2 = createAccountWithoutAdditionalData(OTHER_ISSUER, OTHER_ACCOUNT_NAME);

        defaultStorage.setAccount(account1);
        defaultStorage.setAccount(account2);

        Account account1FromStorage = defaultStorage.getAccount(account1.getId());
        Account account2FromStorage = defaultStorage.getAccount(account2.getId());

        assertNotNull(account1FromStorage);
        assertEquals(account1.getIssuer(), account1FromStorage.getIssuer());
        assertEquals(account1.getAccountName(), account1FromStorage.getAccountName());
        assertNotNull(account2FromStorage);
        assertEquals(account2.getIssuer(), account2FromStorage.getIssuer());
        assertEquals(account2.getAccountName(), account2FromStorage.getAccountName());
    }

    @Test
    public void testNoAccountFound() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        defaultStorage.setAccountData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE));

        Account account1 = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Account account2 = createAccountWithoutAdditionalData(OTHER_ISSUER, OTHER_ACCOUNT_NAME);

        defaultStorage.setAccount(account1);

        Account account1FromStorage = defaultStorage.getAccount(account1.getId());
        Account account2FromStorage = defaultStorage.getAccount(account2.getId());

        assertNotNull(account1FromStorage);
        assertEquals(account1.getIssuer(), account1FromStorage.getIssuer());
        assertEquals(account1.getAccountName(), account1FromStorage.getAccountName());
        assertNull(account2FromStorage);
    }

    @Test
    public void testUpdateExistingAccount() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        defaultStorage.setAccountData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE));

        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        defaultStorage.setAccount(account);

        Account accountFromStorage = defaultStorage.getAccount(account.getId());
        assertNotNull(accountFromStorage);
        assertEquals(account.getIssuer(), accountFromStorage.getIssuer());
        assertEquals(account.getAccountName(), accountFromStorage.getAccountName());
        assertNull(accountFromStorage.getImageURL());
        assertNull(accountFromStorage.getBackgroundColor());

        Account updatedAccount = createAccount(ISSUER, ACCOUNT_NAME, IMAGE_URL, BACKGROUND_COLOR);
        defaultStorage.setAccount(updatedAccount);

        Account updatedAccountFromStorage = defaultStorage.getAccount(updatedAccount.getId());
        assertNotNull(updatedAccountFromStorage);
        assertEquals(updatedAccount.getIssuer(), updatedAccountFromStorage.getIssuer());
        assertEquals(updatedAccount.getAccountName(), updatedAccountFromStorage.getAccountName());
        assertEquals(updatedAccount.getImageURL(), updatedAccountFromStorage.getImageURL());
        assertEquals(updatedAccount.getBackgroundColor(), updatedAccountFromStorage.getBackgroundColor());
    }

    @Test
    public void testRemoveExistingAccount() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        defaultStorage.setAccountData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE));

        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        defaultStorage.setAccount(account);

        Account accountFromStorage = defaultStorage.getAccount(account.getId());
        assertNotNull(accountFromStorage);
        assertEquals(account.getId(), accountFromStorage.getId());
        assertTrue(defaultStorage.removeAccount(accountFromStorage));
        assertNull(defaultStorage.getAccount(accountFromStorage.getId()));
    }

    @Test
    public void testStoreOathMechanism() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        defaultStorage.setAccountData(ApplicationProvider.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE));
        defaultStorage.setMechanismData(ApplicationProvider.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_MECHANISM, Context.MODE_PRIVATE));

        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createOathMechanism(MECHANISM_UID, ISSUER, ACCOUNT_NAME,
                OathMechanism.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);

        defaultStorage.setAccount(account);
        defaultStorage.setMechanism(mechanism);

        Account accountFromStorage = defaultStorage.getAccount(account.getId());
        List<Mechanism> mechanismsFromStorage = defaultStorage.getMechanismsForAccount(accountFromStorage);

        assertNotNull(accountFromStorage);
        assertEquals(account.getId(), accountFromStorage.getId());
        assertNotNull(mechanismsFromStorage);
        assertEquals(mechanism.getMechanismUID(), mechanismsFromStorage.get(0).getMechanismUID());
    }

    @Test
    public void testStorePushMechanism() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        defaultStorage.setAccountData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE));
        defaultStorage.setMechanismData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_MECHANISM, Context.MODE_PRIVATE));

        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createPushMechanism(MECHANISM_UID, ISSUER, ACCOUNT_NAME, SECRET,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);

        defaultStorage.setAccount(account);
        defaultStorage.setMechanism(mechanism);

        Account accountFromStorage = defaultStorage.getAccount(account.getId());
        List<Mechanism> mechanismsFromStorage = defaultStorage.getMechanismsForAccount(accountFromStorage);

        assertNotNull(accountFromStorage);
        assertEquals(account.getId(), accountFromStorage.getId());
        assertNotNull(mechanismsFromStorage);
        assertEquals(mechanism.getMechanismUID(), mechanismsFromStorage.get(0).getMechanismUID());
    }

    @Test
    public void testStoreMultipleMechanismsForSameAccount() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        defaultStorage.setAccountData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE));
        defaultStorage.setMechanismData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_MECHANISM, Context.MODE_PRIVATE));

        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism1 = createPushMechanism(MECHANISM_UID, ISSUER, ACCOUNT_NAME, SECRET,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        Mechanism mechanism2 = createOathMechanism(OTHER_MECHANISM_UID, ISSUER, ACCOUNT_NAME,
                OathMechanism.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);

        defaultStorage.setAccount(account);
        defaultStorage.setMechanism(mechanism1);
        defaultStorage.setMechanism(mechanism2);

        Account accountFromStorage = defaultStorage.getAccount(account.getId());
        List<Mechanism> mechanismsFromStorage = defaultStorage.getMechanismsForAccount(accountFromStorage);

        assertNotNull(accountFromStorage);
        assertEquals(account.getId(), accountFromStorage.getId());
        assertNotNull(mechanismsFromStorage);
        assertEquals(mechanismsFromStorage.size(), 2);
    }

    @Test
    public void testStoreRetrieveMechanismByUID() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        defaultStorage.setAccountData(ApplicationProvider.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE));
        defaultStorage.setMechanismData(ApplicationProvider.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_MECHANISM, Context.MODE_PRIVATE));

        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createOathMechanism(MECHANISM_UID, ISSUER, ACCOUNT_NAME,
                OathMechanism.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);

        defaultStorage.setAccount(account);
        defaultStorage.setMechanism(mechanism);

        Mechanism mechanismsFromStorage = defaultStorage.getMechanismByUUID(MECHANISM_UID);

        assertNotNull(mechanismsFromStorage);
        assertEquals(mechanism.getMechanismUID(), mechanismsFromStorage.getMechanismUID());
    }

    @Test
    public void testUpdateExistingMechanism() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        defaultStorage.setAccountData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE));
        defaultStorage.setMechanismData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_MECHANISM, Context.MODE_PRIVATE));

        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createPushMechanism(MECHANISM_UID, ISSUER, ACCOUNT_NAME, SECRET,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);

        defaultStorage.setAccount(account);
        defaultStorage.setMechanism(mechanism);

        Account accountFromStorage = defaultStorage.getAccount(account.getId());
        PushMechanism pushMechanismFromStorage = (PushMechanism) defaultStorage.getMechanismsForAccount(accountFromStorage).get(0);

        assertNotNull(accountFromStorage);
        assertEquals(account.getId(), accountFromStorage.getId());
        assertNotNull(pushMechanismFromStorage);
        assertEquals(pushMechanismFromStorage.getMechanismUID(), MECHANISM_UID);
        assertEquals(pushMechanismFromStorage.getRegistrationEndpoint(), REGISTRATION_ENDPOINT);

        Mechanism updatedMechanism = createPushMechanism(MECHANISM_UID, ISSUER, ACCOUNT_NAME, SECRET,
                OTHER_REGISTRATION_ENDPOINT, OTHER_AUTHENTICATION_ENDPOINT);
        defaultStorage.setMechanism(updatedMechanism);

        PushMechanism updatedPushMechanismFromStorage = (PushMechanism) defaultStorage.getMechanismsForAccount(accountFromStorage).get(0);

        assertNotNull(updatedPushMechanismFromStorage);
        assertEquals(updatedPushMechanismFromStorage.getMechanismUID(), MECHANISM_UID);
        assertEquals(updatedPushMechanismFromStorage.getRegistrationEndpoint(), OTHER_REGISTRATION_ENDPOINT);
        assertEquals(updatedPushMechanismFromStorage.getAuthenticationEndpoint(), OTHER_AUTHENTICATION_ENDPOINT);
    }

    @Test
    public void testRemoveExistingMechanism() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        defaultStorage.setAccountData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE));
        defaultStorage.setMechanismData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_MECHANISM, Context.MODE_PRIVATE));

        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createOathMechanism(OTHER_MECHANISM_UID, ISSUER, ACCOUNT_NAME,
                OathMechanism.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);

        defaultStorage.setAccount(account);
        defaultStorage.setMechanism(mechanism);

        Account accountFromStorage = defaultStorage.getAccount(account.getId());
        OathMechanism oathMechanismFromStorage = (OathMechanism) defaultStorage.getMechanismsForAccount(accountFromStorage).get(0);

        assertNotNull(accountFromStorage);
        assertEquals(account.getId(), accountFromStorage.getId());
        assertTrue(defaultStorage.removeMechanism(oathMechanismFromStorage));
        assertEquals(defaultStorage.getMechanismsForAccount(accountFromStorage).size(), 0);
    }

    @Test
    public void testStoreNotification() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        defaultStorage.setAccountData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE));
        defaultStorage.setMechanismData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_MECHANISM, Context.MODE_PRIVATE));
        defaultStorage.setNotificationData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_NOTIFICATIONS, Context.MODE_PRIVATE));

        Calendar timeAdded = Calendar.getInstance();

        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createPushMechanism(MECHANISM_UID, ISSUER, ACCOUNT_NAME, SECRET,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        PushNotification pushNotification = createPushNotification(MECHANISM_UID, MESSAGE_ID, CHALLENGE,
                AMLB_COOKIE, timeAdded, TTL);

        defaultStorage.setAccount(account);
        defaultStorage.setMechanism(mechanism);
        defaultStorage.setNotification(pushNotification);

        Account accountFromStorage = defaultStorage.getAccount(account.getId());
        PushMechanism pushMechanismFromStorage = (PushMechanism) defaultStorage.getMechanismsForAccount(accountFromStorage).get(0);
        PushNotification pushNotificationFromStorage = defaultStorage.getAllNotificationsForMechanism(pushMechanismFromStorage).get(0);

        assertNotNull(accountFromStorage);
        assertNotNull(pushMechanismFromStorage);
        assertNotNull(pushNotificationFromStorage);
        assertEquals(pushNotificationFromStorage.getMechanismUID(), MECHANISM_UID);
        assertEquals(pushNotificationFromStorage.getMessageId(), MESSAGE_ID);
    }

    @Test
    public void testNoNotificationFound() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        defaultStorage.setNotificationData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_NOTIFICATIONS, Context.MODE_PRIVATE));

        Calendar timeAdded = Calendar.getInstance();

        PushNotification pushNotification = createPushNotification(MECHANISM_UID, MESSAGE_ID, CHALLENGE,
                AMLB_COOKIE, timeAdded, TTL);
        defaultStorage.setNotification(pushNotification);
        PushNotification pushNotificationFromStorage = defaultStorage.getNotification("INVALID_ID");

        assertNull(pushNotificationFromStorage);
    }

    @Test
    public void testShouldGetNotificationById() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        defaultStorage.setAccountData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE));
        defaultStorage.setMechanismData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_MECHANISM, Context.MODE_PRIVATE));
        defaultStorage.setNotificationData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_NOTIFICATIONS, Context.MODE_PRIVATE));

        Calendar timeAdded = Calendar.getInstance();

        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createPushMechanism(MECHANISM_UID, ISSUER, ACCOUNT_NAME, SECRET,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        PushNotification pushNotification = createPushNotification(MECHANISM_UID, MESSAGE_ID, CHALLENGE,
                AMLB_COOKIE, timeAdded, TTL);

        defaultStorage.setAccount(account);
        defaultStorage.setMechanism(mechanism);
        defaultStorage.setNotification(pushNotification);

        Account accountFromStorage = defaultStorage.getAccount(account.getId());
        PushMechanism pushMechanismFromStorage = (PushMechanism) defaultStorage.getMechanismsForAccount(accountFromStorage).get(0);
        PushNotification pushNotificationFromStorage = defaultStorage.getNotification(pushNotification.getId());

        assertNotNull(accountFromStorage);
        assertNotNull(pushMechanismFromStorage);
        assertNotNull(pushNotificationFromStorage);
        assertEquals(pushNotificationFromStorage.getMechanismUID(), MECHANISM_UID);
        assertEquals(pushNotificationFromStorage.getMessageId(), MESSAGE_ID);
    }

    @Test
    public void testShouldGetNotificationByMessageId() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        defaultStorage.setAccountData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE));
        defaultStorage.setMechanismData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_MECHANISM, Context.MODE_PRIVATE));
        defaultStorage.setNotificationData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_NOTIFICATIONS, Context.MODE_PRIVATE));

        Calendar timeAdded = Calendar.getInstance();

        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createPushMechanism(MECHANISM_UID, ISSUER, ACCOUNT_NAME, SECRET,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        PushNotification pushNotification = createPushNotification(MECHANISM_UID, MESSAGE_ID, CHALLENGE,
                AMLB_COOKIE, timeAdded, TTL);

        defaultStorage.setAccount(account);
        defaultStorage.setMechanism(mechanism);
        defaultStorage.setNotification(pushNotification);

        Account accountFromStorage = defaultStorage.getAccount(account.getId());
        PushMechanism pushMechanismFromStorage = (PushMechanism) defaultStorage.getMechanismsForAccount(accountFromStorage).get(0);
        PushNotification pushNotificationFromStorage = defaultStorage.getNotificationByMessageId(pushNotification.getMessageId());

        assertNotNull(accountFromStorage);
        assertNotNull(pushMechanismFromStorage);
        assertNotNull(pushNotificationFromStorage);
        assertEquals(pushNotificationFromStorage.getMechanismUID(), MECHANISM_UID);
        assertEquals(pushNotificationFromStorage.getMessageId(), MESSAGE_ID);
    }

    @Test
    public void testStoreMultipleNotificationsForSameAccount() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        defaultStorage.setAccountData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE));
        defaultStorage.setMechanismData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_MECHANISM, Context.MODE_PRIVATE));
        defaultStorage.setNotificationData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_NOTIFICATIONS, Context.MODE_PRIVATE));

        Calendar timeAdded1 = Calendar.getInstance();
        Calendar timeAdded2 = Calendar.getInstance();
        Calendar timeAdded3 = Calendar.getInstance();
        timeAdded2.setTimeInMillis(timeAdded2.getTimeInMillis()+100);
        timeAdded3.setTimeInMillis(timeAdded3.getTimeInMillis()+200);

        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createPushMechanism(MECHANISM_UID, ISSUER, ACCOUNT_NAME, SECRET,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        PushNotification pushNotification1 = createPushNotification(MECHANISM_UID, MESSAGE_ID, CHALLENGE,
                AMLB_COOKIE, timeAdded1, TTL);
        PushNotification pushNotification2 = createPushNotification(MECHANISM_UID, OTHER_MESSAGE_ID, CHALLENGE,
                AMLB_COOKIE, timeAdded2, TTL);
        PushNotification pushNotification3 = createPushNotification(MECHANISM_UID, OTHER_MESSAGE_ID, CHALLENGE,
                AMLB_COOKIE, timeAdded3, TTL);

        defaultStorage.setAccount(account);
        defaultStorage.setMechanism(mechanism);
        defaultStorage.setNotification(pushNotification1);
        defaultStorage.setNotification(pushNotification2);
        defaultStorage.setNotification(pushNotification3);

        Account accountFromStorage = defaultStorage.getAccount(account.getId());
        PushMechanism pushMechanismFromStorage = (PushMechanism) defaultStorage.getMechanismsForAccount(accountFromStorage).get(0);
        List<PushNotification> notificationsFromStorage = defaultStorage.getAllNotificationsForMechanism(pushMechanismFromStorage);

        assertNotNull(accountFromStorage);
        assertNotNull(pushMechanismFromStorage);
        assertNotNull(notificationsFromStorage);
        assertEquals(notificationsFromStorage.size(), 3);
    }

    @Test
    public void testStoreMultipleNotificationsForDifferentAccounts() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        defaultStorage.setAccountData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE));
        defaultStorage.setMechanismData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_MECHANISM, Context.MODE_PRIVATE));
        defaultStorage.setNotificationData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_NOTIFICATIONS, Context.MODE_PRIVATE));

        Calendar timeAdded1 = Calendar.getInstance();
        Calendar timeAdded2 = Calendar.getInstance();
        timeAdded2.setTimeInMillis(timeAdded2.getTimeInMillis()+100);

        Account account1 = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Account account2 = createAccountWithoutAdditionalData(OTHER_ISSUER, OTHER_ACCOUNT_NAME);
        Mechanism mechanism1 = createPushMechanism(MECHANISM_UID, ISSUER, ACCOUNT_NAME, SECRET,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        Mechanism mechanism2 = createPushMechanism(OTHER_MECHANISM_UID, OTHER_ISSUER, OTHER_ACCOUNT_NAME, SECRET,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        PushNotification pushNotification1 = createPushNotification(MECHANISM_UID, MESSAGE_ID, CHALLENGE,
                AMLB_COOKIE, timeAdded1, TTL);
        PushNotification pushNotification2 = createPushNotification(MECHANISM_UID, OTHER_MESSAGE_ID, CHALLENGE,
                AMLB_COOKIE, timeAdded2, TTL);
        PushNotification pushNotification3 = createPushNotification(OTHER_MECHANISM_UID, MESSAGE_ID, CHALLENGE,
                AMLB_COOKIE, timeAdded2, TTL);

        defaultStorage.setAccount(account1);
        defaultStorage.setAccount(account2);
        defaultStorage.setMechanism(mechanism1);
        defaultStorage.setMechanism(mechanism2);
        defaultStorage.setNotification(pushNotification1);
        defaultStorage.setNotification(pushNotification2);
        defaultStorage.setNotification(pushNotification3);

        Account accountFromStorage1 = defaultStorage.getAccount(account1.getId());
        Account accountFromStorage2 = defaultStorage.getAccount(account2.getId());
        PushMechanism pushMechanismFromStorage1 = (PushMechanism) defaultStorage.getMechanismsForAccount(accountFromStorage1).get(0);
        PushMechanism pushMechanismFromStorage2 = (PushMechanism) defaultStorage.getMechanismsForAccount(accountFromStorage2).get(0);
        List<PushNotification> notificationsFromStorage1 = defaultStorage.getAllNotificationsForMechanism(pushMechanismFromStorage1);
        List<PushNotification> notificationsFromStorage2 = defaultStorage.getAllNotificationsForMechanism(pushMechanismFromStorage2);

        assertNotNull(accountFromStorage1);
        assertNotNull(pushMechanismFromStorage1);
        assertNotNull(notificationsFromStorage1);
        assertNotNull(accountFromStorage2);
        assertNotNull(pushMechanismFromStorage2);
        assertNotNull(notificationsFromStorage2);
        assertEquals(notificationsFromStorage1.size(), 2);
        assertEquals(notificationsFromStorage2.size(), 1);
    }

    @Test
    public void testUpdateExistingNotification() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        defaultStorage.setAccountData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE));
        defaultStorage.setMechanismData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_MECHANISM, Context.MODE_PRIVATE));
        defaultStorage.setNotificationData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_NOTIFICATIONS, Context.MODE_PRIVATE));

        Calendar timeAdded = Calendar.getInstance();
        boolean approved = false;
        boolean pending = true;

        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createPushMechanism(MECHANISM_UID, ISSUER, ACCOUNT_NAME, SECRET,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        PushNotification pushNotification = createPushNotification(MECHANISM_UID, MESSAGE_ID, CHALLENGE,
                AMLB_COOKIE, timeAdded, TTL);

        defaultStorage.setAccount(account);
        defaultStorage.setMechanism(mechanism);
        defaultStorage.setNotification(pushNotification);

        Account accountFromStorage = defaultStorage.getAccount(account.getId());
        PushMechanism pushMechanismFromStorage = (PushMechanism) defaultStorage.getMechanismsForAccount(accountFromStorage).get(0);
        PushNotification pushNotificationFromStorage = defaultStorage.getAllNotificationsForMechanism(pushMechanismFromStorage).get(0);

        assertNotNull(accountFromStorage);
        assertNotNull(pushMechanismFromStorage);
        assertNotNull(pushNotificationFromStorage);
        assertEquals(pushNotificationFromStorage.getMechanismUID(), MECHANISM_UID);
        assertEquals(pushNotificationFromStorage.getMessageId(), MESSAGE_ID);

        PushNotification updatedPushNotification = createPushNotification(MECHANISM_UID, MESSAGE_ID, CHALLENGE,
                AMLB_COOKIE, timeAdded, timeAdded, TTL, approved, pending);

        defaultStorage.setNotification(updatedPushNotification);

        PushNotification updatedPushNotificationFromStorage = defaultStorage.getAllNotificationsForMechanism(pushMechanismFromStorage).get(0);

        assertNotNull(updatedPushNotificationFromStorage);
        assertEquals(updatedPushNotificationFromStorage.getMessageId(), MESSAGE_ID);
        assertEquals(updatedPushNotificationFromStorage.isApproved(), approved);
        assertEquals(updatedPushNotificationFromStorage.isPending(), pending);
    }

    @Test
    public void testRemoveExistingNotification() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        defaultStorage.setAccountData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE));
        defaultStorage.setMechanismData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_MECHANISM, Context.MODE_PRIVATE));
        defaultStorage.setNotificationData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_NOTIFICATIONS, Context.MODE_PRIVATE));

        Calendar timeAdded = Calendar.getInstance();

        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createPushMechanism(MECHANISM_UID, ISSUER, ACCOUNT_NAME, SECRET,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        PushNotification pushNotification = createPushNotification(MECHANISM_UID, MESSAGE_ID, CHALLENGE,
                AMLB_COOKIE, timeAdded, TTL);

        defaultStorage.setAccount(account);
        defaultStorage.setMechanism(mechanism);
        defaultStorage.setNotification(pushNotification);

        assertFalse(defaultStorage.isEmpty());

        Account accountFromStorage = defaultStorage.getAccount(account.getId());
        PushMechanism pushMechanismFromStorage = (PushMechanism) defaultStorage.getMechanismsForAccount(accountFromStorage).get(0);
        PushNotification pushNotificationFromStorage = defaultStorage.getAllNotificationsForMechanism(pushMechanismFromStorage).get(0);

        assertNotNull(accountFromStorage);
        assertNotNull(pushMechanismFromStorage);
        assertNotNull(pushNotificationFromStorage);
        assertEquals(pushNotificationFromStorage.getMessageId(), MESSAGE_ID);
        assertTrue(defaultStorage.removeNotification(pushNotificationFromStorage));
        assertEquals(defaultStorage.getAllNotificationsForMechanism(pushMechanismFromStorage).size(), 0);
    }

    @Test
    public void testRemoveAllData() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        defaultStorage.setAccountData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE));
        defaultStorage.setMechanismData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_MECHANISM, Context.MODE_PRIVATE));
        defaultStorage.setNotificationData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_NOTIFICATIONS, Context.MODE_PRIVATE));
        defaultStorage.setDeviceTokenData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_DEVICE_TOKEN, Context.MODE_PRIVATE));

        Calendar timeAdded = Calendar.getInstance();

        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createPushMechanism(MECHANISM_UID, ISSUER, ACCOUNT_NAME, SECRET,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        PushNotification pushNotification = createPushNotification(MECHANISM_UID, MESSAGE_ID, CHALLENGE,
                AMLB_COOKIE, timeAdded, TTL);
        PushDeviceToken deviceToken = new PushDeviceToken("testToken123", timeAdded);

        defaultStorage.setAccount(account);
        defaultStorage.setMechanism(mechanism);
        defaultStorage.setNotification(pushNotification);
        defaultStorage.setPushDeviceToken(deviceToken);

        assertFalse(defaultStorage.isEmpty());

        defaultStorage.removeAll();

        assertTrue(defaultStorage.isEmpty());
    }

    @Test
    public void testStorePushDeviceToken() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        defaultStorage.setDeviceTokenData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_DEVICE_TOKEN, Context.MODE_PRIVATE));
        
        // Create test token and its JSON representation
        Calendar timeAdded = Calendar.getInstance();
        timeAdded.setTimeInMillis(1704099600000L);
        PushDeviceToken deviceToken = new PushDeviceToken("testToken123", timeAdded);
        String expectedJson = deviceToken.toJson();

        // Store the device token
        boolean result = defaultStorage.setPushDeviceToken(deviceToken);
        assertTrue(result);

        // Retrieve and verify
        PushDeviceToken retrieved = defaultStorage.getPushDeviceToken();
        assertNotNull(retrieved);
        assertEquals(expectedJson, retrieved.toJson());
        assertEquals("testToken123", retrieved.getTokenId());
        assertEquals(timeAdded.getTimeInMillis(), retrieved.getTimeAdded().getTimeInMillis());
    }

    @Test
    public void testGetPushDeviceTokenWhenNoneExists() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        defaultStorage.setDeviceTokenData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_DEVICE_TOKEN, Context.MODE_PRIVATE));

        PushDeviceToken retrieved = defaultStorage.getPushDeviceToken();
        assertNull(retrieved);
    }

    @Test
    public void testPushDeviceTokenCorruptionRecovery() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        
        // Create a mock SharedPreferences that will throw an exception when getString is called
        SharedPreferences mockPrefs = mock(SharedPreferences.class);
        SharedPreferences.Editor mockEditor = mock(SharedPreferences.Editor.class);
        
        // Configure mock to throw RuntimeException (simulating the corruption scenario)
        when(mockPrefs.getString("deviceToken", null))
            .thenThrow(new RuntimeException("org.json.JSONException: Unterminated string at character 804"));
        
        // Configure mock editor for cleanup
        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.remove("deviceToken")).thenReturn(mockEditor);
        when(mockEditor.commit()).thenReturn(true);
        
        // Inject the mock into the storage client
        defaultStorage.setDeviceTokenData(mockPrefs);
        
        // This should not throw an exception and should return null
        PushDeviceToken retrieved = defaultStorage.getPushDeviceToken();
        assertNull(retrieved);
        
        // Verify that remove was called (cleanup happened)
        verify(mockEditor).remove("deviceToken");
        verify(mockEditor).commit();
    }

    @Test
    public void testPushDeviceTokenBackwardCompatibility() {
        DefaultStorageClient defaultStorage = new DefaultStorageClient(context);
        
        // Create a mock SharedPreferences that returns old format JSON (with Calendar.toString())
        SharedPreferences mockPrefs = mock(SharedPreferences.class);
        SharedPreferences.Editor mockEditor = mock(SharedPreferences.Editor.class);
        
        // Simulate old format JSON that would have caused the corruption
        String oldFormatJson = "{\"tokenId\":\"oldToken123\",\"timeAdded\":\"java.util.GregorianCalendar[time=1704099600000,areFieldsSet=true,areAllFieldsSet=true,lenient=true,zone=java.util.SimpleTimeZone[id=UTC,offset=0,dstSavings=3600000,useDaylight=false,startYear=0,startMode=0,startMonth=0,startDay=0,startDayOfWeek=0,startTime=0,startTimeMode=0,endMode=0,endMonth=0,endDay=0,endDayOfWeek=0,endTime=0,endTimeMode=0],firstDayOfWeek=2,minimalDaysInFirstWeek=1,ERA=1,YEAR=2025,MONTH=8,WEEK_OF_YEAR=37,WEEK_OF_MONTH=2,DAY_OF_MONTH=14,DAY_OF_YEAR=257,DAY_OF_WEEK=1,DAY_OF_WEEK_IN_MONTH=2,AM_PM=1,HOUR=6,HOUR_OF_DAY=18,MINUTE=52,SECOND=21,MILLISECOND=637,ZONE_OFFSET=0,DST_OFFSET=0]\"}";
        
        when(mockPrefs.getString("deviceToken", null)).thenReturn(oldFormatJson);
        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.remove("deviceToken")).thenReturn(mockEditor);
        when(mockEditor.commit()).thenReturn(true);
        
        // Inject the mock into the storage client
        defaultStorage.setDeviceTokenData(mockPrefs);
        
        // This should handle the old format gracefully
        PushDeviceToken retrieved = defaultStorage.getPushDeviceToken();
        assertNotNull(retrieved);
        assertEquals("oldToken123", retrieved.getTokenId());
        // Time should be set to current time as fallback since optLong("timeAdded") returns 0 for the string value
        assertTrue(retrieved.getTimeAdded().getTimeInMillis() > 0);
    }

    @Test
    public void testNewFormatPushDeviceTokenSerialization() {
        // Test serialization format without involving DefaultStorageClient
        Calendar timeAdded = Calendar.getInstance();
        timeAdded.setTimeInMillis(1704099600000L); // Fixed timestamp for testing
        PushDeviceToken deviceToken = new PushDeviceToken("newFormatToken", timeAdded);

        // Verify new format serialization stores timestamp as long
        String json = deviceToken.toJson();
        assertTrue("New format should contain timeAdded as number", json.contains("\"timeAdded\":1704099600000"));
        assertFalse("New format should not contain Calendar object string", json.contains("GregorianCalendar"));

        // Test deserialization
        PushDeviceToken deserialized = PushDeviceToken.deserialize(json);
        assertNotNull(deserialized);
        assertEquals("newFormatToken", deserialized.getTokenId());
        assertEquals(1704099600000L, deserialized.getTimeAdded().getTimeInMillis());
    }

}