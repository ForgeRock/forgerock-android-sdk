/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
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

import org.forgerock.android.auth.authenticator.AuthenticatorService;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Interface to provide common utilities method for accessing the {@link AccountManager}
 */
interface AccountAware {

    String TAG = AccountAware.class.getSimpleName();
    String ACCOUNT_TYPE = "accountType";
    String HEALTH_CHECK_KEY = "org.forgerock.HEALTH_CHECK";

    /**
     * Retrieve the Account Type
     *
     * @param context The Application Context
     * @return The Account Type
     */
    default String getAccountType(Context context) throws PackageManager.NameNotFoundException, IOException, XmlPullParserException {
        // Get the authenticator XML file from AndroidManifest.xml
        ComponentName cn = new ComponentName(context, AuthenticatorService.class);
        ServiceInfo info = context.getPackageManager().getServiceInfo(cn, PackageManager.GET_META_DATA);
        int resourceId = info.metaData.getInt("android.accounts.AccountAuthenticator");

        // Parse the authenticator XML file to get the accountType
        return parse(context, resourceId);
    }

    /**
     * Parse the account type from forgerock_authenticator.xml.
     *
     * @param context    The application context
     * @param resourceId The AccountAuthenticator resource Id.
     * @return The account type
     */
    default String parse(Context context, int resourceId) throws IOException, XmlPullParserException {
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
     * Check to see if the account exists.
     *
     * @param accountManager The AccountManager
     * @param accountType    Account Type
     * @param account        The Account
     * @return True if the account exists
     */
    default boolean isAccountExists(AccountManager accountManager, String accountType, Account account) {
        Account[] accounts = accountManager.getAccountsByType(accountType);
        for (Account acc : accounts) {
            if (acc.name.equals(account.name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verify Account Creation using AccountManager.
     *
     * @param accountManager The AccountManager
     * @param accountType    Account Type
     * @param account        The Account to be created.
     */
    default void verifyAccount(AccountManager accountManager, String accountType, Account account) {
        if (!isAccountExists(accountManager, accountType, account)) {
            boolean result = accountManager.addAccountExplicitly(account, null, null);
            if (!result) {
                throw new IllegalStateException("Failed to add Account");
            }
        } else {
            //even it is exists, make sure that we still have access to it.
            //getUserData will throw SecurityException if user has no permission.
            accountManager.getUserData(account, HEALTH_CHECK_KEY);
        }
    }

    /**
     * Remove the Account from AccountManager.
     *
     * @param accountManager The AccountManager
     * @param account        The Account to be removed
     */
    default void removeAccount(AccountManager accountManager, Account account) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            accountManager.removeAccountExplicitly(account);
        } else {
            AccountManagerFuture<Boolean> future = accountManager.removeAccount(account, null, null);
            try {
                future.getResult();
            } catch (Exception e) {
                Logger.warn(TAG, e, "Failed to remove Account %s.", account.name);
            }
        }
    }


}