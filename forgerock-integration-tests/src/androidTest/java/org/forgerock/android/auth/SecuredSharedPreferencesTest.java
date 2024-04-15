/*
 * Copyright (c) 2019 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.security.KeyStore;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 *
 * Instrumented test, which will execute on an Android device.
 * Use real device keystore to test the encrypt/decrypt
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidVersionAwareTestRunner.class)
public class SecuredSharedPreferencesTest {

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String TEST_ALIAS = "TestAlias";

    private SharedPreferences sharedPreferences;
    private Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        sharedPreferences = new SecuredSharedPreferences(context, "test", TEST_ALIAS);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        keyStore.deleteEntry(TEST_ALIAS);

        Context context = ApplicationProvider.getApplicationContext();
        String filePath = context.getFilesDir().getParent() + "/shared_prefs/test.xml";
        File deletePrefFile = new File(filePath);
        deletePrefFile.delete();
        AndroidMEncryptor.Companion.getKeyReferenceCache().set(null);
    }

    @Test
    public void testCache() {
        AndroidMEncryptor.Companion.getKeyReferenceCache().set(null);
        sharedPreferences.edit().putString("Test", "Value").commit();
        assertEquals("Value", sharedPreferences.getString("Test", null));
        assertNotNull(AndroidMEncryptor.Companion.getKeyReferenceCache().get());
    }


    @Test
    public void testPutString() {
        sharedPreferences.edit().putString("Test", "Value").commit();
        assertEquals("Value", sharedPreferences.getString("Test", null));
        assertNotNull(AndroidMEncryptor.Companion.getKeyReferenceCache().get());
    }

    @Test
    public void testPutInt() {
        sharedPreferences.edit().putInt("Test", 100).commit();
        assertEquals(100, sharedPreferences.getInt("Test", 0));
        assertNotNull(AndroidMEncryptor.Companion.getKeyReferenceCache().get());
    }

    @Test
    public void testPutFloat() {
        sharedPreferences.edit().putFloat("Test", 1.5f).commit();
        assertEquals(1.5f, sharedPreferences.getFloat("Test", 1.5f), 0);
        assertNotNull(AndroidMEncryptor.Companion.getKeyReferenceCache().get());
    }

    @Test
    public void testPutLong() {
        sharedPreferences.edit().putLong("Test", 100L).commit();
        assertEquals(100L, sharedPreferences.getLong("Test", 0L));
    }

    @Test
    public void testPutBoolean() {
        sharedPreferences.edit().putBoolean("Test", true).commit();
        assertTrue(sharedPreferences.getBoolean("Test", false));
    }

    @Test
    public void testPutStringSet() {
        Set<String> stringSet = new HashSet<>();
        stringSet.add("Test1");
        stringSet.add("Test2");
        sharedPreferences.edit().putStringSet("Test", stringSet).commit();
        assertEquals(2, sharedPreferences.getStringSet("Test", null).size());
    }

    @Test
    public void testClear() {
        sharedPreferences.edit().putString("Test", "Value").commit();
        sharedPreferences.edit().clear().putString("Test2", "Value").commit();

        assertNull(sharedPreferences.getString("Test", null));
        assertEquals("Value", sharedPreferences.getString("Test2", null));
    }

    @Test
    public void testGetAll() {
        sharedPreferences.edit()
                .putString("Test", "Value")
                .putString("Test2", "Value2").commit();

        assertEquals(2, sharedPreferences.getAll().size());
        assertEquals("Value", sharedPreferences.getAll().get("Test"));
        assertEquals("Value2", sharedPreferences.getAll().get("Test2"));
    }

    @Test
    public void testListenerOnCommit() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        sharedPreferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                countDownLatch.countDown();
            }
        });
        sharedPreferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                countDownLatch.countDown();
            }
        });
        sharedPreferences.edit().putString("Test", "Value").commit();
        countDownLatch.await();
    }

    @Test
    public void testListenerOnApply() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        sharedPreferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                countDownLatch.countDown();
            }
        });
        sharedPreferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                countDownLatch.countDown();
            }
        });
        sharedPreferences.edit().putString("Test", "Value").apply();
        countDownLatch.await();
    }

    @Test
    public void testSetNull() {
        sharedPreferences.edit().putString("Test", "Value").commit();
        sharedPreferences.edit().putString("Test", null).commit();
        assertNull(sharedPreferences.getString("Test", null));
    }

}
