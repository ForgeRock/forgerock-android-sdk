/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Test class which implements StorageClient interface and uses SharedPreferences to store
 * and load Accounts, Mechanisms and Notifications.
 */
public class CustomStorageClient implements StorageClient {

    //Settings to store the data
    private static final String TEST_SHARED_PREFERENCES_DATA_ACCOUNT = "test.DATA.ACCOUNT";
    private static final String TEST_SHARED_PREFERENCES_DATA_MECHANISM = "test.DATA.MECHANISM";
    private static final String TEST_SHARED_PREFERENCES_DATA_NOTIFICATIONS = "test.DATA.NOTIFICATIONS";

    //The SharedPreferences to store the data
    private SharedPreferences accountData;
    private SharedPreferences mechanismData;
    private SharedPreferences notificationData;

    private static final String TAG = CustomStorageClient.class.getSimpleName();

    public CustomStorageClient(Context context) {
        this.accountData = context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE);
        this.mechanismData = context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_MECHANISM, Context.MODE_PRIVATE);
        this.notificationData = context.getApplicationContext()
                .getSharedPreferences(TEST_SHARED_PREFERENCES_DATA_NOTIFICATIONS, Context.MODE_PRIVATE);
    }

    @Override
    public Account getAccount(String accountId) {
        String json = accountData.getString(accountId, "");
        return Account.fromJson(json);
    }

    @Override
    public List<Account> getAllAccounts() {
        List<Account> accountList = new ArrayList<>();

        Map<String,?> keys = accountData.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            Logger.debug(TAG, "Account map values: ",entry.getKey() + ": " + entry.getValue().toString());
            Account account = Account.fromJson(entry.getValue().toString());
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
        String accountJson = account.toJson();

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

            Mechanism mechanism = Mechanism.fromJson(jsonData);
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
            if(mechanism.getId().contains(account.getId())){
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
        String mechanismJson = mechanism.toJson();

        return mechanismData.edit()
                .putString(mechanism.getId(), mechanismJson)
                .commit();
    }

    /**
     * Get all notifications stored in the system.
     *
     * @return The complete list of notifications.
     */
    private List<PushNotification> getAllNotifications() {
        List<PushNotification> pushNotificationList = new ArrayList<>();

        Map<String,?> keys = notificationData.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            Logger.debug(TAG, "PushNotification map values: ",entry.getKey() + ": " + entry.getValue().toString());
            PushNotification pushNotification = PushNotification.fromJson(entry.getValue().toString());
            if(pushNotification != null)
                pushNotificationList.add(pushNotification);
        }

        return pushNotificationList;
    }

    @Override
    public List<PushNotification> getAllNotificationsForMechanism(Mechanism mechanism) {
        List<PushNotification> pushNotificationList = new ArrayList<>();

        List<PushNotification> allPushNotifications = this.getAllNotifications();
        for(PushNotification pushNotification : allPushNotifications){
            if(pushNotification.getMechanismUID().equals(mechanism.getMechanismUID())){
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
        String notificationJson = pushNotification.toJson();

        return notificationData.edit()
                .putString(pushNotification.getId(), notificationJson)
                .commit();
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

}
