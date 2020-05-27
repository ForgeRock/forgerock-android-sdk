/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.google.firebase.messaging.RemoteMessage;

import org.forgerock.android.auth.exception.AuthenticatorException;
import org.forgerock.android.auth.exception.InvalidNotificationException;
import org.forgerock.android.auth.exception.MechanismCreationException;

import java.util.List;

class AuthenticatorManager {

    /** The Storage client. */
    private StorageClient storageClient;
    /** The FCM Device token. */
    private String deviceToken;
    /** The Application Context. */
    private Context context;
    /** The Oath Factory responsible to build Oath mechanisms. */
    private MechanismFactory oathFactory;
    /** The Push Factory responsible to build Push mechanisms. */
    private MechanismFactory pushFactory;
    /** The Notification Factory responsible to handle remote messages. */
    private NotificationFactory notificationFactory;

    private static final String TAG = AuthenticatorManager.class.getSimpleName();

    AuthenticatorManager(Context context, StorageClient storageClient, String deviceToken) {
        this.context = context;
        this.storageClient = storageClient;
        this.deviceToken = deviceToken;

        this.oathFactory = new OathFactory(context, storageClient);
        OathCodeGenerator.init(storageClient);

        if(deviceToken != null) {
            this.pushFactory = new PushFactory(context, storageClient, deviceToken);
            this.notificationFactory = new NotificationFactory(storageClient);
            PushResponder.init(storageClient);
        } else {
            Logger.debug(TAG, "No FCM device token provided. SDK will not be able to register Push mechanisms.");
        }
    }

    void createMechanismFromUri(String uri, FRAListener<Mechanism> listener) {
        Logger.debug(TAG, "Creating new mechanism from URI: %s", uri);
        if(uri.startsWith(Mechanism.PUSH)) {
            if(pushFactory != null) {
                pushFactory.createFromUri(uri, listener);
            } else {
                Logger.warn(TAG, "Attempt to add a Push mechanism has failed. " +
                        "FCM token was not provided during SDK initialization.");
                listener.onException(new MechanismCreationException("Cannot add Push mechanisms. " +
                        "FCM token not provided during SDK initialization to handle Push Notifications."));
            }
        }
        else if(uri.startsWith(Mechanism.OATH)) {
            oathFactory.createFromUri(uri, listener);
        }
        else {
            Logger.warn(TAG, "Invalid QR Code given for Mechanism initialization.");
            listener.onException(new MechanismCreationException("Invalid QR Code given for Mechanism initialization."));
        }
    }

    List<Account> getAllAccounts() {
        Logger.debug(TAG, "Retrieving all accounts from StorageClient.");
        List<Account> accountList = storageClient.getAllAccounts();

        // Sets mechanisms and/or notifications for each account
        for (Account account : accountList) {
            initializeAccount(account);
        }

        return accountList;
    }

    Account getAccount(String accountId) {
        Logger.debug(TAG, "Retrieving Account with ID '%s' from the StorageClient.", accountId);

        // Retrieve account from StorageClient
        Account account = storageClient.getAccount(accountId);

        // Sets mechanisms and/or notifications associated with this account
        initializeAccount(account);

        return account;
    }

    boolean removeAccount(Account account) {
        Logger.debug(TAG, "Removing Account with ID '%s' from the StorageClient.", account.getId());

        // Remove any mechanism or notifications data associated with the account
        for (Mechanism mechanism : storageClient.getMechanismsForAccount(account)) {
            removeMechanism(mechanism);
        }

        // Remove the account itself
        return storageClient.removeAccount(account);
    }

