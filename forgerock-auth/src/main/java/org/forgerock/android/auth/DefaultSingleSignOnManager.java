/*
 * Copyright (c) 2019 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import lombok.Builder;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static org.forgerock.android.auth.ServerConfig.ACCEPT_API_VERSION;
import static org.forgerock.android.auth.ServerConfig.API_VERSION_3_1;
import static org.forgerock.android.auth.StringUtils.isNotEmpty;

/**
 * Manage the Single Sign On Token, the token will be encrypted and store to {@link AccountManager}
 * or {@link SharedPreferences}.
 */
class DefaultSingleSignOnManager implements SingleSignOnManager, ResponseHandler {

    private static final String TAG = DefaultSingleSignOnManager.class.getSimpleName();

    private SingleSignOnManager singleSignOnManager;
    private ServerConfig serverConfig;
    private static final Action LOGOUT = new Action(Action.LOGOUT);

    @Builder
    private DefaultSingleSignOnManager(@NonNull Context context,
                                       ServerConfig serverConfig,
                                       String accountName,
                                       Encryptor encryptor,
                                       SharedPreferences sharedPreferences) {
        try {
            singleSignOnManager = AccountSingleSignOnManager.builder()
                    .context(context)
                    .accountName(accountName == null ? context.getString(R.string.forgerock_account_name) : accountName)
                    .encryptor(encryptor).build();
        } catch (Exception e) {
            Logger.debug(TAG, "Single Sign On is disabled");
            Logger.warn(TAG, "Fallback to SharedPreference to store SSO Token");
            singleSignOnManager = SharedPreferencesSignOnManager.builder()
                    .context(context)
                    .sharedPreferences(sharedPreferences).build();
        }

        this.serverConfig = serverConfig;
    }

    @Override
    public void persist(SSOToken token) {
        singleSignOnManager.persist(token);
    }

    @Override
    public void persist(Collection<String> cookies) {
        singleSignOnManager.persist(cookies);
    }

    @Override
    public void clear() {
        singleSignOnManager.clear();
        //Broadcast Token removed event
        EventDispatcher.TOKEN_REMOVED.notifyObservers();
    }

    @Override
    public SSOToken getToken() {
        return singleSignOnManager.getToken();
    }

    @Override
    public Collection<String> getCookies() {
        return singleSignOnManager.getCookies();
    }

    @Override
    public boolean hasToken() {
        return singleSignOnManager.hasToken();
    }

    @Override
    public void revoke(final FRListener<Void> listener) {

        SSOToken token = getToken();
        if (token == null) {
            Listener.onException(listener, new IllegalStateException("SSO Token not found."));
            return;
        }

        //No matter success or fail, we clear the token
        singleSignOnManager.revoke(null);

        URL logout = null;
        try {
            logout = getLogoutUrl();
        } catch (MalformedURLException e) {
            Listener.onException(listener, e);
            return;
        }

        OkHttpClient client = OkHttpClientProvider.getInstance().lookup(serverConfig);
        Request request = new Request.Builder()
                .header(serverConfig.getCookieName(), token.getValue())
                .header(ACCEPT_API_VERSION, API_VERSION_3_1)
                .url(logout)
                .post(RequestBody.create(new byte[0]))
                .tag(LOGOUT)
                .build();

        Logger.debug(TAG, "Logout session.");
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Logger.debug(TAG, "Logout session failed %s:", e.getMessage());
                Listener.onException(listener, e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    Logger.debug(TAG, "Logout session success");
                    Listener.onSuccess(listener, null);
                    close(response);
                } else {
                    Logger.debug(TAG, "Logout session failed");
                    handleError(response, listener);
                }
            }
        });
    }

    private URL getLogoutUrl() throws MalformedURLException {
        Uri.Builder builder = Uri.parse(serverConfig.getUrl()).buildUpon();
        if (isNotEmpty(serverConfig.getLogoutEndpoint())) {
            builder.appendEncodedPath(serverConfig.getLogoutEndpoint());
        } else {
            builder.appendPath("json")
                    .appendPath("realms")
                    .appendPath(serverConfig.getRealm())
                    .appendPath("sessions");
        }
        builder.appendQueryParameter("_action", "logout");
        return new URL(builder.build().toString());
    }
}
