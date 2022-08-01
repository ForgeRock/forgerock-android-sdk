package org.forgerock.android.auth.callback;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import androidx.annotation.RequiresApi;

import org.forgerock.android.auth.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.UUID;

public interface KeyAware {

    String ANDROID_KEYSTORE = "AndroidKeyStore";
    String DEVICE_BINDING = "DeviceBinding";

    String getUserId();

    default String getKeyAlias() {
        return getHash(getUserId());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    default String generateKeys(Context context) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, JSONException {
        String userIdHash = getHash(getUserId());
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE);
        keyPairGenerator.initialize(
                new KeyGenParameterSpec.Builder(
                        userIdHash,
                        KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setDigests(KeyProperties.DIGEST_SHA512)
                        .setUserAuthenticationRequired(true)
                        .setUserAuthenticationValidityDurationSeconds(60)
                        .setKeySize(2048)
                        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                        .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                        .build());
        keyPairGenerator.generateKeyPair();
        SharedPreferences sharedPreferences = context.getSharedPreferences(DEVICE_BINDING, Context.MODE_PRIVATE);
        String binding = sharedPreferences.getString(getHash(getUserId()), null);
        if (binding == null) {
            JSONObject jsonObject = new JSONObject();
            String uuid = UUID.randomUUID().toString();
            try {
                jsonObject.put("userId", getUserId());
                jsonObject.put("kid", uuid);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            sharedPreferences.edit().putString(getHash(getUserId()), jsonObject.toString()).apply();
        }
        return userIdHash;
    }

    default JSONObject getBindingInfo(Context context) throws JSONException {
        String userIdHash = getHash(getUserId());
        String info = context.getSharedPreferences(DEVICE_BINDING, Context.MODE_PRIVATE).getString(userIdHash, null);
        if (info != null) {
            return new JSONObject(info);
        }
        throw new IllegalStateException("Binding Info Not Found");
    }



    default KeyStore getKeyStore()
            throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        return keyStore;
    }

    default Key getKey(String keyAlias) {
        try {
            KeyStore keyStore = getKeyStore();
            return keyStore.getKey(keyAlias, null);
        } catch (Exception e) {
            return null;
        }
    }

    default String getHash(String value) {
        byte[] hash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(hash, Base64.DEFAULT);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


}
