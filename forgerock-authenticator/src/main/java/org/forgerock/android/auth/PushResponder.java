/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

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

import org.forgerock.android.auth.exception.PushAuthenticationException;
import org.forgerock.android.auth.exception.PushRegistrationException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * This singleton is an utility used to reply to Push Notifications received from OpenAM.
 */
class PushResponder {

    private static final PushResponder INSTANCE = new PushResponder();

    /** OkHttp client **/
    private OkHttpClient httpClient;

    private static final String JWT_ALGORITHM = "HmacSHA256";
    private static final String RESPONSE_KEY = "response";
    private static final String DENY_KEY = "deny";

    private static final int TIMEOUT = 30;

    private static final String TAG = PushResponder.class.getSimpleName();

    /**
     * Return the PushResponder instance
     *
     * @return PushResponder instance
     */
    public static PushResponder getInstance() {
        return INSTANCE;
    }

    /**
     * Private constructor restricted to this class itself
     */
    private PushResponder() {
    }

    /**
     * Used to generate a challenge response using the shared secret
     *
     * @param base64Secret The shared secret.
     * @param base64Challenge The challenge.
     * @return The challenge response of the request.
     */
    static String generateChallengeResponse(String base64Secret, String base64Challenge) {
        byte[] secret = Base64.decode(base64Secret, Base64.NO_WRAP);
        byte[] challenge;

        challenge = Base64.decode(base64Challenge, Base64.NO_WRAP);

        Mac hmac = null;
        SecretKey key = new SecretKeySpec(secret, 0, secret.length, JWT_ALGORITHM);
        try {
            hmac = Mac.getInstance(JWT_ALGORITHM);
            hmac.init(key);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            Logger.warn(TAG, e, "Failed to generate challenge-response.");
        }
        byte[] output = hmac.doFinal(challenge);
        return Base64.encodeToString(output, Base64.NO_WRAP);
    }

    /**
     * Used to respond an authentication request from a given message
     *
     * @param pushNotification The push notification object.
     * @param approved The approval response
     * @param listener Listener for receiving the HTTP call response code.
     */
    void authentication(PushNotification pushNotification, boolean approved,
                        final FRAListener<Integer> listener) {
        try {
            // Check if notification has been approved
            if(!pushNotification.isPending()) {
                listener.onException(new PushAuthenticationException("PushNotification is not in a" +
                        " valid status to authenticate; either PushNotification has already been" +
                        " authenticated or expired."));
                return;
            }

            // Get authentication endpoint
            String endpoint = pushNotification.getPushMechanism().getAuthenticationEndpoint();
            URL url = new URL(endpoint);;

            // Prepare payload
            Map<String, Object> payload = new HashMap<>();
            payload.put(RESPONSE_KEY, generateChallengeResponse(
                    pushNotification.getPushMechanism().getSecret(),
                    pushNotification.getChallenge()));
            if(!approved)
                payload.put(DENY_KEY, true);

            // Build request
            OkHttpClient okHttpClient = getOkHttpClient();
            Request request = buildRequest(url,
                    pushNotification.getAmlbCookie(),
                    pushNotification.getPushMechanism().getSecret(),
                    pushNotification.getMessageId(),
                    payload);

            // Invoke URL
            okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    // Check if operation succeed
                    if(response.code() == 200) {
                        pushNotification.setPending(false);
                        if(approved)
                            pushNotification.setApproved(true);
                        else
                            pushNotification.setApproved(false);
                    }
                    if (listener != null) {
                        listener.onSuccess(response.code());
                    }
                }

                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    listener.onException(new PushAuthenticationException("Network error while processing the Push " +
                            "Authentication request.\n Error Detail: \n" + e.getLocalizedMessage(), e));
                }
            });
        } catch (JOSEException | IOException e) {
            listener.onException(new PushAuthenticationException("Error processing the Push " +
                    "Authentication request.\n Error Detail: \n" + e.getLocalizedMessage(), e));
        }
    }

    /**
     * Used to respond to a Push registration for a given endpoint
     *
     * @param endpoint The endpoint to respond to.
     * @param amlbCookie The AM load balance cookie of the endpoint.
     * @param base64Secret The secret of the push mechanism.
     * @param messageId The id of the message being responded to.
     * @param payload The data to attach to the response.
     * @param listener Listener for receiving the HTTP call response code.
     */
    void registration(String endpoint, String amlbCookie, String base64Secret,
                      String messageId, Map<String, Object> payload, final FRAListener<Integer> listener) {
        try {
            // Registration URL
            URL url = new URL(endpoint);

            // Build request
            OkHttpClient okHttpClient = getOkHttpClient();
            Request request = buildRequest(url,
                    amlbCookie,
                    base64Secret,
                    messageId,
                    payload);

            // Invoke URL
            okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    listener.onSuccess(response.code());
                }

                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    listener.onException(new PushRegistrationException("Network error while processing the Push " +
                            "Registration request.\n Error Detail: \n" + e.getLocalizedMessage(), e));
                }
            });
        } catch (JOSEException | IOException e) {
            listener.onException(new PushRegistrationException("Error processing the Push " +
                    "Registration request.\n Error Detail: \n" + e.getLocalizedMessage(), e));
        }
    }

    /**
     * The `OkHttpClient` instance used to make the HTTP calls
     *
     * @return OkHttpClient http client
     * */
    private OkHttpClient getOkHttpClient() {
        if (httpClient != null) {
            return httpClient;
        }

        httpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT, SECONDS)
                .readTimeout(TIMEOUT, SECONDS)
                .writeTimeout(TIMEOUT, SECONDS)
                .followRedirects(false)
                .build();

        return httpClient;
    }

    /**
     * Build a request
     *
     * @param url   the endpoint url
     * @param amlbCookie the AM load balance cookie
     * @return the request object
     */
    private Request buildRequest(URL url, String amlbCookie, String base64Secret,
                                 String messageId, Map<String, Object> data)
            throws IOException, JOSEException {
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(url);

        // Add header properties to the request
        requestBuilder.addHeader("Content-Type", "application/json");
        requestBuilder.addHeader("Accept-API-Version", "resource=1.0, protocol=1.0");
        if (amlbCookie != null) {
            requestBuilder.addHeader("Cookie", amlbCookie);
        }

        // Add body parameters to the request
        RequestBody body = new FormBody.Builder()
                .add("messageId", messageId)
                .add("jwt", generateJwt(base64Secret, data))
                .build();
        requestBuilder.post(body);

        return requestBuilder.build();
    }

    /**
     * Sign the payload with JWT
     */
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
