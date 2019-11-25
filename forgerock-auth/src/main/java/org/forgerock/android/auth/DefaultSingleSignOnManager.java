/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import lombok.Builder;
import lombok.NonNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static org.forgerock.android.auth.SSOToken.IPLANET_DIRECTORY_PRO;
import static org.forgerock.android.auth.ServerConfig.ACCEPT_API_VERSION;
import static org.forgerock.android.auth.ServerConfig.API_VERSION_3_1;

/**
 * Manage the Single Sign On Token, the token will be encrypted and store to {@link AccountManager}
 * or {@link SharedPreferences}.
 */
class DefaultSingleSignOnManager implements SingleSignOnManager, ResponseHandler {

    private static final String TAG = DefaultSingleSignOnManager.class.getSimpleName();

    private SingleSignOnManager singleSignOnManager;
    private ServerConfig serverConfig;

    @Builder
    DefaultSingleSignOnManager(@NonNull Context context, ServerConfig serverConfig, Encryptor encryptor, SharedPreferences sharedPreferences) {

        try {
            singleSignOnManager = AccountSingleSignOnManager.builder().context(context).encryptor(encryptor).build();
        } catch (Exception e) {
            Logger.warn(TAG, "Fallback to SharedPreference to store SSO Token");
            singleSignOnManager = SharedPreferencesSignOnManager.builder().context(context).sharedPreferences(sharedPreferences).build();
        }


        Config config = Config.getInstance(context);
        this.serverConfig = config.applyDefaultIfNull(serverConfig);
    }

    @Override
    public void persist(SSOToken token) {
        singleSignOnManager.persist(token);
    }

    @Override
    public void clear() {
        singleSignOnManager.clear();
    }

    @Override
    public SSOToken getToken() {
        return singleSignOnManager.getToken();
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
            logout = new URL(Uri.parse(serverConfig.getUrl())
                    .buildUpon()
                    .appendPath("json")
                    .appendPath("realms")
                    .appendPath(serverConfig.getRealm())
                    .appendPath("sessions")
                    .appendQueryParameter("_action", "logout")
                    .build().toString());
        } catch (MalformedURLException e) {
            Listener.onException(listener, e);
            return;
        }

        OkHttpClient client = OkHttpClientProvider.getInstance().lookup(serverConfig);
        Request request = new Request.Builder()
                .header(IPLANET_DIRECTORY_PRO, token.getValue())
                .header(ACCEPT_API_VERSION, API_VERSION_3_1)
                .url(logout)
                .post(RequestBody.create(new byte[0]))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Listener.onException(listener, e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    Listener.onSuccess(listener, null);
                } else {
                    handleError(response, listener);
                }
            }
        });
    }

}
