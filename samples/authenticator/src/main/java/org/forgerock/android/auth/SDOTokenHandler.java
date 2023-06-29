/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class is responsible to handle SDO Token operations.
 */
public class SDOTokenHandler {

    private static final String TAG = SDOTokenHandler.class.getSimpleName();

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    private final SDOSecureStorage sdoSecureStorage;

    /**
     * The Constructor.
     * @param context Application Context.
     */
    public SDOTokenHandler(Context context) {
        sdoSecureStorage = new SDOSecureStorage(context);
    }

    /**
     * Process the sdoToken sent via PushNotification payload and securely store it.
     * @param notification the PushNotification object.
     * @param mechanism the PushMechanism associated with the notification.
     */
    public void processSdoTokenFromPushNotification(PushNotification notification, Mechanism mechanism) {
        String encryptedToken = null;
        String customPayload = notification.getCustomPayload();
        if(customPayload != null && !customPayload.isEmpty()) {
            try {
                JSONObject payload = new JSONObject(customPayload);
                if(payload.has("sdoToken")) {
                    Logger.debug(TAG, "SDO token found in the notification payload");
                    encryptedToken = payload.getString("sdoToken");

                    Logger.debug(TAG, "Decrypting SDO token...");
                    String sharedSecret = mechanism.getSecret();
                    String sdoToken = decryptToken(encryptedToken, sharedSecret);

                    Logger.debug(TAG, "Saving DO Token: %s", sdoToken);
                    sdoSecureStorage.setToken(sdoToken);
                } else {
                    Logger.debug(TAG, "No SDO token found in the notification payload");
                }
            } catch (JSONException e) {
                Logger.warn(TAG, e, "Error parsing notification payload: %s", customPayload);
            } catch (NoSuchPaddingException | NoSuchAlgorithmException |
                     InvalidAlgorithmParameterException | InvalidKeyException |
                     IllegalBlockSizeException | BadPaddingException e) {
                Logger.warn(TAG, e, "Error decrypting SDO token found in notification payload: %s",
                        encryptedToken);
            }
        } else {
            Logger.debug(TAG, "No custom payload in the notification. Skipping SDO Token processing.");
        }
    }

    /**
     * Retrieves the SDO token.
     * @return The SDO token as string. Returns {null} if no token is available.
     */
    public String getToken() {
        return sdoSecureStorage.getToken();
    }

    private String decryptToken(String encryptedToken, String sharedSecret)
            throws BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException {

        String[] encryptedPair = encryptedToken.split("\\.");
        byte[] byteIv = encryptedPair[0].getBytes();
        String cipherText = encryptedPair[1];

        byte[] byteKey = Base64.decode(sharedSecret, Base64.DEFAULT);

        SecretKey secretKey = new SecretKeySpec(byteKey, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(byteIv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        byte[] plainText = cipher.doFinal(Base64.decode(cipherText, Base64.DEFAULT));

        Logger.debug(TAG, "SDO token successfully decrypted using the sharedSecret");
        return new String(plainText);
    }

}