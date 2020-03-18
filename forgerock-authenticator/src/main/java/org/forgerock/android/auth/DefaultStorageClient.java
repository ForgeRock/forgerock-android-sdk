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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.forgerock.android.authenticator.Account;
import org.forgerock.android.authenticator.Mechanism;
import org.forgerock.android.authenticator.Notification;
import org.forgerock.android.authenticator.StorageClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object which implements StorageClient interface and uses SecureSharedPreferences from
 * forgerock-core SDK to store and load Accounts, Mechanisms and Notifications.
 */
public class DefaultStorageClient implements StorageClient {

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

//        this.accountData = context.getApplicationContext()
//                .getSharedPreferences(ORG_FORGEROCK_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE);
//        this.mechanismData = context.getApplicationContext()
//                .getSharedPreferences(ORG_FORGEROCK_SHARED_PREFERENCES_DATA_MECHANISM, Context.MODE_PRIVATE);
//        this.notificationData = context.getApplicationContext()
//                .getSharedPreferences(ORG_FORGEROCK_SHARED_PREFERENCES_DATA_NOTIFICATIONS, Context.MODE_PRIVATE);
    }

    @Override
    public Account getAccount(String accountId) {
        Gson gson = new Gson();
        String json = accountData.getString(accountId, "");
        return gson.fromJson(json, Account.class);
    }

    @Override
    public List<Account> getAllAccounts() {
        List<Account> accountList = new ArrayList<>();
        Gson gson = new Gson();

        Map<String,?> keys = accountData.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            Logger.debug(TAG, "Account map values: ",entry.getKey() + ": " + entry.getValue().toString());
            Account account = gson.fromJson(entry.getValue().toString(), Account.class);
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
        Gson gson = new Gson();
        String accountJson = gson.toJson(account);

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
        Gson gson = getMechanismSerializer();

        Map<String,?> keys = mechanismData.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            Logger.debug(TAG, "Mechanism map values: ",entry.getKey() + ": " + entry.getValue().toString());
            String jsonData = entry.getValue().toString();

            Mechanism mechanism = gson.fromJson(jsonData, Mechanism.class);
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
    public boolean removeMechanism(Mechanism mechanism) {
        return mechanismData.edit()
                .remove(mechanism.getId())
                .commit();
    }

    @Override
    public boolean setMechanism(Mechanism mechanism) {
        Gson gson = getMechanismSerializer();
        String mechanismJson = gson.toJson(mechanism, Mechanism.class);

        return mechanismData.edit()
                .putString(mechanism.getId(), mechanismJson)
                .commit();
    }

    /**
     * Get all notifications stored in the system.
     *
     * @return The complete list of notifications.
     */
    private List<Notification> getAllNotifications() {
        List<Notification> notificationList = new ArrayList<>();
        Gson gson = new Gson();

        Map<String,?> keys = notificationData.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            Logger.debug(TAG, "Notification map values: ",entry.getKey() + ": " + entry.getValue().toString());
            Notification notification = gson.fromJson(entry.getValue().toString(), Notification.class);
            if(notification != null)
                notificationList.add(notification);
        }

        return notificationList;
    }

    @Override
    public List<Notification> getAllNotificationsForMechanism(Mechanism mechanism) {
        List<Notification> notificationList = new ArrayList<>();

        List<Notification> allNotifications = this.getAllNotifications();
        for(Notification notification : allNotifications){
            if(notification.getMechanismUID().equals(mechanism.getMechanismUID())){
                notificationList.add(notification);
            }
        }

        return notificationList;
    }

    @Override
    public boolean removeNotification(Notification notification) {
        return notificationData.edit()
                .remove(notification.getId())
                .commit();
    }

    @Override
    public boolean setNotification(Notification notification) {
        Gson gson = new Gson();
        String notificationJson = gson.toJson(notification);

        return notificationData.edit()
                .putString(notification.getId(), notificationJson)
                .commit();
    }

    @Override
    public boolean isEmpty() {
        return accountData.getAll().isEmpty() &&
                mechanismData.getAll().isEmpty() &&
                notificationData.getAll().isEmpty();
    }

    /**
     * Remove all the stored {@link Account}, {@link Mechanism} and {@link Notification}
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

    private Gson getMechanismSerializer() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Mechanism.class, new SerializerInterfaceAdapter<Mechanism>());

        return gsonBuilder.create();
    }
}
