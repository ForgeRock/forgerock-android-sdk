/*
 * Copyright (c) 2020 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.controller;

import android.content.Context;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import org.forgerock.android.auth.Account;
import org.forgerock.android.auth.FRAClient;
import org.forgerock.android.auth.FRAListener;
import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.PushNotification;
import org.forgerock.android.auth.exception.AuthenticatorException;
import org.forgerock.android.auth.exception.InvalidNotificationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Class which represents the Model, and handles all operations performed with the Authenticator SDK.
 * It loads the full Accounts data from the storage on the initialisation.
 */
public class AuthenticatorModel {

    private FRAClient fraClient;
    private String fcmToken;
    private List<AuthenticatorModelListener> listeners;
    private List<Account> allAccounts;

    private static final String TAG = AuthenticatorModel.class.getSimpleName();

    private static AuthenticatorModel INSTANCE = null;

    public static AuthenticatorModel getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("AuthenticatorModel is not initialized. " +
                    "Please make sure to call AuthenticatorModel#getInstance passing the Context first.");
        }
        return INSTANCE;
    }

    public static synchronized AuthenticatorModel getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AuthenticatorModel.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AuthenticatorModel(context);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Constructs the AuthenticatorModel
     * @param context the application context
     */
    public AuthenticatorModel(Context context) {
        // Initialize Authenticator SDK
        try {
            fraClient = FRAClient.builder()
                    .withContext(context.getApplicationContext())
                    .start();
        } catch (AuthenticatorException e) {
            Log.e(TAG,"Error initializing Authenticator SDK: ", e);
        }

        // Initialize listener array
        listeners = new ArrayList<>();

        // Retrieve the FCM token
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "getInstanceId failed", task.getException());
                        return;
                    }

                    // Get new Instance ID token
                    fcmToken = task.getResult();
                    Log.v("FCM token:", fcmToken);

                    // Register the token with the SDK to enable Push mechanisms
                    try {
                        fraClient.registerForRemoteNotifications(fcmToken);
                    } catch (AuthenticatorException e) {
                        Log.e(TAG,"Error registering FCM token: ", e);
                    }
                });

        // Retrieve the accounts on initialization
        getAllAccounts();
    }

    /**
     * Add a listener to this model.
     * @param listener The listener to add.
     */
    public void addListener(AuthenticatorModelListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener from the model.
     * @param listener The listener to remove.
     */
    public void removeListener(AuthenticatorModelListener listener) {
        listeners.remove(listener);
    }

    /**
     * Used to notify all listeners that a data has been added or removed.
     */
    public void notifyDataChanged() {
        for (AuthenticatorModelListener listener : listeners) {
            listener.dataChanged();
        }
    }

    /**
     * Retrieve account by id from the storage system
     */
    public Account getAccount(String id) {
        return fraClient.getAccount(id);
    }

    /**
     * Remove account object from the storage system
     */
    public boolean removeAccount(Account account) {
        boolean result = fraClient.removeAccount(account);
        if(result) {
            notifyDataChanged();
        }
        return result;
    }

    /**
     * Retrieve all accounts from the storage system
     */
    public List<Account> getAllAccounts() {
        return allAccounts = fraClient.getAllAccounts();
    }

    /**
     * Create a Mechanism using the URL extracted from the QRCode
     */
    public void createMechanismFromUri(String uri, FRAListener<Mechanism> listener) {
        fraClient.createMechanismFromUri(uri, listener);
    }

    /**
     * Process FCM message into a PushNotification
     */
    public PushNotification handleRemoteMessage(RemoteMessage message)
            throws InvalidNotificationException {
        PushNotification notification = fraClient.handleMessage(message);
        if(notification != null) {
            notifyDataChanged();
        }
        return notification;
    }

    /**
     * Get the Mechanism object with its id
     */
    public Mechanism getMechanism(PushNotification notification) {
        return fraClient.getMechanism(notification);
    }

    /**
     * Remove the passed Mechanism from the storage
     */
    public boolean removeMechanism(Mechanism mechanism) {
        boolean result = fraClient.removeMechanism(mechanism);
        if(result) {
            notifyDataChanged();
        }
        return result;
    }

    /**
     * Remove the passed PushNotification from the storage
     */
    public boolean removeNotification(PushNotification notification) {
        boolean result = fraClient.removeNotification(notification);
        if(result) {
            notifyDataChanged();
        }
        return result;
    }

    /**
     * Retrieve all notifications from the storage system
     */
    public List<PushNotification> getAllNotifications() {
        return fraClient.getAllNotifications();
    }

    /**
     * Retrieve notifications filtered by Mechanism the storage system
     */
    public List<PushNotification> getNotificationsForMechanism(Mechanism mechanism) {
        return fraClient.getAllNotifications(mechanism);
    }

}
