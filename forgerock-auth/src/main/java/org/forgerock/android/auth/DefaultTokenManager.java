/*
 * Copyright (c) 2019 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.forgerock.android.auth.StringUtils.isNotEmpty;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import net.openid.appauth.AppAuthConfiguration;

import org.forgerock.android.auth.exception.ApiException;
import org.forgerock.android.auth.exception.AuthenticationRequiredException;
import org.forgerock.android.auth.exception.InvalidGrantException;
import org.forgerock.android.auth.storage.Storage;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Builder;

/**
 * Default implementation for {@link TokenManager}. By default this class uses {@link SecuredSharedPreferences} to persist
 * the token locally on the device. However, it can be overridden by the {@link DefaultTokenManager#builder#storage}
 * <p>
 * <p>
 * This {@link TokenManager} supports {@link AccessToken} in-memory caching. To control the caching interval use
 * {@link DefaultTokenManager#builder#cacheIntervalMillis}. with the encryption/decryption, disk and Keystore IO, it may drains
 * device CPU and Battery resources. If Application intensively with network operation, setting the
 * this attribute may improve performance.
 */
class DefaultTokenManager implements TokenManager {

    private static final String TAG = "DefaultTokenManager";

    /**
     * The {@link SharedPreferences} to store the tokens
     */
    private final Storage<AccessToken> storage;

    /**
     * The {@link OAuth2Client} to auto refresh {@link AccessToken}
     */
    private final OAuth2Client oAuth2Client;
    private final AtomicReference<CacheEntry<AccessToken>> accessTokenRef;
    private static final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor();

    private final long cacheIntervalMillis;

    /**
     * Threshold to refresh the {@link AccessToken}
     */
    private final long threshold;