    boolean removeMechanism(Mechanism mechanism) {
        Logger.debug(TAG, "Removing Mechanim with ID '%s' from the StorageClient.", mechanism.getMechanismUID());

        // If PushMechanism mechanism, remove any notifications associated with it
        if(mechanism.getType().equals(Mechanism.PUSH)) {
            List<PushNotification> notificationList = storageClient.getAllNotificationsForMechanism(mechanism);
            if(!notificationList.isEmpty()) {
                Logger.debug(TAG, "Removing Push Notifications for Mechanism with ID '%s' from the StorageClient.", mechanism.getMechanismUID());
                for (PushNotification notification : notificationList) {
                    storageClient.removeNotification(notification);
                }
            }
        }

        // Remove the mechanism itself
        return storageClient.removeMechanism(mechanism);
    }

    boolean removeNotification(PushNotification notification) {
        return storageClient.removeNotification(notification);
    }

    void registerForRemoteNotifications(String newDeviceToken) throws AuthenticatorException {
        if(this.deviceToken == null) {
            this.deviceToken = newDeviceToken;
            this.pushFactory = new PushFactory(context, storageClient, newDeviceToken);
            this.notificationFactory = new NotificationFactory(storageClient);
            PushResponder.init(storageClient);
        } else {
            if(this.deviceToken.equals(newDeviceToken)) {
                Logger.warn(TAG, "The SDK was already initialized with this device token: %s",
                        newDeviceToken);
                throw new AuthenticatorException("The SDK was already initialized with the FCM device token.");
            } else {
                Logger.warn(TAG, "The SDK was initialized with a different deviceToken: %s, however a new " +
                        "device token (%s) was received.", this.deviceToken, newDeviceToken);
                throw new AuthenticatorException("The SDK was initialized with a different deviceToken.");
            }
        }
    }

    PushNotification handleMessage(RemoteMessage message)
            throws InvalidNotificationException {
        Logger.debug(TAG, "Processing FCM remote message.");
        if(notificationFactory != null) {
            return notificationFactory.handleMessage(message);
        } else {
            Logger.warn(TAG, "Attempt to process Push Notification has failed. " +
                    "FCM token was not provided during SDK initialization.");
            throw new InvalidNotificationException("Cannot process Push notification. " +
                    "FCM token was not provided during SDK initialization.");
        }
    }

    PushNotification handleMessage(String messageId, String message)
            throws InvalidNotificationException {
        Logger.debug(TAG, "Processing FCM remote message.");
        if(notificationFactory != null) {
            return notificationFactory.handleMessage(messageId, message);
        } else {
            Logger.warn(TAG, "Attempt to process Push Notification has failed. " +
                    "FCM token was not provided during SDK initialization.");
            throw new InvalidNotificationException("Cannot process Push notification. " +
                    "FCM token was not provided during SDK initialization.");
        }
    }

    private void initializeAccount(Account account) {
        if(account != null) {
            Logger.debug(TAG, "Loading associated data for the Account with ID: %s", account.getId());
            List<Mechanism> mechanismList = storageClient.getMechanismsForAccount(account);
            account.setMechanismList(mechanismList);
            for (Mechanism mechanism : mechanismList) {
                if(mechanism.getType().equals(Mechanism.PUSH)) {
                    List<PushNotification> notificationList = storageClient.getAllNotificationsForMechanism(mechanism);
                    ((PushMechanism) mechanism).setPushNotificationList(notificationList);
                }
            }
        }
    }

    private void initializeMechanism(Mechanism mechanism) {
        if(mechanism != null && mechanism.getType().equals(Mechanism.PUSH)) {
            Logger.debug(TAG, "Loading associated notifications for the mechanism with ID: %s", mechanism.getId());

            List<PushNotification> notificationList = storageClient.getAllNotificationsForMechanism(mechanism);
            ((PushMechanism) mechanism).setPushNotificationList(notificationList);
        }
    }

    @VisibleForTesting
    void setPushFactory(MechanismFactory pushFactory) {
        this.pushFactory = pushFactory;
    }

    @VisibleForTesting
    void setNotificationFactory(NotificationFactory notificationFactory) {
        this.notificationFactory = notificationFactory;
    }

}

