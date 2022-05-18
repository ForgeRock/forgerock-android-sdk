/*
 * Copyright (c) 2020 - 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object which implements StorageClient interface and uses SecureSharedPreferences from
 * forgerock-core SDK to store and load Accounts, Mechanisms and Notifications.
 */
class DefaultStorageClient implements StorageClient {

    //Alias to store keys
    private static final String ORG_FORGEROCK_SHARED_PREFERENCES_KEYS = "org.forgerock.android.authenticator.KEYS";

    //Settings to store the data
    private static final String ORG_FORGEROCK_SHARED_PREFERENCES_DATA_ACCOUNT = "org.forgerock.android.authenticator.DATA.ACCOUNT";
    private static final String ORG_FORGEROCK_SHARED_PREFERENCES_DATA_MECHANISM = "org.forgerock.android.authenticator.DATA.MECHANISM";
    private static final String ORG_FORGEROCK_SHARED_PREFERENCES_DATA_NOTIFICATIONS = "org.forgerock.android.authenticator.DATA.NOTIFICATIONS";

    //The SharedPreferences to store the data
    private SharedPreferences accountData;
    private SharedPreferences mechanismData;
    private SharedPreferences notificationData;

    private static final String TAG = DefaultStorageClient.class.getSimpleName();

    public DefaultStorageClient(Context context) {
        this.accountData = new SecuredSharedPreferences(context,
                ORG_FORGEROCK_SHARED_PREFERENCES_DATA_ACCOUNT, ORG_FORGEROCK_SHARED_PREFERENCES_KEYS);
        this.mechanismData = new SecuredSharedPreferences(context,
                ORG_FORGEROCK_SHARED_PREFERENCES_DATA_MECHANISM, ORG_FORGEROCK_SHARED_PREFERENCES_KEYS);
        this.notificationData = new SecuredSharedPreferences(context,
                ORG_FORGEROCK_SHARED_PREFERENCES_DATA_NOTIFICATIONS, ORG_FORGEROCK_SHARED_PREFERENCES_KEYS);
    }

    @Override
    public Account getAccount(String accountId) {
        String json = accountData.getString(accountId, "");
        return Account.deserialize(json);
    }

    @Override
    public List<Account> getAllAccounts() {
        List<Account> accountList = new ArrayList<>();

        Map<String,?> keys = accountData.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            Logger.debug(TAG, "Account map values: ",entry.getKey() + ": " + entry.getValue().toString());
            Account account = Account.deserialize(entry.getValue().toString());
            if(account != null)
                accountList.add(account);
        }

        return accountList;
    }

    @Override
    public boolean removeAccount(Account account) {
        return accountData.edit()
                .remove(account.getId())
                .commit();
    }

    @Override
    public boolean setAccount(Account account) {
        String accountJson = account.serialize();

        return accountData.edit()
                .putString(account.getId(), accountJson)
                .commit();
    }

    /**
     * Get all mechanisms stored in the system.
     *
     * @return The complete list of mechanisms.
     */
    private List<Mechanism> getAllMechanisms() {
        List<Mechanism> mechanismList = new ArrayList<>();

        Map<String,?> keys = mechanismData.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            Logger.debug(TAG, "Mechanism map values: ",entry.getKey() + ": " + entry.getValue().toString());
            String jsonData = entry.getValue().toString();

            Mechanism mechanism = Mechanism.deserialize(jsonData);
            if(mechanism != null)
                mechanismList.add(mechanism);
        }

        return mechanismList;
    }

    @Override
    public List<Mechanism> getMechanismsForAccount(Account account) {
        List<Mechanism> mechanismList = new ArrayList<>();

        List<Mechanism> allMechanisms = this.getAllMechanisms();
        for(Mechanism mechanism : allMechanisms){
            if(mechanism.getIssuer().equals(account.getIssuer()) &&
               mechanism.getAccountName().equals(account.getAccountName())){
                mechanismList.add(mechanism);
            }
        }

        return mechanismList;
    }

    @Override
    public Mechanism getMechanismByUUID(String mechanismUID) {
        Mechanism mechanism = null;

        List<Mechanism> allMechanisms = this.getAllMechanisms();
        for(Mechanism mechanismEntry : allMechanisms){
            if (mechanismEntry.getMechanismUID().equals(mechanismUID)) {
                mechanism = mechanismEntry;
                break;
            }
        }

        return mechanism;
    }

    @Override
    public boolean removeMechanism(Mechanism mechanism) {
        return mechanismData.edit()
                .remove(mechanism.getId())
                .commit();
    }

    @Override
    public boolean setMechanism(Mechanism mechanism) {
        String mechanismJson = mechanism.serialize();

        return mechanismData.edit()
                .putString(mechanism.getId(), mechanismJson)
                .commit();
    }

    @Override
    public List<PushNotification> getAllNotifications() {
        List<PushNotification> pushNotificationList = new ArrayList<>();

        Map<String,?> keys = notificationData.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            Logger.debug(TAG, "PushNotification map values: ",entry.getKey() + ": " + entry.getValue().toString());
            PushNotification pushNotification = PushNotification.deserialize(entry.getValue().toString());
            if(pushNotification != null)
                pushNotificationList.add(pushNotification);
        }

        return pushNotificationList;
    }

    @Override
    public List<PushNotification> getAllNotificationsForMechanism(Mechanism mechanism) {
        List<PushNotification> pushNotificationList = new ArrayList<>();

        List<PushNotification> allPushNotifications = this.getAllNotifications();
        for(PushNotification pushNotification : allPushNotifications) {
            if(pushNotification.getMechanismUID().equals(mechanism.getMechanismUID())) {
                pushNotification.setPushMechanism(mechanism);
                pushNotificationList.add(pushNotification);
            }
        }

        return pushNotificationList;
    }

    @Override
    public boolean removeNotification(PushNotification pushNotification) {
        return notificationData.edit()
                .remove(pushNotification.getId())
                .commit();
    }

    @Override
    public boolean setNotification(PushNotification pushNotification) {
        String notificationJson = pushNotification.serialize();

        return notificationData.edit()
                .putString(pushNotification.getId(), notificationJson)
                .commit();
    }

    @Override
    public PushNotification getNotification(String notificationId) {
        String json = notificationData.getString(notificationId, null);
        return PushNotification.deserialize(json);
    }

    @Override
    public boolean isEmpty() {
        return accountData.getAll().isEmpty() &&
                mechanismData.getAll().isEmpty() &&
                notificationData.getAll().isEmpty();
    }

    /**
     * Remove all the stored {@link Account}, {@link Mechanism} and {@link PushNotification}
     */
    @SuppressLint("ApplySharedPref")
    public void removeAll() {
        accountData.edit()
                .clear()
                .commit();
        mechanismData.edit()
                .clear()
                .commit();
        notificationData.edit()
                .clear()
                .commit();
    }

    @VisibleForTesting
    void setAccountData(SharedPreferences sharedPreferences) {
        this.accountData = sharedPreferences;
    }

    @VisibleForTesting
    void setMechanismData(SharedPreferences sharedPreferences) {
        this.mechanismData = sharedPreferences;
    }

    @VisibleForTesting
    void setNotificationData(SharedPreferences sharedPreferences) {
        this.notificationData = sharedPreferences;
    }
    
}
