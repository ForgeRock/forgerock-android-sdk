/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.util.Base64;

import com.nimbusds.jose.JOSEException;

import org.forgerock.android.auth.Account;
import org.forgerock.android.auth.DefaultStorageClient;
import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.Push;
import org.forgerock.android.auth.PushFactory;
import org.forgerock.android.auth.exception.MechanismCreationException;
import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.net.HttpURLConnection;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@RunWith(RobolectricTestRunner.class)
public class PushFactoryTest {

    private DefaultStorageClient storageClient;
    private PushFactory factory;
    private Context context;
    private FRAListenerFuture pushListenerFuture;
    private MockWebServer server;

    @Before
    public void setUp() throws IOException {
        context = mock(Context.class);

        server = new MockWebServer();
        server.start();

        pushListenerFuture = new FRAListenerFuture<Mechanism>();

        storageClient = mock(DefaultStorageClient.class);
        given(storageClient.setAccount(any(Account.class))).willReturn(true);
        given(storageClient.setMechanism(any(Push.class))).willReturn(true);

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
                "a=" + getBase64PushActionUrl("authenticate") + "&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=" + getBase64PushActionUrl("register") + "&" +
                "s=dA18Iph3slIUDVuRc5+3y7nv9NLGnPksH66d3jIF6uE=&" +
                "c=Yf66ojm3Pm80PVvNpljTB6X9CUhgSJ0WZUzB4su3vCY=&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "issuer=Rm9yZ2Vyb2Nr&" +
                "version=1";

        factory.createFromUri(uri, pushListenerFuture);
        Push push = (Push) pushListenerFuture.get();
        assertEquals(push.getType(), Mechanism.PUSH);
        assertEquals(push.getAccountName(), "demo");
        assertEquals(push.getIssuer(), "Forgerock");
    }

    @Test
    public void testShouldHandleDefaultVersion() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK));

        String uri = "pushauth://push/forgerock:demo?" +
                "a=" + getBase64PushActionUrl("authenticate") + "&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=" + getBase64PushActionUrl("register") + "&" +
                "s=dA18Iph3slIUDVuRc5+3y7nv9NLGnPksH66d3jIF6uE=&" +
                "c=Yf66ojm3Pm80PVvNpljTB6X9CUhgSJ0WZUzB4su3vCY=&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "issuer=Rm9yZ2Vyb2Nr";

        factory.createFromUri(uri, pushListenerFuture);
        Push push = (Push) pushListenerFuture.get();
        assertEquals(push.getType(), Mechanism.PUSH);
        assertEquals(push.getAccountName(), "demo");
        assertEquals(push.getIssuer(), "Forgerock");
    }

    @Test
    public void testShouldRejectInvalidVersion() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK));

        String uri = "pushauth://push/forgerock:demo?" +
                "a=" + getBase64PushActionUrl("authenticate") + "&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=" + getBase64PushActionUrl("register") + "&" +
                "s=dA18Iph3slIUDVuRc5+3y7nv9NLGnPksH66d3jIF6uE=&" +
                "c=Yf66ojm3Pm80PVvNpljTB6X9CUhgSJ0WZUzB4su3vCY=&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "issuer=Rm9yZ2Vyb2Nr&" +
                "version=999";

        try {
            factory.createFromUri(uri, pushListenerFuture);
            Push push = (Push) pushListenerFuture.get();
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getLocalizedMessage().contains("Unknown version:"));
        }
    }

    @Test
    public void testShouldReflectDifferences() throws Exception {
        String uri = "pushauth://push/forgerock:demo?" +
                "a=" + getBase64PushActionUrl("authenticate") + "&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=" + getBase64PushActionUrl("register") + "&" +
                "s=dA18Iph3slIUDVuRc5+3y7nv9NLGnPksH66d3jIF6uE=&" +
                "c=Yf66ojm3Pm80PVvNpljTB6X9CUhgSJ0WZUzB4su3vCY=&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "issuer=Rm9yZ2Vyb2Nr";

        String secondUri = "pushauth://push/forgerock:user?" +
                "a=" + getBase64PushActionUrl("authenticate") + "&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=" + getBase64PushActionUrl("register") + "&" +
                "s=dA18Iph3slIUDVuRc5+3y7nv9NLGnPksH66d3jIF6uE=&" +
                "c=Yf66ojm3Pm80PVvNpljTB6X9CUhgSJ0WZUzB4su3vCY=&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "issuer=RXhhbXBsZQ";


       // String secondUri = "pushauth://push/forgerock:user?a=aHR0cDovL2FtcWEtY2xvbmU2OS50ZXN0LmZvcmdlcm9jay5jb206ODA4MC9vcGVuYW0vanNvbi9wdXNoL3Nucy9tZXNzYWdlP19hY3Rpb249YXV0aGVudGljYXRl&image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&b=ff00ff&r=aHR0cDovL2FtcWEtY2xvbmU2OS50ZXN0LmZvcmdlcm9jay5jb206ODA4MC9vcGVuYW0vanNvbi9wdXNoL3Nucy9tZXNzYWdlP19hY3Rpb249cmVnaXN0ZXI=&s=dA18Iph3slIUDVuRc5+3y7nv9NLGnPksH66d3jIF6uE=&c=Yf66ojm3Pm80PVvNpljTB6X9CUhgSJ0WZUzB4su3vCY=&l=YW1sYmNvb2tpZT0wMQ==&m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&issuer=RXhhbXBsZQ";
        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK));
        factory.createFromUri(uri, pushListenerFuture);
        Push push = (Push) pushListenerFuture.get();

        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK));
        pushListenerFuture = new FRAListenerFuture<Mechanism>();
        factory.createFromUri(secondUri, pushListenerFuture);
        Push secondPush = (Push) pushListenerFuture.get();

        assertNotEquals(secondPush, push);
    }

    @Test
    public void testCannotConnectToServer() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_GATEWAY));

        String uri = "pushauth://push/forgerock:demo?" +
                "a=" + getBase64PushActionUrl("authenticate") + "&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=" + getBase64PushActionUrl("register") + "&" +
                "s=dA18Iph3slIUDVuRc5+3y7nv9NLGnPksH66d3jIF6uE=&" +
                "c=Yf66ojm3Pm80PVvNpljTB6X9CUhgSJ0WZUzB4su3vCY=&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "issuer=Rm9yZ2Vyb2Nr";
        try {
            factory.createFromUri(uri, pushListenerFuture);
            pushListenerFuture.get();
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e.getLocalizedMessage().contains("Communication with server returned 502 code."));
        }
    }

    private String getBase64PushActionUrl(String actionType) {
        String baseUrl = server.url("/").toString() + "openam/json/push/sns/message?_action=" + actionType;
        String base64Url = Base64.encodeToString(baseUrl.getBytes(), Base64.NO_WRAP);
        return base64Url;
    }

}