    @Builder
    public DefaultTokenManager(@NonNull Context context,
                               OAuth2Client oAuth2Client,
                               Storage<AccessToken> storage,
                               Long cacheIntervalMillis,
                               Long threshold) {

        this.storage = storage == null ? Options.INSTANCE.getOidcStorage() : storage;

        Logger.debug(TAG, "Using SharedPreference: %s", this.storage.getClass().getSimpleName());

        this.oAuth2Client = oAuth2Client;
        this.accessTokenRef = new AtomicReference<>();
        this.cacheIntervalMillis = cacheIntervalMillis == null
                ? context.getResources().getInteger(R.integer.forgerock_oauth_cache) * 1000L : cacheIntervalMillis;
        this.threshold = threshold == null
                ? context.getResources().getInteger(R.integer.forgerock_oauth_threshold) : threshold;
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void persist(@NonNull AccessToken accessToken) {
        cache(accessToken);
        storage.save(accessToken);
    }

    @Override
    public void exchangeToken(@NonNull SSOToken token, @NonNull Map<String, String> additionalParameters, FRListener<AccessToken> listener) {
        oAuth2Client.exchangeToken(token, additionalParameters, listener);
    }

    @Override
    public void exchangeToken(String code, PKCE pkce, Map<String, String> additionalParameters, FRListener<AccessToken> listener) {
        oAuth2Client.token(null, code, pkce, additionalParameters, new OAuth2ResponseHandler(), listener);
    }

    @Override
    public void getAccessToken(AccessTokenVerifier verifier, FRListener<AccessToken> tokenListener) {
        AccessToken accessToken = getAccessTokenLocally();
        if (accessToken != null) {
            accessToken.setPersisted(true);

            if (verifier != null && !verifier.isValid(accessToken)) {
                //This can run in the background.
                revokeAndEndSession(new FRListener<>() {
                    @Override
                    public void onSuccess(Void result) {
                        //Success revoke, but we telling caller that, no Access Token is available.
                        Listener.onException(tokenListener,
                                new AuthenticationRequiredException("Access Token is not valid, authentication is required."));
                    }

                    @Override
                    public void onException(Exception e) {
                        Listener.onException(tokenListener,
                                new AuthenticationRequiredException("Access Token is not valid, authentication is required."));
                    }
                });
                return;
            }

            if (accessToken.isExpired(threshold)) {
                Logger.debug(TAG, "Access Token Expired!");
                refresh(accessToken, tokenListener);
            } else {
                Listener.onSuccess(tokenListener, accessToken);
            }

        } else {
            Listener.onException(tokenListener,
                    new AuthenticationRequiredException("No Access Token, authentication is required."));
        }
    }

    @Override
    public AccessToken getAccessToken() {
        return getAccessTokenLocally();
    }

    @Override
    public boolean hasToken() {
        //Consider null if Access token does not exists
        return storage.get() != null;
    }

    @Override
    public void refresh(@NonNull AccessToken accessToken, final FRListener<AccessToken> listener) {

        String refreshToken = accessToken.getRefreshToken();
        if (refreshToken == null) {
            clear();
            Listener.onException(listener, new AuthenticationRequiredException("Refresh Token does not exists."));
            return;
        }
        //If the access token is not expired yet, revoke it
        if (!accessToken.isExpired()) {
            oAuth2Client.revoke(accessToken, false, new DoNothingListener<>());
        }
        Logger.debug(TAG, "Exchange AccessToken with Refresh Token");
        oAuth2Client.refresh(accessToken.getSessionToken(), refreshToken, new FRListener<>() {
            @Override
            public void onSuccess(AccessToken token) {
                Logger.debug(TAG, "Exchange AccessToken with Refresh Token Success");
                persist(token);
                token.setPersisted(true);
                Listener.onSuccess(listener, token);
            }

            @Override
            public void onException(Exception e) {
                Logger.debug(TAG, "Exchange AccessToken with Refresh Token Failed: %s", e.getMessage());
                if (e instanceof ApiException && e.getMessage() != null) {
                    //We clear the tokens if failed to refresh.
                    ApiException apiException = (ApiException) e;
                    if (apiException.getStatusCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
                        try {
                            JSONObject error = new JSONObject(e.getMessage());
                            if (error.getString("error").equals("invalid_grant")) {
                                clear();
                                Listener.onException(listener, new InvalidGrantException("Failed to refresh, due to invalid grant", e));
                                return;
                            }
                        } catch (JSONException jsonException) {
                            //ignore
                        }
                    }
                }
                Listener.onException(listener, e);
            }
        });
    }

    /**
     * Retrieve {@link AccessToken} from cache or from storage.
     *
     * @return The Access Token
     */
    private AccessToken getAccessTokenLocally() {

        CacheEntry<AccessToken> entry = accessTokenRef.get();
        if (entry != null) {
            if (entry.isExpired()) {
                //There is a scheduled task to remove the cache, however scheduled task may not run
                //exact time.
                accessTokenRef.set(null);
            } else {
                Logger.debug(TAG, "Retrieving Access Token from cache");
                return entry.getValue();
            }
        }
        //Consider null if Access token does not exists
        AccessToken accessToken = storage.get();
        cache(accessToken);
        return accessToken;
    }

    /**
     * Cache the {@link AccessToken} in memory and setup a worker thread to clear it after.
     *
     * @param accessToken The AccessToken
     */
    private void cache(AccessToken accessToken) {
        if (cacheIntervalMillis > 0) {
            accessTokenRef.set(new CacheEntry<>(accessToken, cacheIntervalMillis));
            worker.schedule(() -> {
                Logger.debug(TAG, "Removing Access Token from cache.");
                accessTokenRef.set(null);
            }, cacheIntervalMillis, TimeUnit.MILLISECONDS);
        }
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void clear() {
        accessTokenRef.set(null);
        storage.delete();
        //Broadcast Token removed event
        EventDispatcher.TOKEN_REMOVED.notifyObservers();
    }

    @Override
    public void revoke(FRListener<Void> listener) {
        AccessToken accessToken = getAccessTokenLocally();
        //No matter success revoke or not, clear the token locally.
        clear();
        if (accessToken == null) {
            Listener.onException(listener, new IllegalStateException("Access Token Not found!"));
            return;
        }
        //There are 2 steps here to revoke the token, the AccessToken and idToken
        Logger.debug(TAG, "Revoking AccessToken & Refresh Token.");
        oAuth2Client.revoke(accessToken, new FRListener<>() {
            @Override
            public void onSuccess(Void result) {
                Logger.debug(TAG, "Revoking AccessToken & Refresh Token Success");
                Listener.onSuccess(listener, result);
            }

            @Override
            public void onException(Exception e) {
                Logger.debug(TAG, "Revoking AccessToken & Refresh Token failed: %s", e.getMessage());
                Listener.onException(listener, e);
            }
        });
    }

    @Override
    public void revokeAndEndSession(Supplier<AppAuthConfiguration> appAuthConfiguration, FRListener<Void> listener) {
        revokeAndEndSession((accessToken, l) -> {
                    if (StringUtils.isNotEmpty(oAuth2Client.getSignOutRedirectUri())) {
                        oAuth2Client.endSessionWithBrowser(accessToken.getIdToken(), oAuth2Client, appAuthConfiguration.get(), l);
                    } else {
                        oAuth2Client.endSession(accessToken.getIdToken(), l);
                    }
                },
                listener);
    }

    @Override
    public void revokeAndEndSession(FRListener<Void> listener) {
        revokeAndEndSession((accessToken, l) -> {
                    oAuth2Client.endSession(accessToken.getIdToken(), l);
                },
                listener);
    }


    private void revokeAndEndSession(BiConsumer<AccessToken, FRListener<Void>> endSessionBlock, FRListener<Void> listener) {

        AccessToken accessToken = getAccessTokenLocally();
        //No matter success revoke or not, clear the token locally.
        clear();
        if (accessToken == null) {
            Listener.onException(listener, new IllegalStateException("Access Token Not found!"));
            return;
        }
        //There are 2 steps here to revoke the token, the AccessToken and idToken
        Logger.debug(TAG, "Revoking AccessToken & Refresh Token.");
        oAuth2Client.revoke(accessToken, new FRListener<>() {
            @Override
            public void onSuccess(Void result) {
                Logger.debug(TAG, "Revoking AccessToken & Refresh Token Success");
                if (!endSession(true)) {
                    Listener.onSuccess(listener, result);
                }
            }

            @Override
            public void onException(Exception e) {
                //Try best to end the session
                Logger.debug(TAG, "Revoking AccessToken & Refresh Token failed: %s", e.getMessage());
                endSession(false);
                Listener.onException(listener, e);
            }

            /**
             * End the user session only when the token is not bind to existing session
             * @param notifyListener To notify the caller or not
             * @return True if endSession is performed.
             */
            private boolean endSession(boolean notifyListener) {
                FRListener<Void> l = listener;
                if (listener == null || !notifyListener) {
                    l = new DoNothingListener<>();
                }
                //getSessionToken return null when using centralize login
                if (accessToken.getSessionToken() == null && isNotEmpty(accessToken.getIdToken())) {
                    endSessionBlock.accept(accessToken, l);
                    return true;
                }
                return false;
            }
        });
    }

}
