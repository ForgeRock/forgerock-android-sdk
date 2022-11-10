/*
 * Copyright (c) 2019 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

import android.content.Context;
import android.provider.Settings;
import android.util.Base64;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.Certificate;

@RunWith(PowerMockRunner.class)
@PrepareForTest({KeyStoreManager.class, Settings.Secure.class, Base64.class})
public class KeyStoreManagerTest {

    private KeyStoreManager keyStoreManager;
    private KeyStore keyStore = PowerMockito.mock(KeyStore.class);

    @Before
    public void deviceIdentifierSpy() throws Exception {
        Context context = PowerMockito.mock(Context.class);
        PowerMockito.mockStatic(Settings.Secure.class);
        PowerMockito.when(Settings.Secure.getString(any(), anyString())).thenReturn("Test");

        PowerMockito.mockStatic(KeyStore.class);
        PublicKey publicKey = PowerMockito.mock(PublicKey.class);
        byte[] encoded = "public key".getBytes();
        PowerMockito.when(keyStore.containsAlias(anyString())).thenReturn(true);
        PowerMockito.when(publicKey.getEncoded()).thenReturn(encoded);
        Certificate certificate = PowerMockito.mock(Certificate.class);
        PowerMockito.when(certificate.getPublicKey()).thenReturn(publicKey);
        PowerMockito.when(keyStore.containsAlias(anyString())).thenReturn(true);
        PowerMockito.when(keyStore.getCertificate(anyString())).thenReturn(certificate);
        PowerMockito.when(KeyStore.getInstance(anyString())).thenReturn(keyStore);

        PowerMockito.mockStatic(Base64.class);
        PowerMockito.when(Base64.encodeToString(any(), anyInt())).thenReturn("data");

        PowerMockito.mockStatic(KeyPairGenerator.class);
        keyStoreManager = PowerMockito.spy(new KeyStoreManager(context));

    }

    @Test
    public void testIdentifierKey() throws Exception {
        String identifier = new String(keyStoreManager.getIdentifierKey("test").getEncoded());
        Assert.assertEquals("public key", identifier);
    }

    @Test
    public void testGetCertificate() throws GeneralSecurityException, IOException {
        Certificate certificate = keyStoreManager.getCertificate("test");
        Assertions.assertThat(certificate).isNotNull();
    }

}
