/*
 * Copyright (c) 2020 - 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.util.Base64;

import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.messaging.RemoteMessage;

import org.forgerock.android.auth.exception.AuthenticatorException;
import org.forgerock.android.auth.exception.InvalidNotificationException;
import org.forgerock.android.auth.exception.MechanismCreationException;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@RunWith(RobolectricTestRunner.class)
public class AuthenticatorManagerTest extends FRABaseTest {

    private Context context;
    private DefaultStorageClient storageClient;
    private PushFactory pushFactory;
    private FRAListenerFuture pushListenerFuture;
    private FRAListenerFuture oathListenerFuture;
    private AuthenticatorManager authenticatorManager;
    private PushMechanism push;
    private MockWebServer server;

    @Before
    public void setUp() throws IOException {
        context = ApplicationProvider.getApplicationContext();

        push = mockPushMechanism(MECHANISM_UID);

        storageClient = mock(DefaultStorageClient.class);
        given(storageClient.setAccount(any(Account.class))).willReturn(true);
        given(storageClient.setMechanism(any(PushMechanism.class))).willReturn(true);
        given(storageClient.setMechanism(any(OathMechanism.class))).willReturn(true);
        given(storageClient.setNotification(any(PushNotification.class))).willReturn(true);
        given(storageClient.getMechanismByUUID(MECHANISM_UID)).willReturn(push);

        authenticatorManager = new AuthenticatorManager(context, storageClient, "s-o-m-e-t-o-k-e-n");

        oathListenerFuture = new FRAListenerFuture<Mechanism>();
        pushListenerFuture = new FRAListenerFuture<Mechanism>();

        pushFactory = spy(new PushFactory(context, storageClient, "s-o-m-e-t-o-k-e-n"));
        doReturn(true).when(pushFactory).checkGooglePlayServices();

        server = new MockWebServer();
        server.start();
    }

    @After
    public void shutdown() throws IOException {
        server.shutdown();
    }

    @Test
    public void testShouldCreateOathMechanismSuccessfully() throws Exception {

        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ=====";
        authenticatorManager.createMechanismFromUri(uri, oathListenerFuture);
        OathMechanism oath = (OathMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.TOTP);
        assertEquals(oath.getAccountName(), "user1");
    }

    @Test
    public void testFailToCreateMechanismInvalidQRCode() {
        try {
            authenticatorManager = new AuthenticatorManager(context, storageClient, null);

            String uri = "http://unkown/Forgerock:user1?secret=ONSWG4TFOQ=====";

            authenticatorManager.createMechanismFromUri(uri, oathListenerFuture);
            oathListenerFuture.get();
            fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("Invalid QR Code given for Mechanism initialization"));
        }
    }

    @Test
    public void testCreatePushMechanismFailureNoFcmTokenProvided() {
        try {
            authenticatorManager = new AuthenticatorManager(context, storageClient, null);
            server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK));

            String uri = "pushauth://push/forgerock:demo?" +
                    "a=" + getBase64PushActionUrl(server,"authenticate") + "&" +
                    "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                    "b=ff00ff&" +
                    "r=" + getBase64PushActionUrl(server,"register") + "&" +
                    "s=dA18Iph3slIUDVuRc5+3y7nv9NLGnPksH66d3jIF6uE=&" +
                    "c=Yf66ojm3Pm80PVvNpljTB6X9CUhgSJ0WZUzB4su3vCY=&" +
                    "l=YW1sYmNvb2tpZT0wMQ==&" +
                    "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                    "issuer=Rm9yZ2Vyb2Nr";

            authenticatorManager.createMechanismFromUri(uri, pushListenerFuture);
            pushListenerFuture.get();
            fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("FCM token not provided during SDK initialization"));
        }
    }

    @Test
    public void testShouldCreatePushMechanismSuccessfully() throws Exception {
        authenticatorManager.setPushFactory(pushFactory);

        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK));

        String uri = "pushauth://push/forgerock:demo?" +
                "a=" + getBase64PushActionUrl(server,"authenticate") + "&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=" + getBase64PushActionUrl(server,"register") + "&" +
                "s=ryJkqNRjXYd_nX523672AX_oKdVXrKExq-VjVeRKKTc&" +
                "c=Daf8vrc8onKu-dcptwCRS9UHmdui5u16vAdG2HMU4w0&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "issuer=Rm9yZ2Vyb2Nr";

        authenticatorManager.createMechanismFromUri(uri, pushListenerFuture);
        PushMechanism push = (PushMechanism) pushListenerFuture.get();
        assertEquals(push.getType(), Mechanism.PUSH);
        assertEquals(push.getAccountName(), "demo");
        assertEquals(push.getIssuer(), "Forgerock");
    }

    @Test
    public void testShouldHandleMessageAsParameters() throws Exception {
        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, generateBaseMessage());
        String messageId = remoteMessage.getData().get("messageId");
        String message = remoteMessage.getData().get("message");

        PushNotification pushNotification = authenticatorManager.handleMessage(messageId, message);

        assertNotNull(pushNotification);
        assertEquals(MESSAGE_ID, pushNotification.getMessageId());
        assertEquals(CHALLENGE, pushNotification.getChallenge());
        assertEquals(new String(Base64.decode(AMLB_COOKIE, Base64.NO_WRAP)), pushNotification.getAmlbCookie());
        assertEquals(MECHANISM_UID, pushNotification.getMechanismUID());
        assertEquals(TTL, pushNotification.getTtl());
    }

    @Test
    public void testShouldHandleRemoteMessage() throws Exception {
        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, generateBaseMessage());
        PushNotification pushNotification = authenticatorManager.handleMessage(remoteMessage);

        assertNotNull(pushNotification);
        assertEquals(MESSAGE_ID, pushNotification.getMessageId());
        assertEquals(CHALLENGE, pushNotification.getChallenge());
        assertEquals(new String(Base64.decode(AMLB_COOKIE, Base64.NO_WRAP)), pushNotification.getAmlbCookie());
        assertEquals(MECHANISM_UID, pushNotification.getMechanismUID());
        assertEquals(TTL, pushNotification.getTtl());
    }

    @Test
    public void testShouldRegisterDeviceTokenForRemoteNotifications() throws JSONException {
        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, generateBaseMessage());
        PushNotification pushNotification = null;

        authenticatorManager = new AuthenticatorManager(context, storageClient, null);
        try {
            pushNotification = authenticatorManager.handleMessage(remoteMessage);
        } catch (Exception e) {
            assertTrue(e instanceof InvalidNotificationException);
            assertTrue(e.getLocalizedMessage().contains("Cannot process Push notification"));
        }
        assertNull(pushNotification);

        try {
            authenticatorManager.registerForRemoteNotifications("s-o-m-e-t-o-k-e-n");
            pushNotification = authenticatorManager.handleMessage(remoteMessage);
        } catch (AuthenticatorException | InvalidNotificationException e) {
            e.printStackTrace();
        }
        assertNotNull(pushNotification);
    }

    @Test
    public void testShouldFailToRegisterDeviceTokenForRemoteNotificationsAlreadyRegisteredSameToken() {
        authenticatorManager = new AuthenticatorManager(context, storageClient, "s-o-m-e-t-o-k-e-n");
        try {
            authenticatorManager.registerForRemoteNotifications("s-o-m-e-t-o-k-e-n");
            fail("Should throw AuthenticatorException");
        } catch (Exception e) {
            assertTrue(e instanceof AuthenticatorException);
            assertTrue(e.getLocalizedMessage().contains("The SDK was already initialized with the FCM device token"));
        }
    }

    @Test
    public void testShouldFailToRegisterDeviceTokenForRemoteNotificationsAlreadyRegisteredDifferentToken() {
        authenticatorManager = new AuthenticatorManager(context, storageClient, "s-o-m-e-t-o-k-e-n");
        try {
            authenticatorManager.registerForRemoteNotifications("a-n-o-t-h-e-r-t-o-k-e-n");
            fail("Should throw AuthenticatorException");
        } catch (Exception e) {
            assertTrue(e instanceof AuthenticatorException);
            assertTrue(e.getLocalizedMessage().contains("The SDK was initialized with a different deviceToken"));
        }
    }

    @Test
    public void testShouldGetAllStoredAccounts() {
        Account account1 = createAccount(OTHER_ACCOUNT_NAME, OTHER_ISSUER);
        Mechanism oath = createOathMechanism(OTHER_ACCOUNT_NAME, OTHER_ISSUER, OTHER_MECHANISM_UID);

        Account account2 = createAccount(ACCOUNT_NAME, ISSUER);
        Mechanism push = createPushMechanism(ACCOUNT_NAME, ISSUER, MECHANISM_UID);

        List<Account> accountList= new ArrayList<>();
        accountList.add(account1);
        accountList.add(account2);

        List<Mechanism> mechanismList1 = new ArrayList<>();
        mechanismList1.add(oath);

        List<Mechanism> mechanismList2 = new ArrayList<>();
        mechanismList2.add(push);

        List<PushNotification> notificationList = new ArrayList<>();
        notificationList.add(createPushNotification(MESSAGE_ID, push));
        notificationList.add(createPushNotification(OTHER_MESSAGE_ID, push));

        given(storageClient.getAllAccounts()).willReturn(accountList);
        given(storageClient.getMechanismsForAccount(account1)).willReturn(mechanismList1);
        given(storageClient.getMechanismsForAccount(account2)).willReturn(mechanismList2);
        given(storageClient.getAllNotificationsForMechanism(push)).willReturn(notificationList);

        List<Account> accountsFromStorageList = authenticatorManager.getAllAccounts();

        assertNotNull(accountsFromStorageList);
        assertEquals(2, accountsFromStorageList.size());
        assertEquals(accountsFromStorageList.get(0).getMechanisms().size(), 1);
        assertEquals(accountsFromStorageList.get(1).getMechanisms().size(), 1);
    }

    @Test
    public void testShouldGetStoredAccountByID() {
        Account account = createAccount(ACCOUNT_NAME, ISSUER);
        Mechanism oath = createOathMechanism(ACCOUNT_NAME, ISSUER, OTHER_MECHANISM_UID);
        Mechanism push = createPushMechanism(ACCOUNT_NAME, ISSUER, MECHANISM_UID);

        List<Mechanism> mechanismList= new ArrayList<>();
        mechanismList.add(push);
        mechanismList.add(oath);

        List<PushNotification> notificationList = new ArrayList<>();
        notificationList.add(createPushNotification(MESSAGE_ID, push));
        notificationList.add(createPushNotification(OTHER_MESSAGE_ID, push));

        given(storageClient.getAccount(any(String.class))).willReturn(account);
        given(storageClient.getMechanismsForAccount(any(Account.class))).willReturn(mechanismList);
        given(storageClient.getAllNotificationsForMechanism(any(Mechanism.class))).willReturn(notificationList);

        Account accountFromStorage = authenticatorManager.getAccount(account.getId());

        assertNotNull(accountFromStorage);
        assertEquals(account, accountFromStorage);
        assertEquals(2, accountFromStorage.getMechanisms().size());
    }

    @Test
    public void testShouldGetStoredAccountByMechanism() throws Exception {
        Account account = createAccount(ACCOUNT_NAME, ISSUER);
        Mechanism oath = createOathMechanism(ACCOUNT_NAME, ISSUER, OTHER_MECHANISM_UID);

        List<Mechanism> mechanismList= new ArrayList<>();
        mechanismList.add(oath);

        given(storageClient.getAccount(any(String.class))).willReturn(account);
        given(storageClient.getMechanismByUUID(any(String.class))).willReturn(push);
        given(storageClient.getMechanismsForAccount(any(Account.class))).willReturn(mechanismList);

        Account accountFromStorage = authenticatorManager.getAccount(oath);

        assertNotNull(accountFromStorage);
        assertEquals(account, accountFromStorage);
    }

    @Test
    public void testShouldUpdateStoredAccount() throws Exception {
        Account account = createAccount(ACCOUNT_NAME, ISSUER);
        Mechanism oath = createOathMechanism(ACCOUNT_NAME, ISSUER, OTHER_MECHANISM_UID);

        List<Mechanism> mechanismList= new ArrayList<>();
        mechanismList.add(oath);

        given(storageClient.getAccount(any(String.class))).willReturn(account);
        given(storageClient.setAccount(any(Account.class))).willReturn(true);

        account.setDisplayAccountName(OTHER_ACCOUNT_NAME);
        account.setDisplayIssuer(OTHER_ISSUER);

        boolean result = authenticatorManager.updateAccount(account);

        assertTrue(result);
        assertEquals(account.getAccountName(), ACCOUNT_NAME);
        assertEquals(account.getDisplayAccountName(), OTHER_ACCOUNT_NAME);
        assertEquals(account.getIssuer(), ISSUER);
        assertEquals(account.getDisplayIssuer(), OTHER_ISSUER);
    }

    @Test
    public void testShouldGetStoredMechanismByPushNotification() throws Exception {
        Account account = createAccount(ACCOUNT_NAME, ISSUER);
        Mechanism push = createPushMechanism(ACCOUNT_NAME, ISSUER, MECHANISM_UID);

        List<Mechanism> mechanismList= new ArrayList<>();
        mechanismList.add(push);

        PushNotification pushNotification = createPushNotification(MESSAGE_ID, push);
        List<PushNotification> notificationList = new ArrayList<>();
        notificationList.add(pushNotification);
        notificationList.add(createPushNotification(OTHER_MESSAGE_ID, push));

        given(storageClient.getAccount(any(String.class))).willReturn(account);
        given(storageClient.getMechanismsForAccount(any(Account.class))).willReturn(mechanismList);
        given(storageClient.getAllNotificationsForMechanism(any(Mechanism.class))).willReturn(notificationList);
        given(storageClient.getMechanismByUUID(any(String.class))).willReturn(push);

        Mechanism mechanismFromStorage = authenticatorManager.getMechanism(pushNotification);

        assertNotNull(mechanismFromStorage);
        assertEquals(push, mechanismFromStorage);
    }

    @Test
    public void testShouldGetAllNotificationsByMechanism() throws Exception {
        Account account = createAccount(ACCOUNT_NAME, ISSUER);
        Mechanism push = createPushMechanism(ACCOUNT_NAME, ISSUER, MECHANISM_UID);

        List<Mechanism> mechanismList= new ArrayList<>();
        mechanismList.add(push);

        List<PushNotification> notificationList = new ArrayList<>();
        notificationList.add(createPushNotification(MESSAGE_ID, push));
        notificationList.add(createPushNotification(OTHER_MESSAGE_ID, push));

        given(storageClient.getAccount(any(String.class))).willReturn(account);
        given(storageClient.getMechanismsForAccount(any(Account.class))).willReturn(mechanismList);
        given(storageClient.getAllNotificationsForMechanism(any(Mechanism.class))).willReturn(notificationList);

        List<PushNotification> notificationListFromStorage = authenticatorManager.getAllNotifications(push);

        assertNotNull(notificationListFromStorage);
        assertEquals(notificationList.size(), notificationListFromStorage.size());
    }

    @Test
    public void testShouldGetStoredNotificationByID() {
        Account account = createAccount(ACCOUNT_NAME, ISSUER);
        Mechanism push = createPushMechanism(ACCOUNT_NAME, ISSUER, MECHANISM_UID);
        PushNotification notification = createPushNotification(MESSAGE_ID, push);

        List<Mechanism> mechanismList= new ArrayList<>();
        mechanismList.add(push);

        List<PushNotification> notificationList = new ArrayList<>();
        notificationList.add(createPushNotification(MESSAGE_ID, push));
        notificationList.add(createPushNotification(OTHER_MESSAGE_ID, push));

        given(storageClient.getAccount(any(String.class))).willReturn(account);
        given(storageClient.getMechanismsForAccount(any(Account.class))).willReturn(mechanismList);
        given(storageClient.getAllNotificationsForMechanism(any(Mechanism.class))).willReturn(notificationList);
        given(storageClient.getNotification(anyString())).willReturn(notification);

        PushNotification notificationFromStorage = authenticatorManager.getNotification(notification.getId());

        assertNotNull(notificationFromStorage);
        assertEquals(notification, notificationFromStorage);
    }

    @Test
    public void testShouldRemoveExistingAccount() {
        Account account = createAccount(ACCOUNT_NAME, ISSUER);
        Mechanism oath = createOathMechanism(ACCOUNT_NAME, ISSUER, OTHER_MECHANISM_UID);
        Mechanism push = createPushMechanism(ACCOUNT_NAME, ISSUER, MECHANISM_UID);

        List<Mechanism> mechanismList= new ArrayList<>();
        mechanismList.add(push);
        mechanismList.add(oath);

        List<PushNotification> notificationList = new ArrayList<>();
        notificationList.add(createPushNotification(MESSAGE_ID, push));
        notificationList.add(createPushNotification(OTHER_MESSAGE_ID, push));

        given(storageClient.removeAccount(any(Account.class))).willReturn(true);
        given(storageClient.removeMechanism(any(Mechanism.class))).willReturn(true);
        given(storageClient.removeNotification(any(PushNotification.class))).willReturn(true);
        given(storageClient.getAccount(any(String.class))).willReturn(account);
        given(storageClient.getMechanismsForAccount(any(Account.class))).willReturn(mechanismList);
        given(storageClient.getAllNotificationsForMechanism(any(Mechanism.class))).willReturn(notificationList);

        boolean result = authenticatorManager.removeAccount(account);

        assertTrue(result);
    }

    @Test
    public void testShouldRemoveOathExistingMechanism() {
        Account account = createAccount(ACCOUNT_NAME, ISSUER);
        Mechanism oath = createOathMechanism(ACCOUNT_NAME, ISSUER, OTHER_MECHANISM_UID);

        List<Mechanism> mechanismList= new ArrayList<>();
        mechanismList.add(oath);

        given(storageClient.removeAccount(any(Account.class))).willReturn(true);
        given(storageClient.removeMechanism(any(Mechanism.class))).willReturn(true);
        given(storageClient.removeNotification(any(PushNotification.class))).willReturn(true);
        given(storageClient.getAccount(any(String.class))).willReturn(account);
        given(storageClient.getMechanismsForAccount(any(Account.class))).willReturn(mechanismList);

        boolean result = authenticatorManager.removeMechanism(oath);

        assertTrue(result);
    }

    @Test
    public void testShouldRemoveExistingPushMechanism() {
        Account account = createAccount(ACCOUNT_NAME, ISSUER);
        Mechanism push = createPushMechanism(ACCOUNT_NAME, ISSUER, MECHANISM_UID);

        List<Mechanism> mechanismList= new ArrayList<>();
        mechanismList.add(push);

        List<PushNotification> notificationList = new ArrayList<>();
        notificationList.add(createPushNotification(MESSAGE_ID, push));
        notificationList.add(createPushNotification(OTHER_MESSAGE_ID, push));

        given(storageClient.removeAccount(any(Account.class))).willReturn(true);
        given(storageClient.removeMechanism(any(Mechanism.class))).willReturn(true);
        given(storageClient.removeNotification(any(PushNotification.class))).willReturn(true);
        given(storageClient.getAccount(any(String.class))).willReturn(account);
        given(storageClient.getMechanismsForAccount(any(Account.class))).willReturn(mechanismList);
        given(storageClient.getAllNotificationsForMechanism(any(Mechanism.class))).willReturn(notificationList);

        boolean result = authenticatorManager.removeMechanism(push);

        assertTrue(result);
    }

    @Test
    public void testShouldRemoveExistingPushNotification() {
        Account account = createAccount(ACCOUNT_NAME, ISSUER);
        Mechanism push = createPushMechanism(ACCOUNT_NAME, ISSUER, MECHANISM_UID);

        List<Mechanism> mechanismList= new ArrayList<>();
        mechanismList.add(push);

        List<PushNotification> notificationList = new ArrayList<>();
        notificationList.add(createPushNotification(MESSAGE_ID, push));
        notificationList.add(createPushNotification(OTHER_MESSAGE_ID, push));

        given(storageClient.removeAccount(any(Account.class))).willReturn(true);
        given(storageClient.removeMechanism(any(Mechanism.class))).willReturn(true);
        given(storageClient.removeNotification(any(PushNotification.class))).willReturn(true);
        given(storageClient.getAccount(any(String.class))).willReturn(account);
        given(storageClient.getMechanismsForAccount(any(Account.class))).willReturn(mechanismList);
        given(storageClient.getAllNotificationsForMechanism(any(Mechanism.class))).willReturn(notificationList);

        boolean result = authenticatorManager.removeNotification(notificationList.get(0));

        assertTrue(result);
    }

}
