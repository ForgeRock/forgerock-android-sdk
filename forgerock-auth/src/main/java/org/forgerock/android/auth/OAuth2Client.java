/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.net.Uri;
import android.util.Base64;
import lombok.Getter;
import lombok.NonNull;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class OAuth2Client {

    private static final String TAG = "OAuth2Client";
    private static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";

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
        this.okHttpClient = OkHttpClientProvider.getInstance().lookup(serverConfig);
    }

    /**
     * Sends an authorization request to the authorization service.
     *
     * @param token    The SSO Token received with the result of {@link AuthService}
     * @param listener Listener that listens to changes resulting from OAuth endpoints .
     */
    public void exchangeToken(@NonNull Token token, final FRListener<AccessToken> listener) {
        Logger.debug(TAG, "Exchanging Access Token with SSO Token.");
        final OAuth2ResponseHandler handler = new OAuth2ResponseHandler();
        try {
            FormBody.Builder builder = new FormBody.Builder();

            if (scope != null) {
                builder.add(OAuth2.SCOPE, scope);
            }

            final PKCE pkce = generateCodeChallenge();
            RequestBody body = builder.add(OAuth2.CLIENT_ID, clientId)
                    .add(OAuth2.CSRF, token.getValue())
                    .add(OAuth2.RESPONSE_TYPE, responseType)
                    .add(OAuth2.REDIRECT_URI, redirectUri)
                    .add(OAuth2.DECISION, "allow")
                    .add(OAuth2.CODE_CHALLENGE, pkce.getCodeChallenge())
                    .add(OAuth2.CODE_CHALLENGE_METHOD, pkce.getCodeChallengeMethod())
                    .build();

            Request request = new Request.Builder()
                    .url(getAuthorizeUrl(token))
                    .post(body)
                    .header(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
                    .header(ServerConfig.X_REQUESTED_WITH, ServerConfig.XML_HTTP_REQUEST)
                    .build();

            okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {

                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    listener.onException(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    handler.handleAuthorizeResponse(response, new FRListener<String>() {
                        @Override
                        public void onException(Exception e) {
                            listener.onException(e);
                        }

                        @Override
                        public void onSuccess(String code) {
                            token(code, pkce, handler, listener);
                        }
                    });
                }

            });

        } catch (IOException e) {
            listener.onException(e);
        }
    }

    public void refresh(@NonNull String refreshToken, final FRListener<AccessToken> listener) {
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

            Request request = new Request.Builder()
                    .url(getTokenUrl())
                    .post(body)
                    .header(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
                    .header(ServerConfig.X_REQUESTED_WITH, ServerConfig.XML_HTTP_REQUEST)
                    .build();


            okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {

                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    listener.onException(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    handler.handleTokenResponse(response, listener);
                }
            });

        } catch (IOException e) {
            listener.onException(e);
        }
    }

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

            Request request = new Request.Builder()
                    .url(getRevokeUrl())
                    .post(body)
                    .header(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
                    .header(ServerConfig.X_REQUESTED_WITH, ServerConfig.XML_HTTP_REQUEST)
                    .build();


            okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {

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
     * Sends an token request to the authorization service.
     *
     * @param code    The Authorization code.
     * @param pkce    The Proof Key for Code Exchange
     * @param handler Handle changes resulting from OAuth endpoints.
     */
    private void token(@NonNull String code, final PKCE pkce, final OAuth2ResponseHandler handler, final FRListener<AccessToken> listener) {
        Logger.debug(TAG, "Exchange Access Token with Authorization Code");
        try {
            FormBody.Builder builder = new FormBody.Builder();

            RequestBody body = builder
                    .add(OAuth2.CLIENT_ID, clientId)
                    .add(OAuth2.CODE, code)
                    .add(OAuth2.REDIRECT_URI, redirectUri)
                    .add(OAuth2.GRANT_TYPE, OAuth2.AUTHORIZATION_CODE)
                    .add(OAuth2.CODE_VERIFIER, pkce.getCodeVerifier())
                    .build();

            Request request = new Request.Builder()
                    .url(getTokenUrl())
                    .post(body)
                    .header(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
                    .header(ServerConfig.X_REQUESTED_WITH, ServerConfig.XML_HTTP_REQUEST)
                    .build();

            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    listener.onException(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    handler.handleTokenResponse(response, listener);
                }

            });

        } catch (IOException e) {
            listener.onException(e);
        }
    }


    private URL getAuthorizeUrl(Token token) throws MalformedURLException {
        return new URL(Uri.parse(serverConfig.getUrl())
                .buildUpon()
                .appendPath("oauth2")
                .appendPath("realms")
                .appendPath(serverConfig.getRealm())
                .appendPath("authorize")
                .appendQueryParameter(SSOToken.IPLANET_DIRECTORY_PRO, token.getValue())
                .build().toString());
    }

    private URL getTokenUrl() throws MalformedURLException {
        return new URL(Uri.parse(serverConfig.getUrl())
                .buildUpon()
                .appendPath("oauth2")
                .appendPath("realms")
                .appendPath(serverConfig.getRealm())
                .appendPath("access_token")
                .build().toString());
    }

    private URL getRevokeUrl() throws MalformedURLException {
        return new URL(Uri.parse(serverConfig.getUrl())
                .buildUpon()
                .appendPath("oauth2")
                .appendPath("realms")
                .appendPath(serverConfig.getRealm())
                .appendPath("token")
                .appendPath("revoke")
                .build().toString());
    }


    private PKCE generateCodeChallenge() throws UnsupportedEncodingException {
        int encodeFlags = Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE;
        byte[] randomBytes = new byte[64];
        new SecureRandom().nextBytes(randomBytes);
        String codeVerifier = Base64.encodeToString(randomBytes, encodeFlags);
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(codeVerifier.getBytes("ISO_8859_1"));
            byte[] digestBytes = messageDigest.digest();
            return new PKCE(Base64.encodeToString(digestBytes, encodeFlags), "S256", codeVerifier);
        } catch (NoSuchAlgorithmException e) {
            return new PKCE("plain", codeVerifier, codeVerifier);
        }
    }
}
