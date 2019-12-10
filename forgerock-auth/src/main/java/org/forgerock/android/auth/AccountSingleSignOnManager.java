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
import android.os.Build;
import android.util.Base64;

import org.forgerock.android.auth.authenticator.AuthenticatorService;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import lombok.Builder;
import lombok.NonNull;

/**
 * Manage SSO Token with {@link AccountManager} as the storage.
 * The stored token can be shared with SSO group Apps.
 * For Android L, the encrypted {@link javax.crypto.SecretKey} will
 * be stored as user's password using {@link AccountManager#setPassword(Account, String)},
 * for Android M+ the SecretKey will be store in the KeyChain.
 */
class AccountSingleSignOnManager implements SingleSignOnManager, KeyUpdatedListener, SecretKeyStore {

    private static final String TAG = AccountSingleSignOnManager.class.getSimpleName();
    private static final String ACCOUNT_TYPE = "accountType";
    private static final String ORG_FORGEROCK_V_1_SSO_KEYS = "org.forgerock.v1.SSO_KEYS";
    private static final String SSO_TOKEN = "org.forgerock.v1.SSO_TOKEN";

    private String accountType;
    private Encryptor encryptor;
    private AccountManager accountManager;
    private Account account;

    @Builder
    public AccountSingleSignOnManager(@NonNull Context context, Encryptor encryptor) throws Exception {
        Config config = Config.getInstance(context);
        try {
            this.accountType = getAccountType(context);
        } catch (Exception e) {
            //consider SSO is disabled.
            Logger.warn(TAG, "Single Sign On is disabled due to: %s", e.getMessage());
            throw e;
        }
        this.accountManager = AccountManager.get(context);
        this.account = new Account(config.getAccountName(), accountType);
        this.encryptor = config.applyDefaultIfNull(encryptor, context, this::getEncryptor);
        Logger.debug(TAG, "Using Encryptor %s", this.encryptor.getClass().getSimpleName());
    }

    @Override
    public void persist(SSOToken token) {
        persist(token, true);
    }

    private void persist(SSOToken token, boolean retry) {
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
    public void clear() {
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
        return accountManager.getUserData(account, SSO_TOKEN) != null;
    }

    @Override
    public void revoke(FRListener<Void> listener) {
        clear();
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

    @SuppressLint("NewApi")
    private Encryptor getEncryptor(Context context) {
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

    /**
     * When the keys that use to encrypt the data are updated (Platform upgrade from Android L to Android M)
     */
    @Override
    public void onKeyUpdated() {
        clear();
    }

}
