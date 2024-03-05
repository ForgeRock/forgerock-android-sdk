/*
 * Copyright (c) 2019 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.collector;

import static org.forgerock.android.auth.Encryptor.ANDROID_KEYSTORE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.provider.Settings;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.forgerock.android.auth.AndroidBaseTest;
import org.forgerock.android.auth.Config;
import org.forgerock.android.auth.DeviceIdentifier;
import org.forgerock.android.auth.KeyStoreManager;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

@RunWith(AndroidJUnit4.class)
public class FRDeviceIdentifierTest extends AndroidBaseTest {

    @Test
    public void testDeviceId() {

        Config.getInstance().init(context, null);
        DeviceIdentifier deviceIdentifier = DeviceIdentifier.builder()
                .context(context)
                .keyStoreManager(KeyStoreManager.builder().context(context).build())
                .build();

        String instanceId = deviceIdentifier.getIdentifier();
        //Just make sure it generate the same value every it calls getInstanceId()
        assertEquals(instanceId, deviceIdentifier.getIdentifier());
        assertEquals(instanceId, deviceIdentifier.getIdentifier());
        assertEquals(instanceId, deviceIdentifier.getIdentifier());
    }

    @Test
    public void testRegenerateDeviceId() throws GeneralSecurityException, IOException {

        Config.getInstance().init(context,  null);
        DeviceIdentifier deviceIdentifier = DeviceIdentifier.builder()
                .context(context)
                .keyStoreManager(KeyStoreManager.builder().context(context).build())
                .build();
        String instanceId = deviceIdentifier.getIdentifier();

        getKeyStore().deleteEntry(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
        //Just make sure it generate the same value every it calls getInstanceId()
        assertNotEquals(instanceId, deviceIdentifier.getIdentifier());

    }

    private KeyStore getKeyStore()
            throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        return keyStore;
    }
}
