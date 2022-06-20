/*
 * Copyright (c) 2019 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.net.Uri;
import android.util.Base64;

import androidx.annotation.Nullable;

import org.forgerock.android.auth.exception.ApiException;
import org.forgerock.android.auth.exception.AuthorizeException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

import static org.forgerock.android.auth.ServerConfig.ACCEPT_API_VERSION;
import static org.forgerock.android.auth.StringUtils.isNotEmpty;

/**
 * Class to handle OAuth2 related endpoint
 */
@Getter
public class OAuth2Client {

    private static final String TAG = "OAuth2Client";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    private static final Action AUTHORIZE = new Action(Action.AUTHORIZE);
    private static final Action EXCHANGE_TOKEN = new Action(Action.EXCHANGE_TOKEN);
    private static final Action REFRESH_TOKEN = new Action(Action.REFRESH_TOKEN);
    private static final Action REVOKE_TOKEN = new Action(Action.REVOKE_TOKEN);
    private static final Action END_SESSION = new Action(Action.END_SESSION);

    /**
     * The registered client identifier
     */
    private String clientId;

    private String scope;
    private String redirectUri;
    private String responseType = OAuth2.CODE;

    @Getter
    private ServerConfig serverConfig;
    private OkHttpClient okHttpClient;

    @lombok.Builder
    public OAuth2Client(
            @NonNull String clientId,
            @NonNull String scope,
            @NonNull String redirectUri,
            @NonNull ServerConfig serverConfig) {

        this.clientId = clientId;
        this.scope = scope;
        this.redirectUri = redirectUri;
        this.serverConfig = serverConfig;
    }

    /**
     * Sends an authorization request to the authorization service.
     *
     * @param token                The SSO Token received with the result of {@link AuthService}
     * @param additionalParameters Additional parameters for inclusion in the authorization endpoint
     *                             request
     * @param listener             Listener that listens to changes resulting from OAuth endpoints .
     */
    public void exchangeToken(@NonNull SSOToken token,
                              @NonNull Map<String, String> additionalParameters,
                              final FRListener<AccessToken> listener) {
        Logger.debug(TAG, "Exchanging Access Token with SSO Token.");
        final OAuth2ResponseHandler handler = new OAuth2ResponseHandler();
        try {
            FormBody.Builder builder = new FormBody.Builder();

            if (scope != null) {
                builder.add(OAuth2.SCOPE, scope);
            }

            final PKCE pkce = generateCodeChallenge();

            Logger.debug(TAG, "Exchanging Authorization Code with SSO Token.");
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(getAuthorizeUrl(token, pkce, additionalParameters))
                    .get()
                    .header(ACCEPT_API_VERSION, ServerConfig.API_VERSION_2_1)
                    .header(serverConfig.getCookieName(), token.getValue() )
                    .tag(AUTHORIZE)
                    .build();

            getOkHttpClient().newCall(request).enqueue(new okhttp3.Callback() {

                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Logger.debug(TAG, "Failed to exchange for Authorization Code: %s", e.getMessage());
                    listener.onException(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    handler.handleAuthorizeResponse(response, new FRListener<String>() {
                        @Override
                        public void onException(Exception e) {
                            Logger.debug(TAG, "Failed to exchange for Authorization Code: %s", e.getMessage());
                            listener.onException(new AuthorizeException("Failed to exchange authorization code with sso token", e));
                        }

                        @Override
                        public void onSuccess(String code) {
                            Logger.debug(TAG, "Authorization Code received.");
                            token(token, code, pkce, additionalParameters, handler, listener);
                        }
                    });
                }

            });

        } catch (IOException e) {
            listener.onException(e);
        }
    }

