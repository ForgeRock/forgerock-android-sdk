/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.collector;

import android.content.Context;
import android.provider.Settings;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.forgerock.android.auth.Config;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import static org.forgerock.android.auth.Encryptor.ANDROID_KEYSTORE;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FRDeviceIdentifierTest {


    private Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void testDeviceId() {

        Config.getInstance(context);
        String instanceId = new DeviceIdentifier(context).getIdentifier();
        //Just make sure it generate the same value every it calls getInstanceId()
        assertEquals(instanceId, new DeviceIdentifier(context).getIdentifier());
        assertEquals(instanceId, new DeviceIdentifier(context).getIdentifier());
        assertEquals(instanceId, new DeviceIdentifier(context).getIdentifier());
    }

    @Test
    public void testRegenerateDeviceId() throws GeneralSecurityException, IOException {

        Config.getInstance(context);
        String instanceId = new DeviceIdentifier(context).getIdentifier();

        getKeyStore().deleteEntry(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
        //Just make sure it generate the same value every it calls getInstanceId()
        assertNotEquals(instanceId, new DeviceIdentifier(context).getIdentifier());

    }

    private KeyStore getKeyStore()
            throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        return keyStore;
    }
}
