/*
 * Copyright (c) 2019 - 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.XmlResourceParser;
import android.os.Build;
import android.util.Base64;

import org.forgerock.android.auth.authenticator.AuthenticatorService;
import org.json.JSONArray;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import lombok.Builder;
import lombok.NonNull;

import static java.util.Collections.emptySet;
import static org.forgerock.android.auth.Encryptor.getEncryptor;

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
    private static final String COOKIES = "org.forgerock.v1.COOKIES";

    private String accountType;
    private Encryptor encryptor;
    private AccountManager accountManager;
    private Account account;

    @Builder
    AccountSingleSignOnManager(@NonNull Context context, @NonNull String accountName, Encryptor encryptor) throws Exception {
        try {
            this.accountType = getAccountType(context);
        } catch (Exception e) {
            //consider SSO is disabled.
            Logger.warn(TAG, "Single Sign On is disabled due to: %s", e.getMessage());
            throw e;
        }
        this.accountManager = AccountManager.get(context);
        this.account = new Account(accountName, accountType);
        this.encryptor = encryptor == null ?
                getEncryptor(context, ORG_FORGEROCK_V_1_SSO_KEYS, this, this) :
                encryptor;
        Logger.debug(TAG, "Using Encryptor %s", this.encryptor.getClass().getSimpleName());
    }

    @Override
    public void persist(SSOToken token) {
        persist(SSO_TOKEN, token.getValue().getBytes(), true);
    }

    @Override
    public void persist(Collection<String> cookies) {
        if (cookies.isEmpty()) {
            persist(COOKIES, null, true);
            return;
        }
        JSONArray array = new JSONArray();
        for (String s : cookies) {
            array.put(s);
        }
        persist(COOKIES, array.toString().getBytes(), true);

    }

    private void persist(String alias, byte[] data, boolean retry) {
        if ((data == null || data.length == 0) && !isAccountExists()) {
            //Account does not exist and nothing to persist
            return;
        }
        accountManager.addAccountExplicitly(account, null, null);
        try {
            if (data == null) {
                accountManager.setUserData(account, alias, null);
            } else {
                accountManager.setUserData(account, alias,
                        Base64.encodeToString(encryptor.encrypt(data), Base64.DEFAULT));
            }
        } catch (Exception e) {
            try {
                encryptor.reset();
                if (retry) {
                    persist(alias, data, false);
                } else {
                    throw new RuntimeException(e);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private boolean isAccountExists() {
        Account[] accounts = accountManager.getAccountsByType(accountType);
        for (Account acc : accounts) {
            if (acc.name.equals(account.name)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void clear() {
        Account[] accounts = accountManager.getAccountsByType(accountType);
        for (Account acc : accounts) {
            if (acc.name.equals(account.name)) {
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
                return;
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
            Logger.warn(TAG, e, "Failed to decrypt data");
            return null;
        }
    }

    @Override
    public Collection<String> getCookies() {
        try {
            String encryptedCookies = accountManager.getUserData(account, COOKIES);
            if (encryptedCookies != null) {
                JSONArray array = new JSONArray(new String(encryptor.decrypt(Base64.decode(encryptedCookies, Base64.DEFAULT))));
                Set<String> set = new HashSet<>();
                for (int i = 0; i < array.length(); i++) {
                    set.add(array.getString(i));
                }
                return set;
            } else {
                return emptySet();
            }
        } catch (Exception e) {
            return emptySet();
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