    /**
     * Refresh the Access Token with the provided Refresh Token
     *
     * @param sessionToken The Session Token that bind to existing AccessToken
     * @param refreshToken The Refresh Token that use to refresh the Access Token
     * @param listener     Listen for endpoint event
     */
    public void refresh(@Nullable SSOToken sessionToken, @NonNull String refreshToken, final FRListener<AccessToken> listener) {
        Logger.debug(TAG, "Refreshing Access Token");

        final OAuth2ResponseHandler handler = new OAuth2ResponseHandler();
        try {
            FormBody.Builder builder = new FormBody.Builder();

            if (scope != null) {
                builder.add(OAuth2.SCOPE, scope);
            }

            RequestBody body = builder.add(OAuth2.CLIENT_ID, clientId)
                    .add(OAuth2.GRANT_TYPE, OAuth2.REFRESH_TOKEN)
                    .add(OAuth2.RESPONSE_TYPE, responseType)
                    .add(OAuth2.REFRESH_TOKEN, refreshToken)
                    .build();

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(getTokenUrl())
                    .post(body)
                    .header(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
                    .header(ACCEPT_API_VERSION, ServerConfig.API_VERSION_2_1)
                    .tag(REFRESH_TOKEN)
                    .build();


            getOkHttpClient().newCall(request).enqueue(new okhttp3.Callback() {

                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    listener.onException(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    handler.handleTokenResponse(sessionToken, response, refreshToken, listener);
                }
            });

        } catch (IOException e) {
            listener.onException(e);
        }
    }

    /**
     * Revoke the AccessToken, to revoke the access token, first look for refresh token to revoke, if
     * not provided, will revoke with the access token.
     *
     * @param accessToken The AccessToken to be revoked
     * @param listener    Listener to listen for revoke event
     */
    public void revoke(@NonNull AccessToken accessToken, final FRListener<Void> listener) {
        Logger.debug(TAG, "Revoking Access Token & Refresh Token");
        final OAuth2ResponseHandler handler = new OAuth2ResponseHandler();
        try {
            FormBody.Builder builder = new FormBody.Builder();

            String token = accessToken.getRefreshToken() == null ? accessToken.getValue() : accessToken.getRefreshToken();

            RequestBody body = builder
                    .add(OAuth2.CLIENT_ID, clientId)
                    .add(OAuth2.TOKEN, token)
                    .build();

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(getRevokeUrl())
                    .post(body)
                    .header(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
                    .header(ACCEPT_API_VERSION, ServerConfig.API_VERSION_2_1)
                    .tag(REVOKE_TOKEN)
                    .build();


            getOkHttpClient().newCall(request).enqueue(new okhttp3.Callback() {

                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Listener.onException(listener, e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    handler.handleRevokeResponse(response, listener);
                }
            });

        } catch (IOException e) {
            Listener.onException(listener, e);
        }
    }

    /**
     * End the user session with end session endpoint.
     *
     * @param idToken  The ID_TOKEN which associated with the user session.
     * @param listener Listener to listen for end session event.
     */
    public void endSession(@NonNull String idToken, FRListener<Void> listener) {

        okhttp3.Request request = null;
        try {
            request = new okhttp3.Request.Builder()
                    .url(getEndSessionUrl(clientId, idToken))
                    .get()
                    .tag(END_SESSION)
                    .build();
        } catch (MalformedURLException e) {
            Listener.onException(listener, e);
            return;
        }

        final OAuth2ResponseHandler handler = new OAuth2ResponseHandler();
        Logger.debug(TAG, "End session with id token");
        getOkHttpClient().newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Logger.debug(TAG, "Revoke session with id token failed: %s", e.getMessage());
                Listener.onException(listener, e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                handler.handleRevokeResponse(response, listener);
            }
        });
    }

