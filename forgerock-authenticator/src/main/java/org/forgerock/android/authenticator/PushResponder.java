/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

import org.forgerock.android.auth.Logger;
import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.builders.SignedJwtBuilderImpl;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.util.encode.Base64;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
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

    public static String generateChallengeResponse(String base64Secret, String base64Challenge) {
        byte[] secret = Base64.decode(base64Secret);
        byte[] challenge;

        challenge = Base64.decode(base64Challenge);

        Mac hmac = null;
        SecretKey key = new SecretKeySpec(secret, 0, secret.length, "HmacSHA256");
        try {
            hmac = Mac.getInstance("HmacSHA256");
            hmac.init(key);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            Logger.warn(TAG, e, "Failed to generate challenge-response.");
        }
        byte[] output = hmac.doFinal(challenge);
        return Base64.encode(output);
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
            throws IOException, JSONException {
        HttpURLConnection connection = null;
        int returnCode = 404;
        try {
            URL url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept-API-Version", "resource=1.0, protocol=1.0");
            if (amlbCookie != null) {
                connection.setRequestProperty("Cookie", amlbCookie);
            }
            connection.connect();

            JSONObject message = new JSONObject();
            message.put("messageId", messageId);
            message.put("jwt", generateJwt(base64Secret, data));

            OutputStream os = connection.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
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

    private static String generateJwt(String base64Secret, Map<String, Object> data) throws IOException {
        JwtClaimsSetBuilder builder = new JwtClaimsSetBuilder();
        for (String key : data.keySet()) {
            builder.claim(key, data.get(key));
        }

        byte[] secret = Base64.decode(base64Secret);

        SigningHandler signingHandler = new SigningManager().newHmacSigningHandler(secret);
        SignedJwtBuilderImpl jwtBuilder = new SignedJwtBuilderImpl(signingHandler);
        jwtBuilder.claims(builder.build());
        jwtBuilder.headers().alg(JwsAlgorithm.HS256);
        try {
            return jwtBuilder.build();
        } catch (IllegalArgumentException e) {
            throw new IOException("Passed empty secret", e);
        }
    }

}
