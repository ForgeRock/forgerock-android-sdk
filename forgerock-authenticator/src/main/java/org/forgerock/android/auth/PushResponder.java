/*
 * Copyright (c) 2020 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.forgerock.android.auth.exception.ChallengeResponseException;
import org.forgerock.android.auth.exception.PushMechanismException;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * This singleton is an utility used to reply to Push Notifications received from OpenAM.
 */
class PushResponder {

    private static PushResponder INSTANCE = null;

    /** OkHttp client to handle network requests **/
    private OkHttpClient httpClient;
    /** StorageClient to persist operation result **/
    private StorageClient storageClient;

    private static final String JWT_ALGORITHM = "HmacSHA256";
    private static final String RESPONSE_KEY = "response";
    private static final String DENY_KEY = "deny";
    private static final String CHALLENGE_RESPONSE_KEY = "challengeResponse";

    private static final int TIMEOUT = 30;

    private static final String TAG = PushResponder.class.getSimpleName();

    /**
     * Return the PushResponder instance
     *
     * @return PushResponder instance
     */
    static PushResponder getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("PushResponder is not initialized. " +
                    "Please make sure to call PushResponder#init first.");
        }
        return INSTANCE;
    }

    /**
     * Initialize/Return the PushResponder instance
     *
     * @return PushResponder instance
     */
    static PushResponder getInstance(@NonNull StorageClient storageClient) {
        synchronized (PushResponder.class) {
            if (INSTANCE == null) {
                INSTANCE = new PushResponder(storageClient);
            }
            return INSTANCE;
        }
    }

    /**
     * Private constructor restricted to this class itself
     */
    private PushResponder(StorageClient storageClient) {
        this.storageClient = storageClient;
    }

    /**
     * Used to generate a challenge response using the shared secret
     *
     * @param base64Secret The shared secret.
     * @param base64Challenge The challenge.
     * @return The challenge response of the request.
     */
    String generateChallengeResponse(String base64Secret, String base64Challenge) throws ChallengeResponseException {
        byte[] secret = Base64.decode(base64Secret, Base64.NO_WRAP);
        byte[] challenge;

        challenge = Base64.decode(base64Challenge, Base64.NO_WRAP);

        Mac hmac = null;
        try {
            SecretKey key = new SecretKeySpec(secret, 0, secret.length, JWT_ALGORITHM);
            hmac = Mac.getInstance(JWT_ALGORITHM);
            hmac.init(key);
        } catch (NoSuchAlgorithmException | IllegalArgumentException | InvalidKeyException e) {
            Logger.warn(TAG, e, "Failed to generate challenge-response for the secret" +
                    " using algorithm %s.", JWT_ALGORITHM);
            throw new ChallengeResponseException("Failed to generate challenge-response.", e);
        }
        byte[] output = hmac.doFinal(challenge);
        return Base64.encodeToString(output, Base64.NO_WRAP);
    }

    /**
     * Used to respond an authentication request from a given message
     *
     * @param pushNotification The push notification object.
     * @param challengeResponse The response to push challenge
     * @param listener Listener for receiving the HTTP call response code.
     */
    void authenticationWithChallenge(@NonNull PushNotification pushNotification,
                                     @NonNull String challengeResponse,
                                     final @NonNull FRAListener<Void> listener) {
        try {
            // Prepare payload
            Map<String, Object> payload = getAuthenticationPayload(pushNotification);
            if(pushNotification.getPushType() == PushType.CHALLENGE) {
                payload.put(CHALLENGE_RESPONSE_KEY, challengeResponse);
            }
            // Perform authentication
            performAuthentication(pushNotification, true, payload, listener);
        } catch (JOSEException | IllegalArgumentException | IOException | ChallengeResponseException | JSONException e) {
            listener.onException(new PushMechanismException("Error processing the Push " +
                    "Authentication request.\n Error Detail: \n" + e.getLocalizedMessage(), e));
        }
    }

    /**
     * Used to respond an authentication request from a given message
     *
     * @param pushNotification The push notification object.
     * @param approved The approval response
     * @param listener Listener for receiving the HTTP call response code.
     */
    void authentication(@NonNull PushNotification pushNotification,
                        boolean approved,
                        final @NonNull FRAListener<Void> listener) {
        try {
            // Prepare payload
            Map<String, Object> payload = getAuthenticationPayload(pushNotification);
            if(!approved) {
                payload.put(DENY_KEY, true);
            }
            // Perform authentication
            performAuthentication(pushNotification, approved, payload, listener);
        } catch (JOSEException | IllegalArgumentException | IOException | ChallengeResponseException | JSONException e) {
            listener.onException(new PushMechanismException("Error processing the Push " +
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
                      String messageId, Map<String, Object> payload, final FRAListener<Void> listener) {
        try {
            // Registration URL
            URL url = new URL(endpoint);

            // Build request
            OkHttpClient okHttpClient = getOkHttpClient(url);
            Request request = buildRequest(url,
                    amlbCookie,
                    base64Secret,
                    messageId,
                    payload,
                    Action.PUSH_REGISTER);

            // Invoke URL
            okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    Logger.debug(TAG, "Response from server: \n" + response.toString());
                    if(response.code() == 200) {
                        listener.onSuccess(null);
                    } else {
                        listener.onException(new PushMechanismException("Communication with " +
                                "server returned " + response.code() + " code."));
                    }
                    response.close();
                }

                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Logger.warn(TAG, "Failure on connecting to the server: \n" + call.request().toString());
                    listener.onException(new PushMechanismException("Network error while processing the Push " +
                            "Registration request.\n Error Detail: \n" + e.getLocalizedMessage(), e));
                }
            });
        } catch (JOSEException | IllegalArgumentException | IOException | JSONException e) {
            listener.onException(new PushMechanismException("Error processing the Push " +
                    "Registration request.\n Error Detail: \n" + e.getLocalizedMessage(), e));
        }
    }

    /**
     * Used to respond an authentication request from a given message
     *
     * @param pushNotification The push notification object.
     * @param approved The approval response
     * @param payload The payload
     * @param listener Listener for receiving the HTTP call response code.
     */
    private void performAuthentication(@NonNull PushNotification pushNotification,
                                       boolean approved,
                                       Map<String, Object> payload,
                                       final @NonNull FRAListener<Void> listener)
            throws MalformedURLException, JSONException, JOSEException {

        // Check if notification has been approved
        if(!pushNotification.isPending()) {
            listener.onException(new PushMechanismException("PushNotification is not in a" +
                    " valid status to authenticate; either PushNotification has already been" +
                    " authenticated or expired."));
            return;
        }

        // Get authentication endpoint
        String endpoint = pushNotification.getPushMechanism().getAuthenticationEndpoint();
        URL url = new URL(endpoint);

        // Build request
        OkHttpClient okHttpClient = getOkHttpClient(url);
        Request request = buildRequest(url,
                pushNotification.getAmlbCookie(),
                pushNotification.getPushMechanism().getSecret(),
                pushNotification.getMessageId(),
                payload,
                Action.PUSH_AUTHENTICATE);

        // Invoke URL
        okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                Logger.debug(TAG, "Response from server: \n" + response.toString());
                // Check if operation succeed
                if(response.code() == HTTP_OK) {
                    // Update notification status
                    pushNotification.setPending(false);
                    pushNotification.setApproved(approved);
                    // Persist status
                    if(!storageClient.setNotification(pushNotification)) {
                        listener.onException(new PushMechanismException("Push Authentication " +
                                "request was successfully processed, however it could not be persisted."));
                        return;
                    }
                    listener.onSuccess(null);
                } else {
                    listener.onException(new PushMechanismException("Communication with " +
                            "server returned " + response.code() + " code."));
                }
                response.close();
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Logger.warn(TAG, "Failure on connecting to the server: \n" + call.request().toString());
                listener.onException(new PushMechanismException("Network error while processing the Push " +
                        "Authentication request.\n Error Detail: \n" + e.getLocalizedMessage(), e));
            }
        });
    }

    /**
     * The `OkHttpClient` instance used to make the HTTP calls
     *
     * @return OkHttpClient http client
     * */
    private OkHttpClient getOkHttpClient(URL url) {
        if (httpClient != null) {
            return httpClient;
        }

        // Build network config. The host is required to build the NetworkConfig and used by
        // OkHttpClientProvider. There is no need to monitor the the change of hosts considering
        // the usage of this SDK.
        NetworkConfig networkConfig = NetworkConfig.networkBuilder()
                .timeout(TIMEOUT)
                .timeUnit(SECONDS)
                .host(url.getAuthority())
                .interceptorSupplier(() -> singletonList(new OkHttpRequestInterceptor()))
                .build();

        // Obtain instance of OkHttp client
        httpClient = OkHttpClientProvider.getInstance().lookup(networkConfig);

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
                                 String messageId, Map<String, Object> data, String action)
            throws IllegalArgumentException, JOSEException, JSONException {
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(url.toString());

        // Add header properties to the request
        requestBuilder.addHeader("Content-Type", "application/json");
        requestBuilder.addHeader("Accept-API-Version", "resource=1.0, protocol=1.0");
        if (amlbCookie != null) {
            requestBuilder.addHeader("Cookie", amlbCookie);
        }

        // Add body parameters to the request
        JSONObject message = new JSONObject();
        message.put("messageId", messageId);
        message.put("jwt", generateJwt(base64Secret, data));
        RequestBody body = RequestBody.create(message.toString(), MediaType.parse("application/json; charset=utf-8"));
        requestBuilder.post(body);
        requestBuilder.tag(new Action(action));

        return requestBuilder.build();
    }

    /**
     * Sign the payload with JWT
     */
    private static String generateJwt(String base64Secret, Map<String, Object> data)
            throws IllegalArgumentException, JOSEException {
        // Check shared secret
        if(base64Secret == null || base64Secret.length() == 0) {
            Logger.debug(TAG, "Error generating JWT data. Secret is empty or null.");
            throw new IllegalArgumentException("Passed empty secret");
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

        // Create HMAC signer
        byte[] secret = Base64.decode(base64Secret, Base64.NO_WRAP);
        JWSSigner signer = new MACSigner(secret);

        // Sign JWT
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    private Map<String, Object> getAuthenticationPayload(PushNotification pushNotification)
            throws ChallengeResponseException {

        Map<String, Object> payload = new HashMap<>();
        payload.put(RESPONSE_KEY, generateChallengeResponse(
                pushNotification.getPushMechanism().getSecret(),
                pushNotification.getChallenge()));

        return payload;
    }

    @VisibleForTesting
    static void reset() {
        INSTANCE = null;
    }

}
