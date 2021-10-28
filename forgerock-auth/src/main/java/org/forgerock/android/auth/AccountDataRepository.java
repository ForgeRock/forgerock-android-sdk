/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.forgerock.android.auth.Encryptor.getEncryptor;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.Context;
import android.util.Base64;

import androidx.annotation.NonNull;

import lombok.Builder;

/**
 * A Repository that store data in {@link AccountManager}
 */
@TargetApi(23)
class AccountDataRepository implements DataRepository, AccountAware, KeyUpdatedListener {

    private final Encryptor encryptor;
    private final AccountManager accountManager;
    private final Account account;

    /**
     * Create an AccountDataRepository
     *
     * @param context         The Application Context.
     * @param accountName     The Account Name to store the data.
     * @param encryptor       The Encryptor to encrypt the data.
     * @param defaultKeyAlias The key alias to store the key for data encryption.
     * @throws Exception Failed to create an AccountDataRepository.
     */
    @Builder
    public AccountDataRepository(@NonNull Context context, @NonNull String accountName, Encryptor encryptor, String defaultKeyAlias) throws Exception {
        String accountType;
        try {
            accountType = getAccountType(context);
        } catch (Exception e) {
            Logger.error(TAG, "Account Authenticator is not configured: %s", e.getMessage());
            throw e;
        }
        this.accountManager = AccountManager.get(context);
        this.account = new Account(accountName, accountType);
        this.encryptor = encryptor == null ?
                getEncryptor(context, defaultKeyAlias, this) :
                encryptor;
        Logger.debug(TAG, "Using Encryptor %s", this.encryptor.getClass().getSimpleName());

        verifyAccount(accountManager, accountType, account);
    }

    @Override
    public void save(String key, String value) {
        persist(key, value, true);
    }

    @Override
    public String getString(String key) {
        try {
            String encryptedData = accountManager.getUserData(account, key);
            if (encryptedData != null) {
                return new String(encryptor.decrypt(Base64.decode(encryptedData, Base64.DEFAULT)));
            }
        } catch (EncryptionException e) {
            Logger.warn(TAG, e, "Failed to decrypt data");
            //The data are not valid.
            deleteAll();
        }
        return null;
    }

    @Override
    public void delete(String key) {
        accountManager.setUserData(account, key, null);
    }

    @Override
    public void deleteAll() {
        removeAccount(accountManager, account);
    }

    private void persist(String alias, String data, boolean retry) {
        accountManager.addAccountExplicitly(account, null, null);
        try {
            if (data == null) {
                accountManager.setUserData(account, alias, null);
            } else {
                accountManager.setUserData(account, alias,
                        Base64.encodeToString(encryptor.encrypt(data.getBytes()), Base64.DEFAULT));
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

    @Override
    public void onKeyUpdated() {
        deleteAll();
    }

}
