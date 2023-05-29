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

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.forgerock.android.auth.exception.AccountLockException;
import org.forgerock.android.auth.exception.AuthenticatorException;
import org.forgerock.android.auth.exception.InvalidNotificationException;
import org.forgerock.android.auth.exception.InvalidPolicyException;
import org.forgerock.android.auth.policy.FRAPolicy;

import java.util.List;

/**
 * The top level FRAClient object represents the Authenticator module of the ForgeRock
 * Mobile SDK. It is the front facing class where the configuration settings for the SDK can be
 * found and utilized.
 * <p>
 * To create a new FRAClient object use the static {@link FRAClient#builder()}.
 */
public class FRAClient {

    /** The Authenticator Manager instance. */
    private AuthenticatorManager authenticatorManager;

    private static final String TAG = FRAClient.class.getSimpleName();

    private FRAClient(AuthenticatorManager authenticatorManager) {
        this.authenticatorManager = authenticatorManager;
    }

    /**
     * Obtain FRAClient builder. Settings can be configured in the builder, like setting the FCM
     * token or StorageClient implementation.
     * @return FRAClientBuilder the builder to initialize the FRAClient
     * */
    public static FRAClientBuilder builder() {
        return new FRAClientBuilder();
    }

    /**
     * The asynchronous Authenticator client builder.
     */
    public static class FRAClientBuilder {
        private StorageClient storageClient;
        private String fcmToken;
        private Context context;
        private FRAPolicyEvaluator policyEvaluator;

        /**
         * Initialize the FRAClient instance with an Android Context.
         * @param context the context
         * @return this builder
         */
        public FRAClientBuilder withContext(@NonNull Context context) {
            this.context = context;
            return this;
        }

        /**
         * Initialize the FRAClient instance with a custom storage implementation. You can define
         * your own storage implementing {@link StorageClient} or use the default implementation
         * {@link DefaultStorageClient}.
         * @param storage the storage implementation
         * @return this builder
         */
        public FRAClientBuilder withStorage(@NonNull StorageClient storage) {
            this.storageClient = storage;
            return this;
        }

        /**
         * Initialize the FRAClient instance with the FCM device token obtained from FCM service
         * {@link FirebaseMessagingService}.
         * @param deviceToken the FCM device token
         * @return this builder
         */
        public FRAClientBuilder withDeviceToken(@NonNull String deviceToken) {
            this.fcmToken = deviceToken;
            return this;
        }

        /**
         * Initialize the FRAClient instance with your {@link FRAPolicyEvaluator}. You can define
         * your own Policy Evaluator with custom policies {@link FRAPolicy} or use the default
         * implementation.
         * @param policyEvaluator the custom Policy Evaluator
         * @return this builder
         */
        public FRAClientBuilder withPolicyEvaluator(@NonNull FRAPolicyEvaluator policyEvaluator) {
            this.policyEvaluator = policyEvaluator;
            return this;
        }

        /**
         * Initialize the authenticator client {@link FRAClient}.
         * @throws AuthenticatorException If {@link Context} was not provided
         */
        public FRAClient start() throws AuthenticatorException {
            if (context == null) {
                Logger.warn(TAG, "SDK cannot be initialized, no Context provided.");
                throw new AuthenticatorException("Must provide a valid context for the SDK initialization.");
            }

            if(storageClient == null) {
                Logger.warn(TAG, "No custom StoreClient provided, using DefaultStorageClient.");
                storageClient = new DefaultStorageClient(context);
            }

            if(policyEvaluator == null) {
                Logger.warn(TAG, "No custom FRAPolicyEvaluator provided, using default policies.");
                try {
                    policyEvaluator = FRAPolicyEvaluator.builder().build();
                } catch (InvalidPolicyException e) {
                    Logger.warn(TAG, "Error on building FRAPolicyEvaluator using default policies.");
                }
            }

            if (fcmToken == null) {
                Logger.warn(TAG, "A FCM token must be provided to handle Push Registrations. The method" +
                        " FRAClient#registerForRemoteNotifications can also be used to register the device token.");
            }

            return new FRAClient(new AuthenticatorManager(context, storageClient, policyEvaluator,
                    fcmToken));
        }

    }

