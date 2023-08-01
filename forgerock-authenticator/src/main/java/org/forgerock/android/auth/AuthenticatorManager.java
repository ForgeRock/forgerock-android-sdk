/*
 * Copyright (c) 2020 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.google.firebase.messaging.RemoteMessage;

import org.forgerock.android.auth.exception.AccountLockException;
import org.forgerock.android.auth.exception.AuthenticatorException;
import org.forgerock.android.auth.exception.DuplicateMechanismException;
import org.forgerock.android.auth.exception.InvalidNotificationException;
import org.forgerock.android.auth.exception.MechanismCreationException;
import org.forgerock.android.auth.exception.MechanismParsingException;
import org.forgerock.android.auth.exception.MechanismPolicyViolationException;
import org.forgerock.android.auth.policy.FRAPolicy;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    /** The Policy Evaluator is used to enforce policy compliance. */
    private FRAPolicyEvaluator policyEvaluator;

    private static final String TAG = AuthenticatorManager.class.getSimpleName();

    AuthenticatorManager(Context context, StorageClient storageClient, FRAPolicyEvaluator policyEvaluator,
                         String deviceToken) {
        this.context = context;
        this.storageClient = storageClient;
        this.deviceToken = deviceToken;
        this.policyEvaluator = policyEvaluator;
        this.oathFactory = new OathFactory(context, storageClient);
        this.pushFactory = new PushFactory(context, storageClient, deviceToken);
        this.notificationFactory = new NotificationFactory(storageClient);

        OathCodeGenerator.getInstance(storageClient);
        PushResponder.getInstance(storageClient);
    }

    void createMechanismFromUri(String uri, FRAListener<Mechanism> listener) {
        Logger.debug(TAG, "Creating new mechanism from URI: %s", uri);
        if(uri.startsWith(Mechanism.PUSH)) {
            if(deviceToken != null) {
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
        else if(uri.startsWith(Mechanism.MFAUTH)) {
            createCombinedMechanismsFromUri(uri, listener);
        }
        else {
            Logger.warn(TAG, "Invalid QR Code given for Mechanism initialization.");
            listener.onException(new MechanismCreationException("Invalid QR Code given for Mechanism initialization."));
        }
    }

    List<Account> getAllAccounts() {
        return this.getAllAccounts(true);
    }

    List<Account> getAllAccounts(boolean initializeObjects) {
        Logger.debug(TAG, "Retrieving all accounts from StorageClient.");
        List<Account> accountList = storageClient.getAllAccounts();

        // Sets mechanisms and/or notifications for each account
        if(initializeObjects) {
            for (Account account : accountList) {
                initializeAccount(account);
            }
        }

        return accountList;
    }

    Account getAccount(String accountId) {
        Logger.debug(TAG, "Retrieving Account with ID '%s' from the StorageClient.", accountId);

        // Retrieve account from StorageClient
        Account account = storageClient.getAccount(accountId);

        // Sets mechanisms associated with this account
        initializeAccount(account);

        return account;
    }

    Account getAccount(Mechanism mechanism) {
        String mechanismUID = mechanism.getMechanismUID();
        Logger.debug(TAG, "Retrieving Account with Mechanism ID '%s' from the StorageClient.", mechanismUID);

        // Retrieve account from StorageClient
        Account account;
        if(mechanism.getAccount() != null) {
            account = mechanism.getAccount();
        } else {
            account = storageClient.getAccount(mechanism.getAccountId());
            processPoliciesForAccount(account);
        }

        return account;
    }

    boolean updateAccount(Account account) throws AccountLockException {
        Logger.debug(TAG, "Updating Account with ID '%s'", account.getId());

        // Check if Account is locked
        if (account.isLocked()) {
            throw new AccountLockException("This account is locked. It violates the following" +
                    " policy: " + account.getLockingPolicy());
        }

        // Update the account object if it already exist, otherwise return false
        Account oldAccount = storageClient.getAccount(account.getId());
        if (oldAccount != null)  {
            return storageClient.setAccount(account);
        } else {
            return false;
        }
    }

    boolean lockAccount(Account account, FRAPolicy policy) throws AccountLockException {
        if (account.isLocked()) {
            throw new AccountLockException("Error locking the Account: Account is already locked.");
        } else if (policy.getName() == null || policy.getName().isEmpty()) {
            throw new AccountLockException("Error locking the Account: The policy name is required.");
        } else if (!policyEvaluator.isPolicyAttached(account, policy.getName())) {
            throw new AccountLockException("Error locking the Account: The policy provided was not " +
                    "included during Account registration.");
        } else {
            account.lock(policy);
            return storageClient.setAccount(account);
        }
    }

    boolean unlockAccount(Account account) throws AccountLockException {
        if (!account.isLocked()) {
            throw new AccountLockException("Error unlocking the Account: Account is not locked.");
        } else {
            account.unlock();
            return storageClient.setAccount(account);
        }
    }

    Mechanism getMechanism(PushNotification notification) {
        String mechanismUID = notification.getMechanismUID();
        Logger.debug(TAG, "Retrieving Mechanism with ID '%s' from the StorageClient.", mechanismUID);

        // Retrieve mechanism from StorageClient
        Mechanism mechanism;
        if(notification.getPushMechanism() != null) {
            mechanism = notification.getPushMechanism();
        } else {
            mechanism = storageClient.getMechanismByUUID(mechanismUID);
        }

        if(mechanism.getAccount() == null) {
            mechanism.setAccount(storageClient.getAccount(mechanism.getAccountId()));
        }

        return mechanism;
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
        String mechanismUID = mechanism.getMechanismUID();
        Account account = this.getAccount(mechanism);
        Logger.debug(TAG, "Removing Mechanism with ID '%s' from the StorageClient.", mechanismUID);

        // If PushMechanism mechanism, remove any notifications associated with it
        if(mechanism.getType().equals(Mechanism.PUSH)) {
            List<PushNotification> notificationList = storageClient.getAllNotificationsForMechanism(mechanism);
            if(!notificationList.isEmpty()) {
                Logger.debug(TAG, "Removing Push Notifications for Mechanism with ID '%s' from the StorageClient.", mechanismUID);
                for (PushNotification notification : notificationList) {
                    storageClient.removeNotification(notification);
                }
            }
        }

        // Remove the mechanism itself
        boolean result = storageClient.removeMechanism(mechanism);

        // Remove account if no other mechanism exist
        if (storageClient.getMechanismsForAccount(account).isEmpty()) {
            storageClient.removeAccount(account);
        }

        return result;
    }

    boolean removeNotification(PushNotification notification) {
        return storageClient.removeNotification(notification);
    }

    PushNotification getNotification(String notificationId) {
        Logger.debug(TAG, "Retrieving PushNotification with ID '%s' from the StorageClient.", notificationId);

        // Retrieve notification from StorageClient
        PushNotification notification = storageClient.getNotification(notificationId);

        // Sets mechanism associated with this push notification
        if(notification != null) {
            PushMechanism mechanism = (PushMechanism) getMechanism(notification);
            notification.setPushMechanism(mechanism);
        }
        return notification;
    }

    void registerForRemoteNotifications(String newDeviceToken) throws AuthenticatorException {
        if(this.deviceToken == null) {
            this.deviceToken = newDeviceToken;
            this.pushFactory = new PushFactory(context, storageClient, newDeviceToken);
            this.notificationFactory = new NotificationFactory(storageClient);
            PushResponder.getInstance(storageClient);
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
        return notificationFactory.handleMessage(message);
    }

    PushNotification handleMessage(String messageId, String message)
            throws InvalidNotificationException {
        Logger.debug(TAG, "Processing FCM remote message.");
        return notificationFactory.handleMessage(messageId, message);
    }

    List<PushNotification> getAllNotifications() {
        List<PushNotification> notificationList =  storageClient.getAllNotifications();
        Collections.sort(notificationList);
        return notificationList;
    }

    List<PushNotification> getAllNotifications(@NonNull Mechanism mechanism) {
        if(mechanism.getType().equals(Mechanism.PUSH)) {
            List<PushNotification> notificationList = storageClient.getAllNotificationsForMechanism(mechanism);
            Collections.sort(notificationList);
            ((PushMechanism) mechanism).setPushNotificationList(notificationList);
            return notificationList;
        } else {
            return null;
        }
    }

    private void createCombinedMechanismsFromUri(String uri, FRAListener<Mechanism> listener) {
        // Check if account already exist
        List<Mechanism> mechanismList = returnDuplicatedMechanisms(uri);
        if (mechanismList != null && !mechanismList.isEmpty()) {
            listener.onException(new DuplicateMechanismException("Matching mechanism already exists",
                    mechanismList.get(0)));
            return;
        }

        // Check if security policies are available in the URI before proceed with registration
        Logger.debug(TAG, "Evaluating policies for the new Account");
        FRAPolicyEvaluator.Result result = policyEvaluator.evaluate(context, uri);
        if (!result.isComply()) {
            listener.onException(new MechanismPolicyViolationException("This account cannot be " +
                    "registered on this device. It violates the following policy: " +
                    result.getNonCompliancePolicy().getName(), result.getNonCompliancePolicy()));
            return;
        } else {
            Logger.debug(TAG, "All policies passed for the new Account");
        }

        // Register OATH mechanism passing combined parameters
        oathFactory.createFromUri(uri, new FRAListener<Mechanism>() {
            @Override
            public void onSuccess(Mechanism result) {
                Logger.debug(TAG, "OATH mechanism in Combined MFA successfully created.");
            }

            @Override
            public void onException(Exception e) {
                Logger.error(TAG, "Error creating OATH mechanism in Combined MFA.");
            }
        });

        // Register PUSH mechanism passing combined parameters
        pushFactory.createFromUri(uri, new FRAListener<Mechanism>() {
            @Override
            public void onSuccess(Mechanism result) {
                listener.onSuccess(result);
                Logger.debug(TAG, "PUSH mechanism in Combined MFA successfully created.");
            }

            @Override
            public void onException(Exception e) {
                listener.onException(e);
                Logger.error(TAG, "Error creating PUSH mechanism in Combined MFA.");
            }
        });
    }
    
    private void initializeAccount(Account account) {
        if(account != null) {
            processPoliciesForAccount(account);

            Logger.debug(TAG, "Loading associated mechanisms for the Account with ID: %s", account.getId());
            List<Mechanism> mechanismList = storageClient.getMechanismsForAccount(account);
            Collections.sort(mechanismList);
            account.setMechanismList(mechanismList);
            for (Mechanism mechanism : mechanismList) {
                mechanism.setAccount(account);
            }
        }
    }

    private void processPoliciesForAccount(Account account) {
        Logger.debug(TAG, "Evaluating policies for the new Account with ID: %s",
                account.getId());
        FRAPolicyEvaluator.Result result = policyEvaluator.evaluate(context, account);
        if(!account.isLocked() && !result.isComply()) {
            Logger.debug(TAG, "Lock Account due non-compliance policy with name: %s",
                    result.getNonCompliancePolicy().getName());
            account.lock(result.getNonCompliancePolicy());
            storageClient.setAccount(account);
        } else if(account.isLocked() && result.isComply()) {
            Logger.debug(TAG, "Unlock previously locked Account: All policies are compliance.");
            account.unlock();
            storageClient.setAccount(account);
        }
    }

    private List<Mechanism> returnDuplicatedMechanisms(String uri) {
        PushParser parser = new PushParser();
        Map<String, String> map;

        try {
            map = parser.map(uri);
            String id = map.get(MechanismParser.ISSUER) + "-" + map.get(MechanismParser.ACCOUNT_NAME);
            Account account = storageClient.getAccount(id);
            if (account != null) {
                return storageClient.getMechanismsForAccount(account);
            }
        } catch (MechanismParsingException e) {
            Logger.error(TAG, "Error parsing URI.", e);
        }

        return null;
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

