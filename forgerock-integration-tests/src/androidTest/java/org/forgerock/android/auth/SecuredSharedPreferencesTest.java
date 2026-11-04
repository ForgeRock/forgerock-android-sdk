/*
 * Copyright (c) 2019 - 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

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
    private final Context context = ApplicationProvider.getApplicationContext();

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
    }

    @Test
    public void testPutString() {
        sharedPreferences.edit().putString("Test", "Value").commit();
        assertEquals("Value", sharedPreferences.getString("Test", null));
    }

    @Test
    public void testPutInt() {
        sharedPreferences.edit().putInt("Test", 100).commit();
        assertEquals(100, sharedPreferences.getInt("Test", 0));
    }

    @Test
    public void testPutFloat() {
        sharedPreferences.edit().putFloat("Test", 1.5f).commit();
        assertEquals(1.5f, sharedPreferences.getFloat("Test", 1.5f), 0);
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

    /**
     * The corrupted payload shape from SDKS-5549: a valid JSON prefix that cuts off
     * mid-key "realm", followed by non-printable bytes.
     */
    private byte[] corruptedPayload() {
        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.redacted-session-token-value-0123456789abcdef";
        // Mirrors the ticket's decrypted plaintext: the outer wrapper holds the SSOToken
        // JSON as a string (org.json escapes quotes and forward slashes), cut off mid-key
        // "realm" and followed by non-printable bytes.
        String prefix = "{\"type\":0,\"value\":\"{\\\"value\\\":\\\"" + token + "\\\"," +
                "\\\"successUrl\\\":\\\"\\\\/enduser\\\\/?realm=\\\\/alpha\\\",\\\"realm\\\"";
        byte[] garbage = new byte[]{0x00, 0x01, 0x7F, (byte) 0xC3, (byte) 0xFF};
        byte[] out = new byte[prefix.getBytes().length + garbage.length];
        System.arraycopy(prefix.getBytes(), 0, out, 0, prefix.getBytes().length);
        System.arraycopy(garbage, 0, out, prefix.getBytes().length, garbage.length);
        return out;
    }

    /**
     * SDKS-5549: a record that decrypts successfully (valid MAC and GCM tag) but whose
     * plaintext is malformed JSON must be discarded on read instead of throwing on every
     * access, which crash looped the app until app data was cleared.
     */
    @Test
    public void testCorruptedRecordIsDiscardedAndReturnsNull() {
        Encryptor encryptor = Encryptor.getEncryptor(context, TEST_ALIAS);
        // Warm up the keystore so TEST_ALIAS holds a symmetric key, then inject a blob
        // that decrypts cleanly but holds malformed JSON.
        encryptor.encrypt("warmup".getBytes());
        String blob = Base64.encodeToString(encryptor.encrypt(corruptedPayload()), Base64.DEFAULT);
        SharedPreferences raw = context.getSharedPreferences("test", Context.MODE_PRIVATE);
        raw.edit().clear().commit();
        raw.edit().putString("Test", blob).commit();

        assertNull(sharedPreferences.getString("Test", null));
        // The poisoned entry is gone from the underlying storage, later reads stay clean.
        assertNull(raw.getString("Test", null));
    }

    /**
     * SDKS-5549: an explicit clear() also restores a readable state after corruption,
     * validating the "clear app data" remediation.
     */
    @Test
    public void testClearRestoresReadableStateAfterCorruption() {
        Encryptor encryptor = Encryptor.getEncryptor(context, TEST_ALIAS);
        encryptor.encrypt("warmup".getBytes());
        String blob = Base64.encodeToString(encryptor.encrypt(corruptedPayload()), Base64.DEFAULT);
        SharedPreferences raw = context.getSharedPreferences("test", Context.MODE_PRIVATE);
        raw.edit().clear().commit();
        raw.edit().putString("Test", blob).commit();

        assertNull(sharedPreferences.getString("Test", null));
        sharedPreferences.edit().clear().commit();
        assertNull(sharedPreferences.getString("Test", null));
    }

    /**
     * SDKS-5549: a blob produced strictly per the 4.8.0 wire format (HmacSHA256(alias)[32]
     * + IV[12] + AES/GCM ciphertext for a keystore symmetric key) is still decrypted by the
     * current SDK, validating the upgrade path.
     */
    @Test
    public void test480WireFormatStillDecrypts() throws Exception {
        // Warm up the keystore so TEST_ALIAS holds a symmetric key.
        Encryptor.getEncryptor(context, TEST_ALIAS).encrypt("warmup".getBytes());

        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        javax.crypto.SecretKey keystoreKey =
                ((KeyStore.SecretKeyEntry) keyStore.getEntry(TEST_ALIAS, null)).getSecretKey();

        byte[] payload = "{\"type\":0,\"value\":\"session-token-value\"}".getBytes();
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keystoreKey);
        byte[] iv = cipher.getIV();
        byte[] ciphertext = cipher.doFinal(payload);
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(TEST_ALIAS.getBytes("UTF-8"), "HmacSHA256"));
        byte[] macBytes = mac.doFinal(ciphertext);

        byte[] blob480 = new byte[macBytes.length + iv.length + ciphertext.length];
        System.arraycopy(macBytes, 0, blob480, 0, macBytes.length);
        System.arraycopy(iv, 0, blob480, macBytes.length, iv.length);
        System.arraycopy(ciphertext, 0, blob480, macBytes.length + iv.length, ciphertext.length);

        SharedPreferences raw = context.getSharedPreferences("test", Context.MODE_PRIVATE);
        raw.edit().clear().commit();
        raw.edit().putString("Test", Base64.encodeToString(blob480, Base64.DEFAULT)).commit();

        assertEquals("session-token-value", sharedPreferences.getString("Test", null));
    }

}
