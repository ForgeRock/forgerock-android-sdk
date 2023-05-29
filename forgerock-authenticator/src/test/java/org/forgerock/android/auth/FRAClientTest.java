/*
 * Copyright (c) 2020 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

import android.content.Context;
import android.util.Base64;

import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.messaging.RemoteMessage;

import org.forgerock.android.auth.exception.AccountLockException;
import org.forgerock.android.auth.exception.AuthenticatorException;
import org.forgerock.android.auth.exception.InvalidPolicyException;
import org.forgerock.android.auth.exception.MechanismCreationException;
import org.forgerock.android.auth.exception.MechanismParsingException;
import org.forgerock.android.auth.policy.DeviceTamperingPolicy;
import org.forgerock.android.auth.policy.FRAPolicy;
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

@RunWith(RobolectricTestRunner.class)
public class FRAClientTest extends FRABaseTest {

    private Context context;
    private DefaultStorageClient storageClient;
    private PushFactory pushFactory;
    private NotificationFactory notificationFactory;
    private FRAPolicyEvaluator policyEvaluator;
    private PushMechanism push;
    private MockWebServer server;

    @Before
    public void setUp() throws IOException, InvalidPolicyException {
        context = ApplicationProvider.getApplicationContext();

        push = mockPushMechanism(MECHANISM_UID);

        storageClient = mock(DefaultStorageClient.class);
        given(storageClient.setAccount(any(Account.class))).willReturn(true);
        given(storageClient.setMechanism(any(PushMechanism.class))).willReturn(true);
        given(storageClient.setMechanism(any(OathMechanism.class))).willReturn(true);
        given(storageClient.setNotification(any(PushNotification.class))).willReturn(true);
        given(storageClient.getMechanismByUUID(MECHANISM_UID)).willReturn(push);

        pushFactory = spy(new PushFactory(context, storageClient, "s-o-m-e-t-o-k-e-n"));
        doReturn(true).when(pushFactory).checkGooglePlayServices();

        FRAPolicyEvaluator.Result result = new FRAPolicyEvaluator.Result(true, null);
        policyEvaluator = spy(new FRAPolicyEvaluator.FRAPolicyEvaluatorBuilder().build());
        doReturn(result).when(policyEvaluator).evaluate(any(), anyString());
        doReturn(result).when(policyEvaluator).evaluate(any(), any(Account.class));

        notificationFactory = new NotificationFactory(storageClient);

        server = new MockWebServer();
        server.start();
    }

    @After
    public void shutdown() throws IOException {
        server.shutdown();
    }

    @Test
    public void testShouldStartSDKWithContext() throws AuthenticatorException {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .start();

        assertNotNull(fraClient);
        assertNotNull(fraClient.getAuthenticatorManagerInstance());
    }

    @Test
    public void testShouldStartSDKWithEnforceAccountLock() throws AuthenticatorException {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withPolicyEvaluator(policyEvaluator)
                .start();

        assertNotNull(fraClient);
        assertNotNull(fraClient.getAuthenticatorManagerInstance());
    }

    @Test
    public void testShouldStartSDKWithFcmToken() throws AuthenticatorException {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withDeviceToken("s-o-m-e-t-o-k-e-n")
                .start();

        assertNotNull(fraClient);
        assertNotNull(fraClient.getAuthenticatorManagerInstance());
    }

    @Test
    public void testShouldStartSDKWithCustomStorageClient() throws AuthenticatorException {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withDeviceToken("s-o-m-e-t-o-k-e-n")
                .withStorage(new CustomStorageClient(context))
                .start();

        assertNotNull(fraClient);
        assertNotNull(fraClient.getAuthenticatorManagerInstance());
    }

    @Test
    public void testStartSDKFailureMissingParameters() {
        FRAClient fraClient = null;
        try {
            fraClient = FRAClient.builder().start();
            fail("Should trow AuthenticatorException");
        } catch (Exception e) {
            assertTrue(e instanceof AuthenticatorException);
            assertTrue(e.getLocalizedMessage().contains("Must provide a valid context for the SDK initialization"));
            assertNull(fraClient);
        }
    }

    @Test
    public void testCreateMechanismFailureInvalidType() throws Exception {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withStorage(storageClient)
                .start();

        assertNotNull(fraClient);

        String uri = "otpauth://unknown/example?secret=ABC\"";

        FRAListenerFuture mechanismListenerFuture = new FRAListenerFuture<Mechanism>();
        try {
            fraClient.createMechanismFromUri(uri, mechanismListenerFuture);
            mechanismListenerFuture.get();
            fail("Should trow MechanismParsingException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismParsingException);
            assertTrue(e.getLocalizedMessage().contains("Failed to parse URI"));
        }
    }

    @Test
    public void testShouldCreateOathMechanismSuccessfully() throws Exception {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withStorage(storageClient)
                .start();

        assertNotNull(fraClient);
        assertNotNull(fraClient.getAuthenticatorManagerInstance());

        String uri = "otpauth://totp/Forgerock:user1?secret=ONSWG4TFOQ=====";
        FRAListenerFuture oathListenerFuture = new FRAListenerFuture<Mechanism>();
        fraClient.createMechanismFromUri(uri, oathListenerFuture);
        OathMechanism oath = (OathMechanism) oathListenerFuture.get();
        assertEquals(oath.getOathType(), OathMechanism.TokenType.TOTP);
        assertEquals(oath.getAccountName(), "user1");
    }

    @Test
    public void testCreateOathMechanismFailure() throws Exception {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withStorage(storageClient)
                .start();

        assertNotNull(fraClient);

        String uri = "otpauth://totp/?secret=ABC\"";

        FRAListenerFuture oathListenerFuture = new FRAListenerFuture<Mechanism>();
        try {
            fraClient.createMechanismFromUri(uri, oathListenerFuture);
            oathListenerFuture.get();
            fail("Should trow MechanismParsingException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismParsingException);
            assertTrue(e.getLocalizedMessage().contains("Failed to parse URI"));
        }
    }

    @Test
    public void testShouldCreatePushMechanismSuccessfully() throws Exception {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withDeviceToken("s-o-m-e-t-o-k-e-n")
                .withStorage(storageClient)
                .start();

        assertNotNull(fraClient);
        assertNotNull(fraClient.getAuthenticatorManagerInstance());

        AuthenticatorManager managerInstance = fraClient.getAuthenticatorManagerInstance();
        managerInstance.setPushFactory(pushFactory);

        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK));

        String uri = "pushauth://push/forgerock:test?" +
                "a=" + getBase64PushActionUrl(server,"authenticate") + "&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=519387&" +
                "r=" + getBase64PushActionUrl(server,"register") + "&" +
                "s=ryJkqNRjXYd_nX523672AX_oKdVXrKExq-VjVeRKKTc&" +
                "c=Daf8vrc8onKu-dcptwCRS9UHmdui5u16vAdG2HMU4w0&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "issuer=Rm9yZ2Vyb2Nr";

        FRAListenerFuture pushListenerFuture = new FRAListenerFuture<Mechanism>();
        fraClient.createMechanismFromUri(uri, pushListenerFuture);
        PushMechanism push = (PushMechanism) pushListenerFuture.get();
        assertEquals(push.getType(), Mechanism.PUSH);
        assertEquals(push.getAccountName(), "test");
        assertEquals(push.getIssuer(), "Forgerock");
    }

    @Test
    public void testCreatePushMechanismFailureNoFcmTokenProvided() throws Exception {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withStorage(storageClient)
                .start();

        assertNotNull(fraClient);
        assertNotNull(fraClient.getAuthenticatorManagerInstance());

        FRAListenerFuture pushListenerFuture = new FRAListenerFuture<Mechanism>();
        try {
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

            fraClient.createMechanismFromUri(uri, pushListenerFuture);
            pushListenerFuture.get();
            fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("FCM token not provided during SDK initialization"));
        }
    }

    @Test
    public void testShouldCreateCombinedMechanismsSuccessfully() throws Exception {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withDeviceToken("s-o-m-e-t-o-k-e-n")
                .withStorage(storageClient)
                .withPolicyEvaluator(policyEvaluator)
                .start();

        assertNotNull(fraClient);
        assertNotNull(fraClient.getAuthenticatorManagerInstance());

        AuthenticatorManager managerInstance = fraClient.getAuthenticatorManagerInstance();
        managerInstance.setPushFactory(pushFactory);

        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK));

        String combinedUri = "mfauth://totp/ForgeRock:demo?" +
                "a=" + getBase64PushActionUrl(server, "authenticate") + "&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=" + getBase64PushActionUrl(server, "register") + "&" +
                "s=ryJkqNRjXYd_nX523672AX_oKdVXrKExq-VjVeRKKTc&" +
                "c=Daf8vrc8onKu-dcptwCRS9UHmdui5u16vAdG2HMU4w0&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "policies=eyJiaW9tZXRyaWNBdmFpbGFibGUiOiB7IH0sImRldmljZVRhbXBlcmluZyI6IHsic2NvcmUiOiAwLjh9fQ&" +
                "digits=6&" +
                "secret=R2PYFZRISXA5L25NVSSYK2RQ6E======&" +
                "period=30&";

        FRAListenerFuture pushListenerFuture = new FRAListenerFuture<Mechanism>();
        fraClient.createMechanismFromUri(combinedUri, pushListenerFuture);
        PushMechanism push = (PushMechanism) pushListenerFuture.get();
        assertEquals(push.getType(), Mechanism.PUSH);
        assertEquals(push.getAccountName(), "demo");
        assertEquals(push.getIssuer(), "ForgeRock");
    }

    @Test
    public void testShouldHandleMessageAsParameters() throws Exception {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withDeviceToken("s-o-m-e-t-o-k-e-n")
                .withStorage(storageClient)
                .start();

        assertNotNull(fraClient);
        assertNotNull(fraClient.getAuthenticatorManagerInstance());

        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, generateBaseMessage());
        String messageId = remoteMessage.getData().get("messageId");
        String message = remoteMessage.getData().get("message");

        AuthenticatorManager managerInstance = fraClient.getAuthenticatorManagerInstance();
        managerInstance.setNotificationFactory(notificationFactory);

        PushNotification pushNotification = fraClient.handleMessage(messageId, message);

        assertNotNull(pushNotification);
        assertEquals(MESSAGE_ID, pushNotification.getMessageId());
        assertEquals(CHALLENGE, pushNotification.getChallenge());
        assertEquals(new String(Base64.decode(AMLB_COOKIE, Base64.NO_WRAP)), pushNotification.getAmlbCookie());
        assertEquals(MECHANISM_UID, pushNotification.getMechanismUID());
        assertEquals(TTL, pushNotification.getTtl());
    }

    @Test
    public void testShouldHandleRemoteMessage() throws Exception {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withDeviceToken("s-o-m-e-t-o-k-e-n")
                .withStorage(storageClient)
                .start();

        assertNotNull(fraClient);
        assertNotNull(fraClient.getAuthenticatorManagerInstance());

        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, generateBaseMessage());

        AuthenticatorManager managerInstance = fraClient.getAuthenticatorManagerInstance();
        managerInstance.setNotificationFactory(notificationFactory);

        PushNotification pushNotification = fraClient.handleMessage(remoteMessage);

        assertNotNull(pushNotification);
        assertEquals(MESSAGE_ID, pushNotification.getMessageId());
        assertEquals(CHALLENGE, pushNotification.getChallenge());
        assertEquals(new String(Base64.decode(AMLB_COOKIE, Base64.NO_WRAP)), pushNotification.getAmlbCookie());
        assertEquals(MECHANISM_UID, pushNotification.getMechanismUID());
        assertEquals(TTL, pushNotification.getTtl());
    }

    @Test
    public void testShouldRegisterDeviceTokenForRemoteNotifications() {
        try {
            FRAClient fraClient = FRAClient.builder()
                    .withContext(context)
                    .withStorage(storageClient)
                    .start();

            fraClient.registerForRemoteNotifications("s-o-m-e-t-o-k-e-n");
        } catch (AuthenticatorException e) {
            assertNull(e);
        }
    }

    @Test
    public void testShouldFailToRegisterDeviceTokenForRemoteNotificationsAlreadyRegisteredSameToken() {
        try {
            FRAClient fraClient = FRAClient.builder()
                    .withContext(context)
                    .withDeviceToken("s-o-m-e-t-o-k-e-n")
                    .withStorage(storageClient)
                    .start();

            fraClient.registerForRemoteNotifications("s-o-m-e-t-o-k-e-n");
        } catch (Exception e) {
            assertTrue(e instanceof AuthenticatorException);
            assertTrue(e.getLocalizedMessage().contains("The SDK was already initialized with the FCM device token"));
        }
    }

    @Test
    public void testShouldFailToRegisterDeviceTokenForRemoteNotificationsAlreadyRegisteredDifferentToken() {
        try {
            FRAClient fraClient = FRAClient.builder()
                    .withContext(context)
                    .withDeviceToken("s-o-m-e-t-o-k-e-n")
                    .withStorage(storageClient)
                    .start();

            fraClient.registerForRemoteNotifications("a-n-o-t-h-e-r-t-o-k-e-n");
        } catch (Exception e) {
            assertTrue(e instanceof AuthenticatorException);
            assertTrue(e.getLocalizedMessage().contains("The SDK was initialized with a different deviceToken"));
        }
    }

    @Test
    public void testShouldGetAllStoredAccounts() throws Exception {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withDeviceToken("s-o-m-e-t-o-k-e-n")
                .withStorage(storageClient)
                .start();

        assertNotNull(fraClient);
        assertNotNull(fraClient.getAuthenticatorManagerInstance());

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

        List<Account> accountsFromStorageList = fraClient.getAllAccounts();

        assertNotNull(accountsFromStorageList);
        assertEquals(2, accountsFromStorageList.size());
    }

    @Test
    public void testShouldGetStoredAccountByID() throws Exception {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withDeviceToken("s-o-m-e-t-o-k-e-n")
                .withStorage(storageClient)
                .start();

        assertNotNull(fraClient);
        assertNotNull(fraClient.getAuthenticatorManagerInstance());

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

        Account accountFromStorage = fraClient.getAccount(account.getId());

        assertNotNull(accountFromStorage);
        assertEquals(account, accountFromStorage);
        assertEquals(2, accountFromStorage.getMechanisms().size());
    }

    @Test
    public void testShouldGetStoredAccountByMechanism() throws Exception {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withDeviceToken("s-o-m-e-t-o-k-e-n")
                .withStorage(storageClient)
                .start();

        assertNotNull(fraClient);
        assertNotNull(fraClient.getAuthenticatorManagerInstance());

        Account account = createAccount(ACCOUNT_NAME, ISSUER);
        Mechanism oath = createOathMechanism(ACCOUNT_NAME, ISSUER, OTHER_MECHANISM_UID);

        List<Mechanism> mechanismList= new ArrayList<>();
        mechanismList.add(oath);

        given(storageClient.getAccount(any(String.class))).willReturn(account);
        given(storageClient.getMechanismByUUID(any(String.class))).willReturn(push);
        given(storageClient.getMechanismsForAccount(any(Account.class))).willReturn(mechanismList);

        Account accountFromStorage = fraClient.getAccount(oath);

        assertNotNull(accountFromStorage);
        assertEquals(account, accountFromStorage);
    }

    @Test
    public void testShouldUpdateStoredAccount() throws Exception {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withDeviceToken("s-o-m-e-t-o-k-e-n")
                .withStorage(storageClient)
                .start();

        assertNotNull(fraClient);
        assertNotNull(fraClient.getAuthenticatorManagerInstance());

        Account account = createAccount(ACCOUNT_NAME, ISSUER);
        Mechanism oath = createOathMechanism(ACCOUNT_NAME, ISSUER, OTHER_MECHANISM_UID);

        List<Mechanism> mechanismList= new ArrayList<>();
        mechanismList.add(oath);

        given(storageClient.getAccount(any(String.class))).willReturn(account);
        given(storageClient.setAccount(any(Account.class))).willReturn(true);

        account.setDisplayAccountName(OTHER_ACCOUNT_NAME);
        account.setDisplayIssuer(OTHER_ISSUER);

        boolean result = fraClient.updateAccount(account);

        assertTrue(result);
        assertEquals(account.getAccountName(), ACCOUNT_NAME);
        assertEquals(account.getDisplayAccountName(), OTHER_ACCOUNT_NAME);
        assertEquals(account.getIssuer(), ISSUER);
        assertEquals(account.getDisplayIssuer(), OTHER_ISSUER);
    }

    @Test
    public void testShouldFailToUpdateLockedAccount() throws Exception {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withDeviceToken("s-o-m-e-t-o-k-e-n")
                .withStorage(storageClient)
                .start();

        assertNotNull(fraClient);
        assertNotNull(fraClient.getAuthenticatorManagerInstance());

        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .setPolicies(POLICIES)
                .setLock(true)
                .setLockingPolicy("deviceTampering")
                .build();

        Mechanism oath = createOathMechanism(ACCOUNT_NAME, ISSUER, OTHER_MECHANISM_UID);
        Mechanism push = createPushMechanism(ACCOUNT_NAME, ISSUER, MECHANISM_UID);
        List<Mechanism> mechanismList= new ArrayList<>();
        mechanismList.add(push);
        mechanismList.add(oath);

        given(storageClient.setAccount(any(Account.class))).willReturn(true);
        given(storageClient.getAccount(any(String.class))).willReturn(account);
        given(storageClient.getMechanismsForAccount(any(Account.class))).willReturn(mechanismList);

        account.setDisplayAccountName("userOne");

        try {
            fraClient.updateAccount(account);
        } catch (Exception e) {
            assertTrue(e instanceof AccountLockException);
            assertTrue(e.getLocalizedMessage()
                    .contains("This account is locked. It violates the following policy"));
            assertTrue(account.isLocked());
            assertNotNull(account.getLockingPolicy());
        }
    }

    @Test
    public void testShouldGetStoredMechanismByPushNotification() throws Exception {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withDeviceToken("s-o-m-e-t-o-k-e-n")
                .withStorage(storageClient)
                .start();

        assertNotNull(fraClient);
        assertNotNull(fraClient.getAuthenticatorManagerInstance());

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

        Mechanism mechanismFromStorage = fraClient.getMechanism(pushNotification);

        assertNotNull(mechanismFromStorage);
        assertEquals(push, mechanismFromStorage);
    }

    @Test
    public void testShouldGetAllNotifications() throws Exception {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withDeviceToken("s-o-m-e-t-o-k-e-n")
                .withStorage(storageClient)
                .start();

        assertNotNull(fraClient);
        assertNotNull(fraClient.getAuthenticatorManagerInstance());

        Account account = createAccount(ACCOUNT_NAME, ISSUER);
        Mechanism push = createPushMechanism(ACCOUNT_NAME, ISSUER, MECHANISM_UID);

        List<Mechanism> mechanismList= new ArrayList<>();
        mechanismList.add(push);

        List<PushNotification> notificationList = new ArrayList<>();
        notificationList.add(createPushNotification(MESSAGE_ID, push));
        notificationList.add(createPushNotification(OTHER_MESSAGE_ID, push));

        given(storageClient.getAccount(any(String.class))).willReturn(account);
        given(storageClient.getMechanismsForAccount(any(Account.class))).willReturn(mechanismList);
        given(storageClient.getAllNotifications()).willReturn(notificationList);

        List<PushNotification> notificationListFromStorage = fraClient.getAllNotifications();

        assertNotNull(notificationListFromStorage);
        assertEquals(notificationList.size(), notificationListFromStorage.size());
    }

    @Test
    public void testShouldGetAllNotificationsByMechanism() throws Exception {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withDeviceToken("s-o-m-e-t-o-k-e-n")
                .withStorage(storageClient)
                .start();

        assertNotNull(fraClient);
        assertNotNull(fraClient.getAuthenticatorManagerInstance());

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

        List<PushNotification> notificationListFromStorage = fraClient.getAllNotifications(push);

        assertNotNull(notificationListFromStorage);
        assertEquals(notificationList.size(), notificationListFromStorage.size());
    }

    @Test
    public void testShouldGetStoredNotificationByID() throws Exception {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withDeviceToken("s-o-m-e-t-o-k-e-n")
                .withStorage(storageClient)
                .start();

        assertNotNull(fraClient);
        assertNotNull(fraClient.getAuthenticatorManagerInstance());

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

        PushNotification notificationFromStorage = fraClient.getNotification(notification.getId());

        assertNotNull(notificationFromStorage);
        assertEquals(notification, notificationFromStorage);
    }

    @Test
    public void testShouldNotGetStoredNotificationByID() throws Exception {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withDeviceToken("s-o-m-e-t-o-k-e-n")
                .withStorage(storageClient)
                .start();

        assertNotNull(fraClient);
        assertNotNull(fraClient.getAuthenticatorManagerInstance());

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
        given(storageClient.getNotification(anyString())).willReturn(null);

        PushNotification notificationFromStorage = fraClient.getNotification("INVALID_ID");

        assertNull(notificationFromStorage);
    }

    @Test
    public void testGetAccountByIDFailNotFound() throws Exception {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withDeviceToken("s-o-m-e-t-o-k-e-n")
                .withStorage(storageClient)
                .start();

        assertNotNull(fraClient);

        given(storageClient.getAccount(any(String.class))).willReturn(null);

        Account accountFromStorage = fraClient.getAccount("s-o-m-e-i-d");

        assertNull(accountFromStorage);
    }

    @Test
    public void testShouldRemoveExistingAccount() throws Exception {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withDeviceToken("s-o-m-e-t-o-k-e-n")
                .withStorage(storageClient)
                .start();

        assertNotNull(fraClient);

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

        boolean result = fraClient.removeAccount(account);

        assertTrue(result);
    }

    @Test
    public void testShouldRemoveExistingPushMechanism() throws AuthenticatorException {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withDeviceToken("s-o-m-e-t-o-k-e-n")
                .withStorage(storageClient)
                .start();

        assertNotNull(fraClient);

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

        boolean result = fraClient.removeMechanism(push);

        assertTrue(result);
    }

    @Test
    public void testShouldRemoveExistingPushNotification() throws AuthenticatorException {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withDeviceToken("s-o-m-e-t-o-k-e-n")
                .withStorage(storageClient)
                .start();

        assertNotNull(fraClient);

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

        boolean result = fraClient.removeNotification(notificationList.get(0));

        assertTrue(result);
    }

    @Test
    public void testShouldLockAccount() throws AccountLockException, AuthenticatorException {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withDeviceToken("s-o-m-e-t-o-k-e-n")
                .withStorage(storageClient)
                .start();

        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .setPolicies(POLICIES)
                .build();

        Mechanism oath = createOathMechanism(ACCOUNT_NAME, ISSUER, OTHER_MECHANISM_UID);
        Mechanism push = createPushMechanism(ACCOUNT_NAME, ISSUER, MECHANISM_UID);
        List<Mechanism> mechanismList= new ArrayList<>();
        mechanismList.add(push);
        mechanismList.add(oath);

        FRAPolicy policy = new DeviceTamperingPolicy();

        given(storageClient.setAccount(any(Account.class))).willReturn(true);
        given(storageClient.getAccount(any(String.class))).willReturn(account);
        given(storageClient.getMechanismsForAccount(any(Account.class))).willReturn(mechanismList);

        boolean result = fraClient.lockAccount(account, policy);

        assertTrue(result);
        assertTrue(account.isLocked());
        assertEquals(account.getLockingPolicy(), policy.getName());
    }

    @Test
    public void testShouldUnlockAccount() throws AccountLockException, AuthenticatorException {
        FRAClient fraClient = FRAClient.builder()
                .withContext(context)
                .withDeviceToken("s-o-m-e-t-o-k-e-n")
                .withStorage(storageClient)
                .start();

        assertNotNull(fraClient);

        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .setPolicies(POLICIES)
                .setLock(true)
                .setLockingPolicy("deviceTampering")
                .build();

        Mechanism oath = createOathMechanism(ACCOUNT_NAME, ISSUER, OTHER_MECHANISM_UID);
        Mechanism push = createPushMechanism(ACCOUNT_NAME, ISSUER, MECHANISM_UID);
        List<Mechanism> mechanismList= new ArrayList<>();
        mechanismList.add(push);
        mechanismList.add(oath);

        given(storageClient.setAccount(any(Account.class))).willReturn(true);
        given(storageClient.getAccount(any(String.class))).willReturn(account);
        given(storageClient.getMechanismsForAccount(any(Account.class))).willReturn(mechanismList);

        boolean result = fraClient.unlockAccount(account);

        assertTrue(result);
        assertFalse(account.isLocked());
        assertNull(account.getLockingPolicy());
    }
}
