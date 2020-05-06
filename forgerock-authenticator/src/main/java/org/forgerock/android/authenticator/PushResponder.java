/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

import android.util.Base64;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.forgerock.android.auth.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * This is an utility used to reply to Push Notifications received from OpenAM.
 */
class PushResponder {

    private static final String TAG = PushResponder.class.getSimpleName();

    /**
     * Used to generate a challenge response using the shared secret.
     * @param base64Secret The shared secret.
     * @param base64Challenge The challenge.
     * @return The challenge response of the request.
     */
    public static String generateChallengeResponse(String base64Secret, String base64Challenge) {
        byte[] secret = Base64.decode(base64Secret, Base64.NO_WRAP);
        byte[] challenge;

        challenge = Base64.decode(base64Challenge, Base64.NO_WRAP);

        Mac hmac = null;
        SecretKey key = new SecretKeySpec(secret, 0, secret.length, "HmacSHA256");
        try {
            hmac = Mac.getInstance("HmacSHA256");
            hmac.init(key);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            Logger.warn(TAG, e, "Failed to generate challenge-response.");
        }
        byte[] output = hmac.doFinal(challenge);
        return Base64.encodeToString(output, Base64.NO_WRAP);
    }

    /**
     * Used to respond to a given message ID at a given endpoint.
     * @param endpoint The endpoint to respond to.
     * @param messageId The id of the message being responded to.
     * @param data The data to attach to the response.
     * @return The response code of the request.
     * @throws IOException If a network issue occurred.
     * @throws JSONException If an encoding issue occurred.
     */
    public static int respond(String endpoint, String amlbCookie, String base64Secret,
                              String messageId, Map<String, Object> data)
            throws IOException, JSONException, JOSEException {
        HttpURLConnection connection = null;
        int returnCode = 404;
        try {
            // Establish connection with the endpoint
            connection = openUrlConnection(endpoint, amlbCookie);
            if(connection != null)
                connection.connect();

            // Write response and get result
            JSONObject message = new JSONObject();
            message.put("messageId", messageId);
            message.put("jwt", generateJwt(base64Secret, data));

            OutputStream os = connection.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
            osw.write(message.toString());
            osw.flush();
            osw.close();
            returnCode = connection.getResponseCode();
        } finally {
            if(connection != null) {
                connection.disconnect();
            }
        }
        return returnCode;
    }

    private static HttpURLConnection openUrlConnection(String endpoint, String amlbCookie)
            throws IOException {
        HttpURLConnection connection = null;
        URL url = null;

        try {
            url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();

            // Set request parameters and establish connection
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept-API-Version", "resource=1.0, protocol=1.0");
            if (amlbCookie != null) {
                connection.setRequestProperty("Cookie", amlbCookie);
            }
        } catch (MalformedURLException e) {
            Logger.warn(TAG, e, "Error establishing connection. URL malformed or invalid.");
            throw new IOException("Invalid URL!", e);
        } catch (ProtocolException e) {
            Logger.warn(TAG, e,"Error establishing connection. POST requested method " +
                    "isn't valid for the endpoint.");
            throw new IOException("Invalid URL!", e);
        }

        return connection;
    }

    private static String generateJwt(String base64Secret, Map<String, Object> data)
            throws IOException, JOSEException {

        // Check shared secret
        if(base64Secret == null || base64Secret.length() == 0) {
            Logger.debug(TAG, "Error generating JWT data. Secret is empty or null.");
            throw new IOException("Passed empty secret");
        }

        // Prepare JWT with claims
        JWTClaimsSet.Builder claimBuilder = new JWTClaimsSet.Builder();
        for (String key : data.keySet()) {
            claimBuilder.claim(key, data.get(key));
        }

        // Apply the HMAC protection
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                .type(JOSEObjectType.JWT)
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claimBuilder.build());

        try {
            // Create HMAC signer
            byte[] secret = Base64.decode(base64Secret, Base64.NO_WRAP);
            JWSSigner signer = new MACSigner(secret);

            // Sign JWT
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (IllegalArgumentException | KeyLengthException e) {
            Logger.debug(TAG, "Error generating JWT data. Secret malformed or invalid.");
            throw new IOException("Invalid secret!", e);
        } catch (JOSEException e) {
            Logger.warn(TAG, e, "Failed to sign the data.");
            throw new JOSEException("Failed to sign JWT", e);
        }
    }

}
