/*
 * Copyright (c) 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.VisibleForTesting;

import java.util.List;

/**
 * Manager responsible for migrating data from DefaultStorageClient to SQLStorageClient.
 * This class provides methods to automatically detect and perform migration
 * when the FRAClient is initialized with SQLStorageClient.
 */
public class StorageMigrationManager {

    private static final String TAG = StorageMigrationManager.class.getSimpleName();
    private static final String MIGRATION_PREFERENCES = "org.forgerock.android.authenticator.MIGRATION";
    private static final String KEY_HAS_MIGRATED = "hasMigrated";

    private final Context context;
    private final DefaultStorageClient sourceStorage;
    private final SQLStorageClient targetStorage;

    /**
     * Create an instance of StorageMigrationManager to handle
     * data migration between storage implementations.
     *
     * @param context The application context
     * @param targetStorage The SQLStorageClient to migrate data to
     */
    public StorageMigrationManager(Context context, SQLStorageClient targetStorage) {
        this.context = context;
        this.sourceStorage = createSourceStorage(context);
        this.targetStorage = targetStorage;
    }
    
    /**
     * Creates the source storage client.
     * This method can be overridden for testing purposes.
     *
     * @param context The application context
     * @return A new DefaultStorageClient instance
     */
    @VisibleForTesting
    protected DefaultStorageClient createSourceStorage(Context context) {
        return new DefaultStorageClient(context);
    }

    /**
     * Check if a migration is needed from DefaultStorageClient to SQLStorageClient.
     * Migration is needed if DefaultStorageClient has data and no migration has been performed yet.
     *
     * @return true if migration is needed, false otherwise
     */
    public boolean isMigrationNeeded() {
        // Check if we've already migrated
        SharedPreferences migrationPrefs = context.getSharedPreferences(
                MIGRATION_PREFERENCES, Context.MODE_PRIVATE);
        boolean hasMigrated = migrationPrefs.getBoolean(KEY_HAS_MIGRATED, false);
        
        if (hasMigrated) {
            Logger.debug(TAG, "Migration has already been performed.");
            return false;
        }
        
        // Check if there's data in DefaultStorageClient
        boolean hasData = !sourceStorage.isEmpty();
        
        if (hasData) {
            Logger.debug(TAG, "Migration needed: DefaultStorageClient contains data.");
            return true;
        }
        
        // Mark as migrated if there's no data to migrate
        setMigrationComplete();
        Logger.debug(TAG, "No migration needed: DefaultStorageClient is empty.");
        return false;
    }
    
    /**
     * Perform migration from DefaultStorageClient to SQLStorageClient.
     * This will transfer all Accounts, Mechanisms, Notifications and the DeviceToken.
     *
     * @param deleteSourceAfterMigration Whether to delete data from DefaultStorageClient after migration
     * @return true if migration was successful, false otherwise
     */
    public boolean migrateData(boolean deleteSourceAfterMigration) {
        Logger.debug(TAG, "Starting migration from DefaultStorageClient to SQLStorageClient");
        
        try {
            // Step 1: Migrate accounts and their associated mechanisms
            List<Account> accounts = sourceStorage.getAllAccounts();
            if (accounts != null && !accounts.isEmpty()) {
                Logger.debug(TAG, "Migrating " + accounts.size() + " accounts");
                for (Account account : accounts) {
                    Logger.debug(TAG, "Migrating account: " + account.getId());
                    boolean accountSuccess = targetStorage.setAccount(account);
                    if (!accountSuccess) {
                        Logger.error(TAG, "Failed to migrate account: " + account.getId());
                        return false;
                    }
                    
                    // Migrate mechanisms for this account
                    List<Mechanism> mechanisms = sourceStorage.getMechanismsForAccount(account);
                    if (mechanisms != null && !mechanisms.isEmpty()) {
                        Logger.debug(TAG, "Migrating " + mechanisms.size() + " mechanisms for account: " + account.getId());
                        for (Mechanism mechanism : mechanisms) {
                            boolean mechanismSuccess = targetStorage.setMechanism(mechanism);
                            if (!mechanismSuccess) {
                                Logger.error(TAG, "Failed to migrate mechanism: " + mechanism.getId());
                                return false;
                            }
                            
                            // Migrate notifications for this mechanism (only if it's a push mechanism)
                            if (mechanism.getType() != null && mechanism.getType().equals(Mechanism.PUSH) && mechanism.getMechanismUID() != null) {
                                List<PushNotification> notifications = sourceStorage.getAllNotificationsForMechanism(mechanism);
                                if (notifications != null && !notifications.isEmpty()) {
                                    Logger.debug(TAG, "Migrating " + notifications.size() + " notifications for push mechanism: " + mechanism.getMechanismUID());
                                    for (PushNotification notification : notifications) {
                                        boolean notificationSuccess = targetStorage.setNotification(notification);
                                        if (!notificationSuccess) {
                                            Logger.error(TAG, "Failed to migrate notification: " + notification.getId());
                                            return false;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Step 2: Migrate device token
            PushDeviceToken deviceToken = sourceStorage.getPushDeviceToken();
            if (deviceToken != null) {
                Logger.debug(TAG, "Migrating device token");
                boolean tokenSuccess = targetStorage.setPushDeviceToken(deviceToken);
                if (!tokenSuccess) {
                    Logger.error(TAG, "Failed to migrate device token");
                    return false;
                }
            }
            
            // Mark migration as complete
            setMigrationComplete();
            
            // Step 3: Optionally delete data from the source storage
            if (deleteSourceAfterMigration) {
                Logger.debug(TAG, "Deleting data from DefaultStorageClient after migration");
                sourceStorage.removeAll();
            }
            
            Logger.info(TAG, "Migration completed successfully");
            return true;
        } catch (Exception e) {
            Logger.error(TAG, "Migration failed: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Mark migration as complete to prevent repeated migrations.
     */
    private void setMigrationComplete() {
        SharedPreferences migrationPrefs = context.getSharedPreferences(
                MIGRATION_PREFERENCES, Context.MODE_PRIVATE);
        migrationPrefs.edit().putBoolean(KEY_HAS_MIGRATED, true).apply();
    }
    
    /**
     * Reset the migration status. This is primarily for testing purposes.
     */
    @VisibleForTesting
    public void resetMigrationStatus() {
        SharedPreferences migrationPrefs = context.getSharedPreferences(
                MIGRATION_PREFERENCES, Context.MODE_PRIVATE);
        migrationPrefs.edit().putBoolean(KEY_HAS_MIGRATED, false).apply();
    }
}