    /**
     * Create a Mechanism using the URL extracted from the QRCode. This URL contains information about
     * the mechanism itself, as the account. After validation the mechanism will be persisted and returned
     * via the callback {@link FRAListener<Mechanism>}.
     * @param uri The URI extracted from the QRCode
     * @param listener Callback for receiving the mechanism registration result
     */
    public void createMechanismFromUri(@NonNull String uri, @NonNull FRAListener<Mechanism> listener) {
        this.authenticatorManager.createMechanismFromUri(uri, listener);
    }

    /**
     * Get all accounts stored in the system. Returns {@code null} if no Account could be found.
     * This method full initialize the {@link Mechanism} and/or {@link PushNotification} objects
     * associated with the accounts.
     * @return List<Account> The complete list of accounts stored on the system
     */
    public List<Account> getAllAccounts() {
        return this.authenticatorManager.getAllAccounts();
    }

    /**
     * Get the Account object with its id. Identifier of Account object is "<issuer>-<accountName>"
     * Returns {@code null} if Account could not be found.
     * This method full initialize the {@link Mechanism} objects associated with the account.
     * @param accountId The account unique ID
     * @return The account object
     */
    public Account getAccount(@NonNull String accountId) {
        return this.authenticatorManager.getAccount(accountId);
    }

    /**
     * Get the Account object with its associated Mechanism.  Returns {@code null} if Account could
     * not be found.
     * This method does not initialize the {@link Mechanism} and/or {@link PushNotification} objects
     * associated with the account.
     * @param mechanism The Mechanism object
     * @return The account object
     */
    public Account getAccount(@NonNull Mechanism mechanism) {
        return this.authenticatorManager.getAccount(mechanism);
    }

    /**
     * Update the {@link Account} object the storage system. Returns {@code false} if it could
     * not be found or updated.
     * @param account The Account to update.
     * @return boolean as result of the operation
     * @throws AccountLockException if account is locked
     */
    public boolean updateAccount(@NonNull Account account) throws AccountLockException {
        return this.authenticatorManager.updateAccount(account);
    }

    /**
     * Remove from the storage the {@link Account} that was passed in, all {@link Mechanism} objects
     * and any {@link PushNotification} objects associated with it.
     * @param account The account object to delete
     * @return boolean as result of the operation
     */
    public boolean removeAccount(@NonNull Account account) {
        return this.authenticatorManager.removeAccount(account);
    }

    /**
     * Lock the {@link Account} that was passed in, limiting the access to all {@link Mechanism}
     * objects and any {@link PushNotification} objects associated with it.
     * @param account The account object to lock
     * @param policy The non-compliance policy
     * @return boolean as result of the operation
     * @throws AccountLockException if account is already locked, policy name is invalid or
     * the policy was not attached to the account during registration
     */
    public boolean lockAccount(@NonNull Account account, @NonNull FRAPolicy policy)
            throws AccountLockException {
        return this.authenticatorManager.lockAccount(account, policy);
    }

    /**
     * Unlock the {@link Account} that was passed in.
     * @param account The account object to unlock
     * @return boolean as result of the operation
     * @throws AccountLockException if account is not locked
     */
    public boolean unlockAccount(@NonNull Account account) throws AccountLockException {
        return this.authenticatorManager.unlockAccount(account);
    }

    /**
     * Get the Mechanism object associated with the PushNotification object.  Returns {@code null}
     * if Mechanism could not be found.
     * This method full initialize the {@link PushNotification} objects associated with.
     * {@link PushMechanism} type
     * @param notification The uniquely identifiable UUID for the mechanism
     * @return The Mechanism object
     */
    public Mechanism getMechanism(@NonNull PushNotification notification) {
        return this.authenticatorManager.getMechanism(notification);
    }