    private OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            okHttpClient = OkHttpClientProvider.getInstance().lookup(serverConfig);
        }
        return okHttpClient;
    }

    /**
     * Sends an token request to the authorization service.
     *
     * @param sessionToken         The Session Token
     * @param code                 The Authorization code.
     * @param pkce                 The Proof Key for Code Exchange
     * @param additionalParameters Additional parameters for inclusion in the token endpoint
     *                             request
     * @param handler              Handle changes resulting from OAuth endpoints.
     */
    public void token(@Nullable SSOToken sessionToken,
                      @NonNull String code,
                      final PKCE pkce,
                      final Map<String, String> additionalParameters,
                      final OAuth2ResponseHandler handler,
                      final FRListener<AccessToken> listener) {
        Logger.debug(TAG, "Exchange Access Token with Authorization Code");
        try {
            FormBody.Builder builder = new FormBody.Builder();

            for (Map.Entry<String, String> entry : additionalParameters.entrySet()) {
                builder.add(entry.getKey(), entry.getValue());
            }

            RequestBody body = builder
                    .add(OAuth2.CLIENT_ID, clientId)
                    .add(OAuth2.CODE, code)
                    .add(OAuth2.REDIRECT_URI, redirectUri)
                    .add(OAuth2.GRANT_TYPE, OAuth2.AUTHORIZATION_CODE)
                    .add(OAuth2.CODE_VERIFIER, pkce.getCodeVerifier())
                    .build();

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(getTokenUrl())
                    .post(body)
                    .header(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
                    .header(ACCEPT_API_VERSION, ServerConfig.API_VERSION_2_1)
                    .tag(EXCHANGE_TOKEN)
                    .build();

            getOkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Logger.debug(TAG, "Exchange Access Token with Authorization Code failed: %s", e.getMessage());
                    listener.onException(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    handler.handleTokenResponse(sessionToken, response, null, listener);
                }

            });

        } catch (IOException e) {
            listener.onException(e);
        }
    }

    private URL getAuthorizeUrl(Token token, PKCE pkce, Map<String, String> additionalParameters) throws MalformedURLException, UnsupportedEncodingException {
        Uri.Builder builder = Uri.parse(getAuthorizeUrl().toString()).buildUpon();
        for (Map.Entry<String, String> entry : additionalParameters.entrySet()) {
            builder.appendQueryParameter(entry.getKey(), entry.getValue());
        }
        return new URL(builder
                .appendQueryParameter(OAuth2.CLIENT_ID, clientId)
                .appendQueryParameter(OAuth2.SCOPE, scope)
                .appendQueryParameter(OAuth2.RESPONSE_TYPE, responseType)
                .appendQueryParameter(OAuth2.REDIRECT_URI, redirectUri)
                .appendQueryParameter(OAuth2.CODE_CHALLENGE, pkce.getCodeChallenge())
                .appendQueryParameter(OAuth2.CODE_CHALLENGE_METHOD, pkce.getCodeChallengeMethod())
                .build().toString());
    }

    URL getAuthorizeUrl() throws MalformedURLException {
        Uri.Builder builder = Uri.parse(serverConfig.getUrl()).buildUpon();
        if (isNotEmpty(serverConfig.getAuthorizeEndpoint())) {
            builder.appendEncodedPath(serverConfig.getAuthorizeEndpoint());
        } else {
            builder.appendPath("oauth2")
                    .appendPath("realms")
                    .appendPath(serverConfig.getRealm())
                    .appendPath("authorize");
        }
        return new URL(builder.build().toString());
    }

    URL getTokenUrl() throws MalformedURLException {
        Uri.Builder builder = Uri.parse(serverConfig.getUrl()).buildUpon();
        if (isNotEmpty(serverConfig.getTokenEndpoint())) {
            builder.appendEncodedPath(serverConfig.getTokenEndpoint());
        } else {
            builder.appendPath("oauth2")
                    .appendPath("realms")
                    .appendPath(serverConfig.getRealm())
                    .appendPath("access_token");
        }
        return new URL(builder.build().toString());
    }

    URL getRevokeUrl() throws MalformedURLException {

        Uri.Builder builder = Uri.parse(serverConfig.getUrl()).buildUpon();
        if (isNotEmpty(serverConfig.getRevokeEndpoint())) {
            builder.appendEncodedPath(serverConfig.getRevokeEndpoint());
        } else {
            builder.appendPath("oauth2")
                    .appendPath("realms")
                    .appendPath(serverConfig.getRealm())
                    .appendPath("token")
                    .appendPath("revoke");
        }
        return new URL(builder.build().toString());
    }

    URL getEndSessionUrl(String clientId, String idToken) throws MalformedURLException {

        Uri.Builder builder = Uri.parse(serverConfig.getUrl()).buildUpon();
        if (isNotEmpty(serverConfig.getEndSessionEndpoint())) {
            builder.appendEncodedPath(serverConfig.getEndSessionEndpoint());
        } else {
            builder.appendPath("oauth2")
                    .appendPath("realms")
                    .appendPath(serverConfig.getRealm())
                    .appendPath("connect")
                    .appendPath("endSession");
        }
        builder.appendQueryParameter("id_token_hint", idToken);
        builder.appendQueryParameter("client_id", clientId);
        return new URL(builder.build().toString());
    }


    private PKCE generateCodeChallenge() throws UnsupportedEncodingException {
        int encodeFlags = Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE;
        byte[] randomBytes = new byte[64];
        new SecureRandom().nextBytes(randomBytes);
        String codeVerifier = Base64.encodeToString(randomBytes, encodeFlags);
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(codeVerifier.getBytes(StandardCharsets.ISO_8859_1));
            byte[] digestBytes = messageDigest.digest();
            return new PKCE(Base64.encodeToString(digestBytes, encodeFlags), "S256", codeVerifier);
        } catch (NoSuchAlgorithmException e) {
            return new PKCE("plain", codeVerifier, codeVerifier);
        }
    }

}
