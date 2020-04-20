/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

import android.content.Context;

import org.forgerock.android.auth.DefaultStorageClient;
import org.forgerock.android.authenticator.exception.MechanismCreationException;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class PushFactoryTest {

    private DefaultStorageClient storageClient;
    private PushFactory factory;
    private Context context;

    @Before
    public void setUp() throws IOException, JSONException {
        context = mock(Context.class);

        storageClient = mock(DefaultStorageClient.class);
        given(storageClient.setAccount(any(Account.class))).willReturn(true);
        given(storageClient.setMechanism(any(Push.class))).willReturn(true);

        factory = spy(new PushFactory(context, storageClient, "s-o-m-e-i-d"));
        doReturn(true).when(factory).checkGooglePlayServices();
        doReturn(200).when(factory).performPushRegistration(any(String.class),
                any(String.class), any(String.class), any(String.class), anyMap());
    }

    @Test
    public void testShouldParseVersionOne() throws Exception {
        String uri = "pushauth://push/forgerock:demo?a=aHR0cDovL2FtcWEtY2xvbmU2OS50ZXN0LmZvcmdlcm9jay5jb206ODA4MC9vcGVuYW0vanNvbi9wdXNoL3Nucy9tZXNzYWdlP19hY3Rpb249YXV0aGVudGljYXRl&image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&b=ff00ff&r=aHR0cDovL2FtcWEtY2xvbmU2OS50ZXN0LmZvcmdlcm9jay5jb206ODA4MC9vcGVuYW0vanNvbi9wdXNoL3Nucy9tZXNzYWdlP19hY3Rpb249cmVnaXN0ZXI=&s=dA18Iph3slIUDVuRc5+3y7nv9NLGnPksH66d3jIF6uE=&c=Yf66ojm3Pm80PVvNpljTB6X9CUhgSJ0WZUzB4su3vCY=&l=YW1sYmNvb2tpZT0wMQ==&m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&issuer=Rm9yZ2Vyb2Nr&version=1";
        Push push = (Push) factory.createFromUri(uri);
        assertEquals(push.getType(), Mechanism.PUSH);
        assertEquals(push.getAccountName(), "demo");
        assertEquals(push.getIssuer(), "Forgerock");
    }

    @Test
    public void testShouldHandleDefaultVersion() throws Exception {
        String uri = "pushauth://push/forgerock:demo?a=aHR0cDovL2FtcWEtY2xvbmU2OS50ZXN0LmZvcmdlcm9jay5jb206ODA4MC9vcGVuYW0vanNvbi9wdXNoL3Nucy9tZXNzYWdlP19hY3Rpb249YXV0aGVudGljYXRl&image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&b=ff00ff&r=aHR0cDovL2FtcWEtY2xvbmU2OS50ZXN0LmZvcmdlcm9jay5jb206ODA4MC9vcGVuYW0vanNvbi9wdXNoL3Nucy9tZXNzYWdlP19hY3Rpb249cmVnaXN0ZXI=&s=dA18Iph3slIUDVuRc5+3y7nv9NLGnPksH66d3jIF6uE=&c=Yf66ojm3Pm80PVvNpljTB6X9CUhgSJ0WZUzB4su3vCY=&l=YW1sYmNvb2tpZT0wMQ==&m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&issuer=Rm9yZ2Vyb2Nr";
        Push push = (Push) factory.createFromUri(uri);
        assertEquals(push.getType(), Mechanism.PUSH);
        assertEquals(push.getAccountName(), "demo");
        assertEquals(push.getIssuer(), "Forgerock");
    }

    @Test
    public void testShouldRejectInvalidVersion() throws Exception {
        String uri = "pushauth://push/forgerock:demo?a=aHR0cDovL2FtcWEtY2xvbmU2OS50ZXN0LmZvcmdlcm9jay5jb206ODA4MC9vcGVuYW0vanNvbi9wdXNoL3Nucy9tZXNzYWdlP19hY3Rpb249YXV0aGVudGljYXRl&image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&b=ff00ff&r=aHR0cDovL2FtcWEtY2xvbmU2OS50ZXN0LmZvcmdlcm9jay5jb206ODA4MC9vcGVuYW0vanNvbi9wdXNoL3Nucy9tZXNzYWdlP19hY3Rpb249cmVnaXN0ZXI=&s=dA18Iph3slIUDVuRc5+3y7nv9NLGnPksH66d3jIF6uE=&c=Yf66ojm3Pm80PVvNpljTB6X9CUhgSJ0WZUzB4su3vCY=&l=YW1sYmNvb2tpZT0wMQ==&m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&issuer=Rm9yZ2Vyb2Nr&version=99999";
        try {
            factory.createFromUri(uri);
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e instanceof MechanismCreationException);
        }
    }

    @Test
    public void testShouldReflectDifferences() throws Exception {
        String uri = "pushauth://push/forgerock:demo?a=aHR0cDovL2FtcWEtY2xvbmU2OS50ZXN0LmZvcmdlcm9jay5jb206ODA4MC9vcGVuYW0vanNvbi9wdXNoL3Nucy9tZXNzYWdlP19hY3Rpb249YXV0aGVudGljYXRl&image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&b=ff00ff&r=aHR0cDovL2FtcWEtY2xvbmU2OS50ZXN0LmZvcmdlcm9jay5jb206ODA4MC9vcGVuYW0vanNvbi9wdXNoL3Nucy9tZXNzYWdlP19hY3Rpb249cmVnaXN0ZXI=&s=dA18Iph3slIUDVuRc5+3y7nv9NLGnPksH66d3jIF6uE=&c=Yf66ojm3Pm80PVvNpljTB6X9CUhgSJ0WZUzB4su3vCY=&l=YW1sYmNvb2tpZT0wMQ==&m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&issuer=Rm9yZ2Vyb2Nr";
        String secondUri = "pushauth://push/forgerock:user?a=aHR0cDovL2FtcWEtY2xvbmU2OS50ZXN0LmZvcmdlcm9jay5jb206ODA4MC9vcGVuYW0vanNvbi9wdXNoL3Nucy9tZXNzYWdlP19hY3Rpb249YXV0aGVudGljYXRl&image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&b=ff00ff&r=aHR0cDovL2FtcWEtY2xvbmU2OS50ZXN0LmZvcmdlcm9jay5jb206ODA4MC9vcGVuYW0vanNvbi9wdXNoL3Nucy9tZXNzYWdlP19hY3Rpb249cmVnaXN0ZXI=&s=dA18Iph3slIUDVuRc5+3y7nv9NLGnPksH66d3jIF6uE=&c=Yf66ojm3Pm80PVvNpljTB6X9CUhgSJ0WZUzB4su3vCY=&l=YW1sYmNvb2tpZT0wMQ==&m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&issuer=RXhhbXBsZQ";
        Push push = (Push) factory.createFromUri(uri);
        Push secondPush = (Push) factory.createFromUri(secondUri);

        assertNotEquals(secondPush, push);
    }

    @Test
    public void testCannotConnectToServer() throws Exception {
        factory = spy(new PushFactory(context, storageClient, "s-o-m-e-i-d"));
        doReturn(true).when(factory).checkGooglePlayServices();

        String uri = "pushauth://push/forgerock:demo?a=aHR0cDovL2FtcWEtY2xvbmU2OS50ZXN0LmZvcmdlcm9jay5jb206ODA4MC9vcGVuYW0vanNvbi9wdXNoL3Nucy9tZXNzYWdlP19hY3Rpb249YXV0aGVudGljYXRl&image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&b=ff00ff&r=aHR0cDovL2FtcWEtY2xvbmU2OS50ZXN0LmZvcmdlcm9jay5jb206ODA4MC9vcGVuYW0vanNvbi9wdXNoL3Nucy9tZXNzYWdlP19hY3Rpb249cmVnaXN0ZXI=&s=dA18Iph3slIUDVuRc5+3y7nv9NLGnPksH66d3jIF6uE=&c=Yf66ojm3Pm80PVvNpljTB6X9CUhgSJ0WZUzB4su3vCY=&l=YW1sYmNvb2tpZT0wMQ==&m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&issuer=Rm9yZ2Vyb2Nr";
        try {
            factory.createFromUri(uri);
            Assert.fail("Should throw MechanismCreationException");
        } catch (Exception e) {
            assertTrue(e instanceof MechanismCreationException);
            assertTrue(e.getLocalizedMessage().contains("Failed to register with server."));
        }
    }

}