    /**
     * Remove from the storage the {@link Mechanism} that was passed in and any notifications
     * associated with it.
     * @param mechanism The mechanism object to delete
     * @return boolean as result of the operation
     */
    public boolean removeMechanism(@NonNull Mechanism mechanism) {
        return this.authenticatorManager.removeMechanism(mechanism);
    }

    /**
     * Get single list of notifications across all mechanisms.
     * Returns {@code null} if no {@link PushNotification} could be found.
     * @return The complete list of notifications
     */
    public List<PushNotification> getAllNotifications() {
        return this.authenticatorManager.getAllNotifications();
    }

    /**
     * Get all of the notifications that belong to a {@link PushMechanism} object.
     * Returns {@code null} if no {@link PushNotification} could be found or the Mechanism type
     * is invalid.
     * This also updates the passed {@link PushMechanism} object with the list of
     * {@link PushNotification} objects associated with it.
     * @param mechanism The Mechanism object
     * @return The list of notifications
     */
    public List<PushNotification> getAllNotifications(@NonNull Mechanism mechanism) {
        return this.authenticatorManager.getAllNotifications(mechanism);
    }

    /**
     * Get the PushNotification object with its id. Identifier of PushNotification object is "<mechanismUUID>-<timeAdded>"
     * Returns {@code null} if PushNotification could not be found.
     * This method also sets the {@link Mechanism} object associated with the PushNotification.
     * @param notificationId The notification unique ID
     * @return The PushNotification object
     */
    public PushNotification getNotification(@NonNull String notificationId) {
        return this.authenticatorManager.getNotification(notificationId);
    }

    /**
     * Remove from the storage the {@link PushNotification} that was passed in.
     * @param notification The PushNotification object to delete
     * @return boolean as result of the operation
     */
    public boolean removeNotification(@NonNull PushNotification notification) {
        return this.authenticatorManager.removeNotification(notification);
    }

    /**
     * Receives a FCM remote message and covert into a {@link PushNotification} object,
     * which allows accept or deny Push Authentication requests.
     * @param message FCM remote message
     * @return PushNotification The notification configured with the information extracted the remote message
     * @throws InvalidNotificationException if the remote message does not contain required information
     */
    public PushNotification handleMessage(@NonNull RemoteMessage message)
            throws InvalidNotificationException {
        return this.authenticatorManager.handleMessage(message);
    }

    /**
     * Receives the parameters from a FCM remote message and covert into a {@link PushNotification}
     * object, which allows accept or deny Push Authentication requests.
     * @param messageId the 'messageId' attribute obtained from the {@link RemoteMessage} object
     * @param message the 'message' attribute obtained from the {@link RemoteMessage} object
     * @return PushNotification The notification configured with the information extracted the remote message
     * @throws InvalidNotificationException if the remote message does not contain required information
     */
    public PushNotification handleMessage(@NonNull String messageId, @NonNull String message)
            throws InvalidNotificationException {
        return this.authenticatorManager.handleMessage(messageId, message);
    }

    /**
     * This method allows to register the FCM device token to handle Push mechanisms after the
     * SDK initialization.
     * Note: This method cannot be used to handle FCM device token updates received on
     * {@link FirebaseMessagingService#onNewToken}. Currently, AM does not accept deviceToken updates
     * from the SDK. If any {@link PushMechanism} was registered with the previous token, this
     * mechanism needs to be removed and registered again using this new deviceToken.
     * @param deviceToken the FCM device token
     * @throws AuthenticatorException if the SDK was already initialized with a device token
     */
    public void registerForRemoteNotifications(@NonNull String deviceToken) throws AuthenticatorException {
        this.authenticatorManager.registerForRemoteNotifications(deviceToken);
    }

    /** No Public methods **/

    @VisibleForTesting
    AuthenticatorManager getAuthenticatorManagerInstance() {
        return this.authenticatorManager;
    }

}

