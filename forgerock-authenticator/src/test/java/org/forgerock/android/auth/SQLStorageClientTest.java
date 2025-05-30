/*
 * Copyright (c) 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.forgerock.android.auth.exception.InvalidNotificationException;
import org.forgerock.android.auth.exception.MechanismCreationException;
import org.junit.After;
import org.junit.Before;
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

@RunWith(RobolectricTestRunner.class)
public class SQLStorageClientTest extends FRABaseTest {

    private Context context;
    private SQLStorageClient storageClient;

    // Test constants
    private static final String ISSUER = "issuer";
    private static final String ACCOUNT_NAME = "account_name";
    private static final String OTHER_ISSUER = "other_issuer";
    private static final String OTHER_ACCOUNT_NAME = "other_account_name";
    private static final String IMAGE_URL = "image_url";
    private static final String BACKGROUND_COLOR = "background_color";
    private static final String MECHANISM_UID = "mechanism_uid";
    private static final String OTHER_MECHANISM_UID = "other_mechanism_uid";
    private static final String SECRET = "secret";
    private static final String REGISTRATION_ENDPOINT = "registration_endpoint";
    private static final String AUTHENTICATION_ENDPOINT = "authentication_endpoint";
    private static final String OTHER_REGISTRATION_ENDPOINT = "other_registration_endpoint";
    private static final String OTHER_AUTHENTICATION_ENDPOINT = "other_authentication_endpoint";
    private static final String ALGORITHM = "algorithm";
    private static final int DIGITS = 6;
    private static final int COUNTER = 0;
    private static final int PERIOD = 30;
    private static final String MESSAGE_ID = "message_id";
    private static final String OTHER_MESSAGE_ID = "other_message_id";
    private static final String CHALLENGE = "challenge";
    private static final String AMLB_COOKIE = "amlb_cookie";
    private static final int TTL = 120;
    private static final boolean approved = true;
    private static final boolean pending = false;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        storageClient = new SQLStorageClient(context);
    }

    @After
    public void tearDown() {
        storageClient.removeAll();
    }

    @Test
    public void testInitialization() {
        assertNotNull(storageClient);
    }

    @Test
    public void testStoreAccount() {
        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);

        storageClient.setAccount(account);

        Account accountFromStorage = storageClient.getAccount(account.getId());

        assertNotNull(accountFromStorage);
        assertEquals(account.getIssuer(), accountFromStorage.getIssuer());
        assertEquals(account.getAccountName(), accountFromStorage.getAccountName());
    }

    @Test
    public void testStoreMultipleAccounts() {
        Account account1 = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Account account2 = createAccountWithoutAdditionalData(OTHER_ISSUER, OTHER_ACCOUNT_NAME);

        storageClient.setAccount(account1);
        storageClient.setAccount(account2);

        Account account1FromStorage = storageClient.getAccount(account1.getId());
        Account account2FromStorage = storageClient.getAccount(account2.getId());

        assertNotNull(account1FromStorage);
        assertEquals(account1.getIssuer(), account1FromStorage.getIssuer());
        assertEquals(account1.getAccountName(), account1FromStorage.getAccountName());
        assertNotNull(account2FromStorage);
        assertEquals(account2.getIssuer(), account2FromStorage.getIssuer());
        assertEquals(account2.getAccountName(), account2FromStorage.getAccountName());
    }

    @Test
    public void testNoAccountFound() {
        Account account1 = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Account account2 = createAccountWithoutAdditionalData(OTHER_ISSUER, OTHER_ACCOUNT_NAME);

        storageClient.setAccount(account1);

        Account account1FromStorage = storageClient.getAccount(account1.getId());
        Account account2FromStorage = storageClient.getAccount(account2.getId());

        assertNotNull(account1FromStorage);
        assertEquals(account1.getIssuer(), account1FromStorage.getIssuer());
        assertEquals(account1.getAccountName(), account1FromStorage.getAccountName());
        assertNull(account2FromStorage);
    }

    @Test
    public void testUpdateExistingAccount() {
        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        storageClient.setAccount(account);

        Account accountFromStorage = storageClient.getAccount(account.getId());
        assertNotNull(accountFromStorage);
        assertEquals(account.getIssuer(), accountFromStorage.getIssuer());
        assertEquals(account.getAccountName(), accountFromStorage.getAccountName());
        assertNull(accountFromStorage.getImageURL());
        assertNull(accountFromStorage.getBackgroundColor());

        Account updatedAccount = createAccount(ISSUER, ACCOUNT_NAME, IMAGE_URL, BACKGROUND_COLOR);
        storageClient.setAccount(updatedAccount);

        Account updatedAccountFromStorage = storageClient.getAccount(updatedAccount.getId());
        assertNotNull(updatedAccountFromStorage);
        assertEquals(updatedAccount.getIssuer(), updatedAccountFromStorage.getIssuer());
        assertEquals(updatedAccount.getAccountName(), updatedAccountFromStorage.getAccountName());
        assertEquals(updatedAccount.getImageURL(), updatedAccountFromStorage.getImageURL());
        assertEquals(updatedAccount.getBackgroundColor(), updatedAccountFromStorage.getBackgroundColor());
    }

    @Test
    public void testRemoveExistingAccount() {
        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        storageClient.setAccount(account);

        Account accountFromStorage = storageClient.getAccount(account.getId());
        assertNotNull(accountFromStorage);
        assertEquals(account.getId(), accountFromStorage.getId());
        
        assertTrue(storageClient.removeAccount(accountFromStorage));
        assertNull(storageClient.getAccount(accountFromStorage.getId()));
    }

    @Test
    public void testStoreOathMechanism() throws MechanismCreationException {
        String uniqueMechanismUID = MECHANISM_UID + "_oath";
        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createOathMechanism(uniqueMechanismUID, ISSUER, ACCOUNT_NAME,
                OathMechanism.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);
        storageClient.setAccount(account);
        storageClient.setMechanism(mechanism);
        Account accountFromStorage = storageClient.getAccount(account.getId());
        List<Mechanism> mechanismsFromStorage = storageClient.getMechanismsForAccount(accountFromStorage);
        assertNotNull(accountFromStorage);
        assertEquals(account.getId(), accountFromStorage.getId());
        assertNotNull(mechanismsFromStorage);
        assertFalse("Mechanisms list should not be empty", mechanismsFromStorage.isEmpty());
        assertEquals(uniqueMechanismUID, mechanismsFromStorage.get(0).getMechanismUID());
    }

    @Test
    public void testStorePushMechanism() throws MechanismCreationException {
        String uniqueMechanismUID = MECHANISM_UID + "_push";
        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createPushMechanism(uniqueMechanismUID, ISSUER, ACCOUNT_NAME, SECRET,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        storageClient.setAccount(account);
        storageClient.setMechanism(mechanism);
        Account accountFromStorage = storageClient.getAccount(account.getId());
        List<Mechanism> mechanismsFromStorage = storageClient.getMechanismsForAccount(accountFromStorage);
        assertNotNull(accountFromStorage);
        assertEquals(account.getId(), accountFromStorage.getId());
        assertNotNull(mechanismsFromStorage);
        assertFalse("Mechanisms list should not be empty", mechanismsFromStorage.isEmpty());
        assertEquals(uniqueMechanismUID, mechanismsFromStorage.get(0).getMechanismUID());
    }

    @Test
    public void testStoreMultipleMechanismsForSameAccount() throws MechanismCreationException {
        String uniqueMechanismUID1 = MECHANISM_UID + "_multi1";
        String uniqueMechanismUID2 = OTHER_MECHANISM_UID + "_multi2";
        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism1 = createPushMechanism(uniqueMechanismUID1, ISSUER, ACCOUNT_NAME, SECRET,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        Mechanism mechanism2 = createOathMechanism(uniqueMechanismUID2, ISSUER, ACCOUNT_NAME,
                OathMechanism.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);
        storageClient.setAccount(account);
        storageClient.setMechanism(mechanism1);
        storageClient.setMechanism(mechanism2);
        Account accountFromStorage = storageClient.getAccount(account.getId());
        List<Mechanism> mechanismsFromStorage = storageClient.getMechanismsForAccount(accountFromStorage);
        assertNotNull(accountFromStorage);
        assertEquals(account.getId(), accountFromStorage.getId());
        assertNotNull(mechanismsFromStorage);
        assertEquals("Should have 2 mechanisms", 2, mechanismsFromStorage.size());
    }

    @Test
    public void testStoreRetrieveMechanismByUID() throws MechanismCreationException {
        String uniqueMechanismUID = MECHANISM_UID + "_retrieve";
        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createOathMechanism(uniqueMechanismUID, ISSUER, ACCOUNT_NAME,
                OathMechanism.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);
        storageClient.setAccount(account);
        storageClient.setMechanism(mechanism);
        Mechanism mechanismFromStorage = storageClient.getMechanismByUUID(uniqueMechanismUID);
        assertNotNull(mechanismFromStorage);
        assertEquals(uniqueMechanismUID, mechanismFromStorage.getMechanismUID());
    }

    @Test
    public void testUpdateExistingMechanism() throws MechanismCreationException {
        String uniqueMechanismUID = MECHANISM_UID + "_update";
        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createPushMechanism(uniqueMechanismUID, ISSUER, ACCOUNT_NAME, SECRET,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        storageClient.setAccount(account);
        storageClient.setMechanism(mechanism);
        Account accountFromStorage = storageClient.getAccount(account.getId());
        List<Mechanism> mechanismsFromStorage = storageClient.getMechanismsForAccount(accountFromStorage);
        assertNotNull(accountFromStorage);
        assertEquals(account.getId(), accountFromStorage.getId());
        assertNotNull(mechanismsFromStorage);
        assertFalse("Mechanisms list should not be empty", mechanismsFromStorage.isEmpty());
        PushMechanism pushMechanismFromStorage = (PushMechanism) mechanismsFromStorage.get(0);
        assertEquals(uniqueMechanismUID, pushMechanismFromStorage.getMechanismUID());
        assertEquals(REGISTRATION_ENDPOINT, pushMechanismFromStorage.getRegistrationEndpoint());
        Mechanism updatedMechanism = createPushMechanism(uniqueMechanismUID, ISSUER, ACCOUNT_NAME, SECRET,
                OTHER_REGISTRATION_ENDPOINT, OTHER_AUTHENTICATION_ENDPOINT);
        storageClient.setMechanism(updatedMechanism);
        List<Mechanism> updatedMechanismsFromStorage = storageClient.getMechanismsForAccount(accountFromStorage);
        assertFalse("Mechanisms list should not be empty after update", updatedMechanismsFromStorage.isEmpty());
        PushMechanism updatedPushMechanismFromStorage = (PushMechanism) updatedMechanismsFromStorage.get(0);
        assertNotNull(updatedPushMechanismFromStorage);
        assertEquals(uniqueMechanismUID, updatedPushMechanismFromStorage.getMechanismUID());
        assertEquals(OTHER_REGISTRATION_ENDPOINT, updatedPushMechanismFromStorage.getRegistrationEndpoint());
        assertEquals(OTHER_AUTHENTICATION_ENDPOINT, updatedPushMechanismFromStorage.getAuthenticationEndpoint());
    }

    @Test
    public void testRemoveExistingMechanism() throws MechanismCreationException {
        String uniqueMechanismUID = OTHER_MECHANISM_UID + "_remove";
        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createOathMechanism(uniqueMechanismUID, ISSUER, ACCOUNT_NAME,
                OathMechanism.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);
        storageClient.setAccount(account);
        storageClient.setMechanism(mechanism);
        Account accountFromStorage = storageClient.getAccount(account.getId());
        List<Mechanism> mechanismsFromStorage = storageClient.getMechanismsForAccount(accountFromStorage);
        assertNotNull(accountFromStorage);
        assertEquals(account.getId(), accountFromStorage.getId());
        assertNotNull(mechanismsFromStorage);
        assertFalse("Mechanisms list should not be empty", mechanismsFromStorage.isEmpty());
        OathMechanism oathMechanismFromStorage = (OathMechanism) mechanismsFromStorage.get(0);
        assertTrue(storageClient.removeMechanism(oathMechanismFromStorage));
        assertEquals("Mechanisms list should be empty after removal", 0, storageClient.getMechanismsForAccount(accountFromStorage).size());
    }

    @Test
    public void testStoreNotification() throws InvalidNotificationException, MechanismCreationException {
        String uniqueMechanismUID = MECHANISM_UID + "_notif";
        String uniqueMessageId = MESSAGE_ID + "_notif";
        Calendar timeAdded = Calendar.getInstance();
        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createPushMechanism(uniqueMechanismUID, ISSUER, ACCOUNT_NAME, SECRET,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        PushNotification pushNotification = createPushNotification(uniqueMechanismUID, uniqueMessageId, CHALLENGE,
                AMLB_COOKIE, timeAdded, TTL);
        storageClient.setAccount(account);
        storageClient.setMechanism(mechanism);
        storageClient.setNotification(pushNotification);
        Account accountFromStorage = storageClient.getAccount(account.getId());
        List<Mechanism> mechanismsFromStorage = storageClient.getMechanismsForAccount(accountFromStorage);
        assertNotNull(accountFromStorage);
        assertNotNull(mechanismsFromStorage);
        assertFalse("Mechanisms list should not be empty", mechanismsFromStorage.isEmpty());
        PushMechanism pushMechanismFromStorage = (PushMechanism) mechanismsFromStorage.get(0);
        List<PushNotification> notificationsFromStorage = storageClient.getAllNotificationsForMechanism(pushMechanismFromStorage);
        assertFalse("Notifications list should not be empty", notificationsFromStorage.isEmpty());
        PushNotification pushNotificationFromStorage = notificationsFromStorage.get(0);
        assertNotNull(pushNotificationFromStorage);
        assertEquals(uniqueMechanismUID, pushNotificationFromStorage.getMechanismUID());
        assertEquals(uniqueMessageId, pushNotificationFromStorage.getMessageId());
    }

    @Test
    public void testStoreMultipleNotificationsForSameAccount() throws InvalidNotificationException, MechanismCreationException {
        String uniqueMechanismUID = MECHANISM_UID + "_multiNotif";
        String uniqueMessageId1 = MESSAGE_ID + "_multi1";
        String uniqueMessageId2 = OTHER_MESSAGE_ID + "_multi2";
        String uniqueMessageId3 = OTHER_MESSAGE_ID + "_multi3";
        Calendar timeAdded1 = Calendar.getInstance();
        Calendar timeAdded2 = Calendar.getInstance();
        Calendar timeAdded3 = Calendar.getInstance();
        timeAdded2.setTimeInMillis(timeAdded2.getTimeInMillis() + 100);
        timeAdded3.setTimeInMillis(timeAdded3.getTimeInMillis() + 200);
        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createPushMechanism(uniqueMechanismUID, ISSUER, ACCOUNT_NAME, SECRET,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        PushNotification pushNotification1 = createPushNotification(uniqueMechanismUID, uniqueMessageId1, CHALLENGE,
                AMLB_COOKIE, timeAdded1, TTL);
        PushNotification pushNotification2 = createPushNotification(uniqueMechanismUID, uniqueMessageId2, CHALLENGE,
                AMLB_COOKIE, timeAdded2, TTL);
        PushNotification pushNotification3 = createPushNotification(uniqueMechanismUID, uniqueMessageId3, CHALLENGE,
                AMLB_COOKIE, timeAdded3, TTL);
        storageClient.setAccount(account);
        storageClient.setMechanism(mechanism);
        storageClient.setNotification(pushNotification1);
        storageClient.setNotification(pushNotification2);
        storageClient.setNotification(pushNotification3);
        Account accountFromStorage = storageClient.getAccount(account.getId());
        List<Mechanism> mechanismsFromStorage = storageClient.getMechanismsForAccount(accountFromStorage);
        assertNotNull(accountFromStorage);
        assertNotNull(mechanismsFromStorage);
        assertFalse("Mechanisms list should not be empty", mechanismsFromStorage.isEmpty());
        PushMechanism pushMechanismFromStorage = (PushMechanism) mechanismsFromStorage.get(0);
        List<PushNotification> notificationsFromStorage = storageClient.getAllNotificationsForMechanism(pushMechanismFromStorage);
        assertNotNull(notificationsFromStorage);
        assertEquals("Should have 3 notifications", 3, notificationsFromStorage.size());
    }

    @Test
    public void testUpdateExistingNotification() throws InvalidNotificationException, MechanismCreationException {
        String uniqueMechanismUID = MECHANISM_UID + "_updateNotif";
        String uniqueMessageId = MESSAGE_ID + "_updateNotif";
        Calendar timeAdded = Calendar.getInstance();
        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createPushMechanism(uniqueMechanismUID, ISSUER, ACCOUNT_NAME, SECRET,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        PushNotification pushNotification = createPushNotification(uniqueMechanismUID, uniqueMessageId, CHALLENGE,
                AMLB_COOKIE, timeAdded, TTL);
        storageClient.setAccount(account);
        storageClient.setMechanism(mechanism);
        storageClient.setNotification(pushNotification);
        Account accountFromStorage = storageClient.getAccount(account.getId());
        List<Mechanism> mechanismsFromStorage = storageClient.getMechanismsForAccount(accountFromStorage);
        assertNotNull(accountFromStorage);
        assertNotNull(mechanismsFromStorage);
        assertFalse("Mechanisms list should not be empty", mechanismsFromStorage.isEmpty());
        PushMechanism pushMechanismFromStorage = (PushMechanism) mechanismsFromStorage.get(0);
        List<PushNotification> notificationsFromStorage = storageClient.getAllNotificationsForMechanism(pushMechanismFromStorage);
        assertFalse("Notifications list should not be empty", notificationsFromStorage.isEmpty());
        PushNotification pushNotificationFromStorage = notificationsFromStorage.get(0);
        assertNotNull(pushNotificationFromStorage);
        assertEquals(uniqueMessageId, pushNotificationFromStorage.getMessageId());
        PushNotification updatedPushNotification = createPushNotification(uniqueMechanismUID, uniqueMessageId, CHALLENGE,
                AMLB_COOKIE, timeAdded, timeAdded, TTL, approved, pending);
        storageClient.setNotification(updatedPushNotification);
        List<PushNotification> updatedNotificationsFromStorage = storageClient.getAllNotificationsForMechanism(pushMechanismFromStorage);
        assertFalse("Notifications list should not be empty after update", updatedNotificationsFromStorage.isEmpty());
        PushNotification updatedPushNotificationFromStorage = updatedNotificationsFromStorage.get(0);
        assertNotNull(updatedPushNotificationFromStorage);
        assertEquals(uniqueMessageId, updatedPushNotificationFromStorage.getMessageId());
        assertEquals(approved, updatedPushNotificationFromStorage.isApproved());
        assertEquals(pending, updatedPushNotificationFromStorage.isPending());
    }

    @Test
    public void testRemoveExistingNotification() throws InvalidNotificationException, MechanismCreationException {
        String uniqueMechanismUID = MECHANISM_UID + "_removeNotif";
        String uniqueMessageId = MESSAGE_ID + "_removeNotif";
        Calendar timeAdded = Calendar.getInstance();
        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createPushMechanism(uniqueMechanismUID, ISSUER, ACCOUNT_NAME, SECRET,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        PushNotification pushNotification = createPushNotification(uniqueMechanismUID, uniqueMessageId, CHALLENGE,
                AMLB_COOKIE, timeAdded, TTL);
        storageClient.setAccount(account);
        storageClient.setMechanism(mechanism);
        storageClient.setNotification(pushNotification);
        assertFalse(storageClient.isEmpty());
        Account accountFromStorage = storageClient.getAccount(account.getId());
        List<Mechanism> mechanismsFromStorage = storageClient.getMechanismsForAccount(accountFromStorage);
        assertNotNull(accountFromStorage);
        assertNotNull(mechanismsFromStorage);
        assertFalse("Mechanisms list should not be empty", mechanismsFromStorage.isEmpty());
        PushMechanism pushMechanismFromStorage = (PushMechanism) mechanismsFromStorage.get(0);
        List<PushNotification> notificationsFromStorage = storageClient.getAllNotificationsForMechanism(pushMechanismFromStorage);
        assertFalse("Notifications list should not be empty", notificationsFromStorage.isEmpty());
        PushNotification pushNotificationFromStorage = notificationsFromStorage.get(0);
        assertNotNull(pushNotificationFromStorage);
        assertEquals(uniqueMessageId, pushNotificationFromStorage.getMessageId());
        assertTrue(storageClient.removeNotification(pushNotificationFromStorage));
        assertEquals("Notifications list should be empty after removal", 0, storageClient.getAllNotificationsForMechanism(pushMechanismFromStorage).size());
    }

    @Test
    public void testShouldGetNotificationById() throws InvalidNotificationException, MechanismCreationException {
        String uniqueMechanismUID = MECHANISM_UID + "_byId";
        String uniqueMessageId = MESSAGE_ID + "_byId";
        Calendar timeAdded = Calendar.getInstance();
        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createPushMechanism(uniqueMechanismUID, ISSUER, ACCOUNT_NAME, SECRET,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        PushNotification pushNotification = createPushNotification(uniqueMechanismUID, uniqueMessageId, CHALLENGE,
                AMLB_COOKIE, timeAdded, TTL);
        storageClient.setAccount(account);
        storageClient.setMechanism(mechanism);
        storageClient.setNotification(pushNotification);
        Account accountFromStorage = storageClient.getAccount(account.getId());
        List<Mechanism> mechanismsFromStorage = storageClient.getMechanismsForAccount(accountFromStorage);
        assertNotNull(accountFromStorage);
        assertNotNull(mechanismsFromStorage);
        assertFalse("Mechanisms list should not be empty", mechanismsFromStorage.isEmpty());
        PushMechanism pushMechanismFromStorage = (PushMechanism) mechanismsFromStorage.get(0);
        PushNotification pushNotificationFromStorage = storageClient.getNotification(pushNotification.getId());
        assertNotNull(pushNotificationFromStorage);
        assertEquals(uniqueMechanismUID, pushNotificationFromStorage.getMechanismUID());
        assertEquals(uniqueMessageId, pushNotificationFromStorage.getMessageId());
    }

    @Test
    public void testShouldGetNotificationByMessageId() throws InvalidNotificationException, MechanismCreationException {
        String uniqueMechanismUID = MECHANISM_UID + "_byMsgId";
        String uniqueMessageId = MESSAGE_ID + "_byMsgId";
        Calendar timeAdded = Calendar.getInstance();
        Account account = createAccountWithoutAdditionalData(ISSUER, ACCOUNT_NAME);
        Mechanism mechanism = createPushMechanism(uniqueMechanismUID, ISSUER, ACCOUNT_NAME, SECRET,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        PushNotification pushNotification = createPushNotification(uniqueMechanismUID, uniqueMessageId, CHALLENGE,
                AMLB_COOKIE, timeAdded, TTL);
        storageClient.setAccount(account);
        storageClient.setMechanism(mechanism);
        storageClient.setNotification(pushNotification);
        Account accountFromStorage = storageClient.getAccount(account.getId());
        List<Mechanism> mechanismsFromStorage = storageClient.getMechanismsForAccount(accountFromStorage);
        assertNotNull(accountFromStorage);
        assertNotNull(mechanismsFromStorage);
        assertFalse("Mechanisms list should not be empty", mechanismsFromStorage.isEmpty());
        PushMechanism pushMechanismFromStorage = (PushMechanism) mechanismsFromStorage.get(0);
        PushNotification pushNotificationFromStorage = storageClient.getNotificationByMessageId(pushNotification.getMessageId());
        assertNotNull(pushNotificationFromStorage);
        assertEquals(uniqueMechanismUID, pushNotificationFromStorage.getMechanismUID());
        assertEquals(uniqueMessageId, pushNotificationFromStorage.getMessageId());
    }

    @Test
    public void testNewSchemaFunctionality() throws Exception {
        SQLStorageClient storageClient = new SQLStorageClient(context);
        // Test storing and retrieving an account
        String uniqueIssuer = "issuer_newschema";
        String uniqueAccountName = "accountName_newschema";
        String uniqueMechanismUID = "mechanismUID_newschema";
        String uniqueMessageId = "messageId_newschema";
        Account account = Account.builder()
                .setIssuer(uniqueIssuer)
                .setAccountName(uniqueAccountName)
                .build();
        storageClient.setAccount(account);
        Account retrievedAccount = storageClient.getAccount(account.getId());
        assertNotNull(retrievedAccount);
        assertEquals(uniqueIssuer, retrievedAccount.getIssuer());
        assertEquals(uniqueAccountName, retrievedAccount.getAccountName());
        // Test storing and retrieving a mechanism (use PushMechanism for concrete type)
        PushMechanism mechanism = PushMechanism.builder()
                .setMechanismUID(uniqueMechanismUID)
                .setIssuer(uniqueIssuer)
                .setAccountName(uniqueAccountName)
                .setSecret("secret")
                .setRegistrationEndpoint("https://example.com/register")
                .setAuthenticationEndpoint("https://example.com/auth")
                .build();
        storageClient.setMechanism(mechanism);
        Mechanism retrievedMechanism = storageClient.getMechanismByUUID(mechanism.getMechanismUID());
        assertNotNull(retrievedMechanism);
        assertEquals(uniqueMechanismUID, retrievedMechanism.getMechanismUID());
        // Test storing and retrieving a notification (minimal required fields)
        Calendar now = Calendar.getInstance();
        PushNotification notification = PushNotification.builder()
                .setMechanismUID(uniqueMechanismUID)
                .setMessageId(uniqueMessageId)
                .setChallenge("challenge")
                .setTimeAdded(now)
                .build();
        storageClient.setNotification(notification);
        PushNotification retrievedNotification = storageClient.getNotification(notification.getId());
        assertNotNull(retrievedNotification);
        assertEquals(uniqueMessageId, retrievedNotification.getMessageId());
    }
}
