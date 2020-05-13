/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
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
    private Push push;
    private MockWebServer server;

    @Before
    public void setUp() throws IOException {
        context = ApplicationProvider.getApplicationContext();

        push = mockPushMechanism(MECHANISM_UID);

        storageClient = mock(DefaultStorageClient.class);
        given(storageClient.setAccount(any(Account.class))).willReturn(true);
        given(storageClient.setMechanism(any(Push.class))).willReturn(true);
        given(storageClient.setMechanism(any(Oath.class))).willReturn(true);
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
        Oath oath = (Oath) oathListenerFuture.get();
        assertEquals(oath.getOathType(), Oath.TokenType.TOTP);
        assertEquals(oath.getAccountName(), "user1");
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
                "s=dA18Iph3slIUDVuRc5+3y7nv9NLGnPksH66d3jIF6uE=&" +
                "c=Yf66ojm3Pm80PVvNpljTB6X9CUhgSJ0WZUzB4su3vCY=&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "issuer=Rm9yZ2Vyb2Nr";

        authenticatorManager.createMechanismFromUri(uri, pushListenerFuture);
        Push push = (Push) pushListenerFuture.get();
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
    public void testShouldRegisterDeviceTokenForRemoteNotifications() {

        authenticatorManager = new AuthenticatorManager(context, storageClient, null);
        try {
            authenticatorManager.registerForRemoteNotifications("s-o-m-e-t-o-k-e-n");
        } catch (AuthenticatorException e) {
            assertNull(e);
        }
    }

    @Test
    public void testShouldFailToRegisterDeviceTokenForRemoteNotificationsAlreadyRegisteredSameToken() {
        try {
            authenticatorManager.registerForRemoteNotifications("s-o-m-e-t-o-k-e-n");
        } catch (AuthenticatorException e) {
            assertNotNull(e);
            assertTrue(e.getLocalizedMessage().contains("The SDK was already initialized with this device token"));
        }
    }

    @Test
    public void testShouldFailToRegisterDeviceTokenForRemoteNotificationsAlreadyRegisteredDifferentToken() {
        try {
            authenticatorManager.registerForRemoteNotifications("a-n-o-t-h-e-r-t-o-k-e-n");
        } catch (AuthenticatorException e) {
            assertNotNull(e);
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
