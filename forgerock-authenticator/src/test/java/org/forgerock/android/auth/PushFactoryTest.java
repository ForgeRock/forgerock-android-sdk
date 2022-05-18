/*
 * Copyright (c) 2020 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.forgerock.android.auth.exception.DuplicateMechanismException;
import org.forgerock.android.auth.exception.MechanismCreationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collections;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@RunWith(RobolectricTestRunner.class)
public class PushFactoryTest extends FRABaseTest {

    private DefaultStorageClient storageClient;
    private PushFactory factory;
    private Context context;
    private FRAListenerFuture pushListenerFuture;
    private MockWebServer server;

    private static final boolean CLEAN_UP_DATA = false;

    @After
    public void cleanUp() {
        if(CLEAN_UP_DATA) {
            storageClient.removeAll();
        }
    }

    @Before
    public void setUp() throws IOException {
        context = ApplicationProvider.getApplicationContext();

        server = new MockWebServer();
        server.start();

        pushListenerFuture = new FRAListenerFuture<Mechanism>();

        storageClient = new DefaultStorageClient(context);
        storageClient.setAccountData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE));
        storageClient.setMechanismData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_MECHANISM, Context.MODE_PRIVATE));
        storageClient.setNotificationData(context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_NOTIFICATIONS, Context.MODE_PRIVATE));

        PushResponder.getInstance(storageClient);

        factory = spy(new PushFactory(context, storageClient, "s-o-m-e-i-d"));
        doReturn(true).when(factory).checkGooglePlayServices();
    }

    @After
    public void shutdown() throws IOException {
        server.shutdown();
    }

    @Test
    public void testShouldParseVersionOne() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK));

        String uri = "pushauth://push/forgerock:demo?" +
                "a=" + getBase64PushActionUrl(server, "authenticate") + "&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=" + getBase64PushActionUrl(server, "register") + "&" +
                "s=ryJkqNRjXYd_nX523672AX_oKdVXrKExq-VjVeRKKTc&" +
                "c=Daf8vrc8onKu-dcptwCRS9UHmdui5u16vAdG2HMU4w0&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "issuer=Rm9yZ2Vyb2Nr&" +
                "version=1";

        factory.createFromUri(uri, pushListenerFuture);
        PushMechanism push = (PushMechanism) pushListenerFuture.get();
        assertEquals(push.getType(), Mechanism.PUSH);
        assertEquals(push.getAccountName(), "demo");
        assertEquals(push.getIssuer(), "Forgerock");
    }

    @Test
    public void testShouldHandleDefaultVersion() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK));

        String uri = "pushauth://push/forgerock:demo?" +
                "a=" + getBase64PushActionUrl(server, "authenticate") + "&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=" + getBase64PushActionUrl(server, "register") + "&" +
                "s=ryJkqNRjXYd_nX523672AX_oKdVXrKExq-VjVeRKKTc&" +
                "c=Daf8vrc8onKu-dcptwCRS9UHmdui5u16vAdG2HMU4w0&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "issuer=Rm9yZ2Vyb2Nr";

        factory.createFromUri(uri, pushListenerFuture);
        PushMechanism push = (PushMechanism) pushListenerFuture.get();
        assertEquals(push.getType(), Mechanism.PUSH);
        assertEquals(push.getAccountName(), "demo");
        assertEquals(push.getIssuer(), "Forgerock");
    }

    @Test
    public void testShouldRejectInvalidVersion() {
        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK));

        String uri = "pushauth://push/forgerock:demo?" +
                "a=" + getBase64PushActionUrl(server, "authenticate") + "&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=" + getBase64PushActionUrl(server, "register") + "&" +
                "s=ryJkqNRjXYd_nX523672AX_oKdVXrKExq-VjVeRKKTc&" +
                "c=Daf8vrc8onKu-dcptwCRS9UHmdui5u16vAdG2HMU4w0&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "issuer=Rm9yZ2Vyb2Nr&" +
                "version=999";

        try {
            factory.createFromUri(uri, pushListenerFuture);
            pushListenerFuture.get();
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("Unknown version:"));
        }
    }

    @Test
    public void testShouldRejectInvalidVersionNotANumber() {
        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK));

        String uri = "pushauth://push/forgerock:demo?" +
                "a=" + getBase64PushActionUrl(server, "authenticate") + "&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=" + getBase64PushActionUrl(server, "register") + "&" +
                "s=ryJkqNRjXYd_nX523672AX_oKdVXrKExq-VjVeRKKTc&" +
                "c=Daf8vrc8onKu-dcptwCRS9UHmdui5u16vAdG2HMU4w0&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "issuer=Rm9yZ2Vyb2Nr&" +
                "version=a";

        try {
            factory.createFromUri(uri, pushListenerFuture);
            PushMechanism push = (PushMechanism) pushListenerFuture.get();
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("Expected valid integer"));
        }
    }

    @Test
    public void testFailToSaveAccount() {
        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK));

        String uri = "pushauth://push/forgerock:demo?" +
                "a=" + getBase64PushActionUrl(server, "authenticate") + "&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=" + getBase64PushActionUrl(server, "register") + "&" +
                "s=ryJkqNRjXYd_nX523672AX_oKdVXrKExq-VjVeRKKTc&" +
                "c=Daf8vrc8onKu-dcptwCRS9UHmdui5u16vAdG2HMU4w0&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "issuer=Rm9yZ2Vyb2Nr";

        StorageClient storageClient = mock(DefaultStorageClient.class);
        given(storageClient.getAccount(any(String.class))).willReturn( null);
        given(storageClient.setAccount(any(Account.class))).willReturn(false);
        given(storageClient.getMechanismsForAccount(any(Account.class))).willReturn(Collections.emptyList());
        given(storageClient.setMechanism(any(Mechanism.class))).willReturn(true);
        factory = spy(new PushFactory(context, storageClient, "s-o-m-e-i-d"));
        doReturn(true).when(factory).checkGooglePlayServices();

        try {
            factory.createFromUri(uri, pushListenerFuture);
            pushListenerFuture.get();
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("Error while storing the Account"));
        }
    }

    @Test
    public void testFailToSaveMechanism() {
        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK));

        String uri = "pushauth://push/forgerock:demo?" +
                "a=" + getBase64PushActionUrl(server, "authenticate") + "&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=" + getBase64PushActionUrl(server, "register") + "&" +
                "s=ryJkqNRjXYd_nX523672AX_oKdVXrKExq-VjVeRKKTc&" +
                "c=Daf8vrc8onKu-dcptwCRS9UHmdui5u16vAdG2HMU4w0&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "issuer=Rm9yZ2Vyb2Nr";

        StorageClient storageClient = mock(DefaultStorageClient.class);
        given(storageClient.getAccount(any(String.class))).willReturn(null);
        given(storageClient.setAccount(any(Account.class))).willReturn(true);
        given(storageClient.setMechanism(any(Mechanism.class))).willReturn(false);
        factory = spy(new PushFactory(context, storageClient, "s-o-m-e-i-d"));
        doReturn(true).when(factory).checkGooglePlayServices();

        try {
            factory.createFromUri(uri, pushListenerFuture);
            pushListenerFuture.get();
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("Error storing the mechanism"));
        }
    }

    @Test
    public void testShouldFailDuplicatedMechanism() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK));

        String uri = "pushauth://push/forgerock:demo?" +
                "a=" + getBase64PushActionUrl(server, "authenticate") + "&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=" + getBase64PushActionUrl(server, "register") + "&" +
                "s=ryJkqNRjXYd_nX523672AX_oKdVXrKExq-VjVeRKKTc&" +
                "c=Daf8vrc8onKu-dcptwCRS9UHmdui5u16vAdG2HMU4w0&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "issuer=Rm9yZ2Vyb2Nr";

        factory.createFromUri(uri, pushListenerFuture);
        PushMechanism push = (PushMechanism) pushListenerFuture.get();
        assertEquals(push.getType(), Mechanism.PUSH);
        assertEquals(push.getAccountName(), "demo");
        assertEquals(push.getIssuer(), "Forgerock");

        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK));
        FRAListenerFuture pushListenerFuture2 = new FRAListenerFuture<Mechanism>();
        try {
            factory.createFromUri(uri, pushListenerFuture2);
            pushListenerFuture2.get();
            Assert.fail("Should throw DuplicateMechanismException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof DuplicateMechanismException);
            assertTrue(e.getLocalizedMessage().contains("Matching mechanism already exists"));
            assertTrue(((DuplicateMechanismException) e.getCause()).getCausingMechanism() instanceof PushMechanism);
        }
    }

    @Test
    public void testShouldFailNoFCMToken() {
        String uri = "pushauth://push/forgerock:demo?" +
                "a=" + getBase64PushActionUrl(server, "authenticate") + "&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=" + getBase64PushActionUrl(server, "register") + "&" +
                "s=ryJkqNRjXYd_nX523672AX_oKdVXrKExq-VjVeRKKTc&" +
                "c=Daf8vrc8onKu-dcptwCRS9UHmdui5u16vAdG2HMU4w0&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "issuer=Rm9yZ2Vyb2Nr&" +
                "version=1";

        factory = spy(new PushFactory(context, storageClient, ""));
        doReturn(true).when(factory).checkGooglePlayServices();

        try {
            factory.createFromUri(uri, pushListenerFuture);
            pushListenerFuture.get();
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("Invalid FCM token"));
        }
    }

    @Test
    public void testShouldFailGoogleServicesNotEnabled() {
        String uri = "pushauth://push/forgerock:demo?" +
                "a=" + getBase64PushActionUrl(server, "authenticate") + "&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=" + getBase64PushActionUrl(server, "register") + "&" +
                "s=ryJkqNRjXYd_nX523672AX_oKdVXrKExq-VjVeRKKTc&" +
                "c=Daf8vrc8onKu-dcptwCRS9UHmdui5u16vAdG2HMU4w0&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "issuer=Rm9yZ2Vyb2Nr&" +
                "version=1";

        factory = spy(new PushFactory(context, storageClient, "s-o-m-e-i-d"));
        doReturn(false).when(factory).checkGooglePlayServices();
        try {
            factory.createFromUri(uri, pushListenerFuture);
            pushListenerFuture.get();
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("Google Play Services not enabled"));
        }
    }

    @Test
    public void testShouldReflectDifferences() throws Exception {
        String uri = "pushauth://push/forgerock:demo?" +
                "a=" + getBase64PushActionUrl(server, "authenticate") + "&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=" + getBase64PushActionUrl(server, "register") + "&" +
                "s=ryJkqNRjXYd_nX523672AX_oKdVXrKExq-VjVeRKKTc&" +
                "c=Daf8vrc8onKu-dcptwCRS9UHmdui5u16vAdG2HMU4w0&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "issuer=Rm9yZ2Vyb2Nr";

        String secondUri = "pushauth://push/forgerock:user?" +
                "a=" + getBase64PushActionUrl(server, "authenticate") + "&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=" + getBase64PushActionUrl(server, "register") + "&" +
                "s=ryJkqNRjXYd_nX523672AX_oKdVXrKExq-VjVeRKKTc&" +
                "c=Daf8vrc8onKu-dcptwCRS9UHmdui5u16vAdG2HMU4w0&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "issuer=RXhhbXBsZQ";

        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK));
        factory.createFromUri(uri, pushListenerFuture);
        PushMechanism push = (PushMechanism) pushListenerFuture.get();

        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK));
        pushListenerFuture = new FRAListenerFuture<Mechanism>();
        factory.createFromUri(secondUri, pushListenerFuture);
        PushMechanism secondPush = (PushMechanism) pushListenerFuture.get();

        assertNotEquals(secondPush, push);
    }

    @Test
    public void testCannotConnectToServer() {
        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_GATEWAY));

        String uri = "pushauth://push/forgerock:demo?" +
                "a=" + getBase64PushActionUrl(server, "authenticate") + "&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=" + getBase64PushActionUrl(server, "register") + "&" +
                "s=ryJkqNRjXYd_nX523672AX_oKdVXrKExq-VjVeRKKTc&" +
                "c=Daf8vrc8onKu-dcptwCRS9UHmdui5u16vAdG2HMU4w0&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "issuer=Rm9yZ2Vyb2Nr";
        try {
            factory.createFromUri(uri, pushListenerFuture);
            pushListenerFuture.get();
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("Communication with server returned 502 code."));
        }
    }

    @Test
    public void testNetworkIssue() {
        MockResponse response = new MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START);
        server.enqueue(response);

        String uri = "pushauth://push/forgerock:demo?" +
                "a=" + getBase64PushActionUrl(server, "authenticate") + "&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=" + getBase64PushActionUrl(server, "register") + "&" +
                "s=ryJkqNRjXYd_nX523672AX_oKdVXrKExq-VjVeRKKTc&" +
                "c=Daf8vrc8onKu-dcptwCRS9UHmdui5u16vAdG2HMU4w0&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "issuer=Rm9yZ2Vyb2Nr";
        try {
            factory.createFromUri(uri, pushListenerFuture);
            pushListenerFuture.get();
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("Failed to register with server"));
        }
    }

}
