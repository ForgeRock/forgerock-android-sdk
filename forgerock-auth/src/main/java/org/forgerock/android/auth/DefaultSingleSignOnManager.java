/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;

import org.forgerock.android.auth.authenticator.AuthenticatorService;
import org.jetbrains.annotations.NotNull;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

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

/**
 * Manage the Single Sign On Token, the token will be encrypted and store to {@link AccountManager}, for Android L, the
 * encrypted {@link javax.crypto.SecretKey} will be stored as user's password using
 * {@link AccountManager#setPassword(Account, String)}, for Android M+ the SecretKey will be store in the KeyChain.
 */
class DefaultSingleSignOnManager implements SingleSignOnManager, KeyUpdatedListener, SecretKeyStore, ResponseHandler {

    private static final String TAG = DefaultSingleSignOnManager.class.getSimpleName();
    //Alias to store the SecretKey
    static final String ORG_FORGEROCK_V_1_SSO_KEYS = "org.forgerock.v1.SSO_KEYS";
    private static final String ACCOUNT_TYPE = "accountType";
    private static final String SSO_TOKEN = "org.forgerock.v1.SSO_TOKEN";
    private String accountType;
    private Encryptor encryptor;
    private AccountManager accountManager;
    private Account account;
    private boolean ssoEnabled = true;
    private ServerConfig serverConfig;

    @Builder
    DefaultSingleSignOnManager(@NonNull Context context, ServerConfig serverConfig, Encryptor encryptor, Boolean disableSSO) {
        if (disableSSO != null && disableSSO) {
            ssoEnabled = false;
            return;
        }
        Config config = Config.getInstance(context);
        try {
            this.accountType = getAccountType(context);
        } catch (Exception e) {
            //consider SSO is disabled.
            Logger.warn(TAG, "Single Sign On is disabled due to: %s", e.getMessage());
            ssoEnabled = false;
            return;
        }
        this.accountManager = AccountManager.get(context);
        this.account = new Account(config.getAccountName(), accountType);
        this.encryptor = config.applyDefaultIfNull(encryptor, context, this::getEncryptor);
        Logger.debug(TAG, "Using Encryptor %s", this.encryptor.getClass().getSimpleName());
        this.serverConfig = config.applyDefaultIfNull(serverConfig);
    }

    @Override
    public void persist(SSOToken token) {
        persist(token, true);
    }

    @SuppressLint("NewApi")
    protected Encryptor getEncryptor(Context context) {
        switch (Build.VERSION.SDK_INT) {
            case Build.VERSION_CODES.LOLLIPOP:
            case Build.VERSION_CODES.LOLLIPOP_MR1:
                return new AndroidLEncryptor(context, ORG_FORGEROCK_V_1_SSO_KEYS, this);
            case Build.VERSION_CODES.M:
                return new AndroidMEncryptor(ORG_FORGEROCK_V_1_SSO_KEYS, this);
            case Build.VERSION_CODES.N:
                return new AndroidNEncryptor(ORG_FORGEROCK_V_1_SSO_KEYS, this);
            default:
                return new AndroidNEncryptor(ORG_FORGEROCK_V_1_SSO_KEYS, this);
        }
    }


    private void persist(SSOToken token, boolean retry) {
        if (!ssoEnabled) return;
        accountManager.addAccountExplicitly(account, null, null);
        try {
            accountManager.setUserData(account, SSO_TOKEN,
                    Base64.encodeToString(encryptor.encrypt(token.getValue().getBytes()), Base64.DEFAULT));
        } catch (Exception e) {
            try {
                encryptor.reset();
                if (retry) {
                    persist(token, false);
                } else {
                    throw new RuntimeException(e);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

        }
    }

    @Override
    public void persist(String encryptedSecretKey) {
        accountManager.setPassword(account, encryptedSecretKey);
    }

    @Override
    public String getEncryptedSecretKey() {
        return accountManager.getPassword(account);
    }

    @Override
    public void remove() {
        accountManager.setPassword(account, null);
    }

    @Override
    public void clear() {
        if (!ssoEnabled) return;
        Account[] accounts = accountManager.getAccountsByType(accountType);
        for (Account acc : accounts) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                accountManager.removeAccountExplicitly(acc);
            } else {
                AccountManagerFuture<Boolean> future = accountManager.removeAccount(acc, null, null);
                try {
                    future.getResult();
                } catch (Exception e) {
                    Logger.warn(TAG, e, "Failed to remove Account %s.", acc.name);
                }
            }
        }
    }

    @Override
    public SSOToken getToken() {
        if (!ssoEnabled) return null;
        try {
            String encryptedToken = accountManager.getUserData(account, SSO_TOKEN);
            if (encryptedToken != null) {
                return new SSOToken(new String(encryptor.decrypt(Base64.decode(encryptedToken, Base64.DEFAULT))));
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean hasToken() {
        if (!ssoEnabled) return false;
        return accountManager.getUserData(account, SSO_TOKEN) != null;
    }

    @Override
    public void revoke(final FRListener<Void> listener) {

        SSOToken token = getToken();
        if (token == null) {
            Listener.onException(listener, new IllegalStateException("SSO Token not found."));
            return;
        }

        //No matter success or fail, we clear the token
        clear();

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
                .header(SSOToken.IPLANET_DIRECTORY_PRO, token.getValue())
                .header(ServerConfig.X_REQUESTED_WITH, ServerConfig.XML_HTTP_REQUEST)
                .url(logout)
                .post(RequestBody.create(new byte[0]))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Listener.onException(listener, e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if (response.isSuccessful()) {
                    Listener.onSuccess(listener, null);
                } else {
                    handleError(response, listener);
                }
            }
        });
    }

    private String getAccountType(Context context) throws PackageManager.NameNotFoundException, IOException, XmlPullParserException {
        // Get the authenticator XML file from AndroidManifest.xml
        ComponentName cn = new ComponentName(context, AuthenticatorService.class);
        ServiceInfo info = context.getPackageManager().getServiceInfo(cn, PackageManager.GET_META_DATA);
        int resourceId = info.metaData.getInt("android.accounts.AccountAuthenticator");

        // Parse the authenticator XML file to get the accountType
        return parse(context, resourceId);
    }

    private String parse(Context context, int resourceId) throws IOException, XmlPullParserException {
        XmlResourceParser xrp = context.getResources().getXml(resourceId);
        xrp.next();
        int eventType = xrp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG || eventType == XmlPullParser.END_TAG) {
                for (int i = 0; i < xrp.getAttributeCount(); i++) {
                    String name = xrp.getAttributeName(i);
                    if (ACCOUNT_TYPE.equals(name)) {
                        return xrp.getAttributeValue(i);
                    }
                }
            }
            eventType = xrp.next();
        }
        throw new IllegalArgumentException("AccountType is not defined under forgerock_authenticator.xml");
    }

    /**
     * When the keys that use to encrypt the data are updated (Platform upgrade from Android L to Android M)
     */
    @Override
    public void onKeyUpdated() {
        clear();
    }

}
