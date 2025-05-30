/*
 * Copyright (c) 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;

import net.sqlcipher.database.SQLiteDatabase;

import org.forgerock.android.auth.exception.MechanismCreationException;
import org.forgerock.android.auth.storage.CipherDatabaseHelper;
import org.forgerock.android.auth.storage.DatabaseHelper;
import org.forgerock.android.auth.storage.DatabaseSchema;
import org.forgerock.android.auth.storage.MockDatabaseHelper;
import org.forgerock.android.auth.storage.PassphraseManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Data Access Object which implements StorageClient interface and uses SQLCipher for 
 * encrypted database storage of Accounts, Mechanisms and Notifications.
 * Uses a passphrase that is securely generated and managed.
 */
public class SQLStorageClient implements StorageClient {

    // The tag for logging
    private static final String TAG = SQLStorageClient.class.getSimpleName();
    
    // Constants for database
    private static final String DATABASE_NAME = "forgerock_authenticator.db";
    private static final String DEVICE_TOKEN_ID = "deviceToken";
    
    // The database helper
    private final DatabaseHelper databaseHelper;
    
    // The passphrase manager
    private final PassphraseManager passphraseManager;

    /**
     * Constructor for SQLStorageClient.
     *
     * @param context The application context.
     */
    public SQLStorageClient(@NonNull Context context) {
        // Initialize the passphrase manager
        this.passphraseManager = new PassphraseManager(context);
        
        // Get or create the database passphrase
        String passphrase = passphraseManager.getOrCreatePassphrase();
        
        // Check if running in test environment
        boolean isTestEnvironment = isRunningInTestEnvironment();
        
        if (isTestEnvironment) {
            Logger.debug(TAG, "Using test database implementation");
            this.databaseHelper = new MockDatabaseHelper(context, passphrase);
        } else {
            // Initialize SQLCipher
            SQLiteDatabase.loadLibs(context);
            
            // Initialize the database helper
            this.databaseHelper = new CipherDatabaseHelper(context, passphrase);
        }
        
        // Create database tables if they don't exist or upgrade if needed
        databaseHelper.createTablesIfNeeded();
    }
    
    /**
     * Determines if the application is running in a test environment.
     *
     * @return True if running in a test environment, false otherwise.
     */
    protected boolean isRunningInTestEnvironment() {
        try {
            Class.forName("org.robolectric.Robolectric");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public Account getAccount(String accountId) {
        // Define the columns to retrieve
        String[] projection = {
                DatabaseSchema.COLUMN_ID,
                DatabaseSchema.COLUMN_ISSUER,
                DatabaseSchema.COLUMN_DISPLAY_ISSUER,
                DatabaseSchema.COLUMN_ACCOUNT_NAME,
                DatabaseSchema.COLUMN_DISPLAY_ACCOUNT_NAME,
                DatabaseSchema.COLUMN_IMAGE_URL,
                DatabaseSchema.COLUMN_BACKGROUND_COLOR,
                DatabaseSchema.COLUMN_TIME_ADDED,
                DatabaseSchema.COLUMN_POLICIES,
                DatabaseSchema.COLUMN_LOCKING_POLICY,
                DatabaseSchema.COLUMN_IS_LOCKED
        };
        
        // Define the selection criteria
        String selection = DatabaseSchema.COLUMN_ID + " = ?";
        String[] selectionArgs = {accountId};
        
        Cursor cursor = databaseHelper.query(
                DatabaseSchema.TABLE_ACCOUNTS,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        
        Account account = null;
        if (cursor != null && cursor.moveToFirst()) {
            account = createAccountFromCursor(cursor);
            cursor.close();
        }
        
        return account;
    }

    @Override
    public List<Account> getAllAccounts() {
        List<Account> accountList = new ArrayList<>();
        
        // Define the columns to retrieve
        String[] projection = {
                DatabaseSchema.COLUMN_ID,
                DatabaseSchema.COLUMN_ISSUER,
                DatabaseSchema.COLUMN_DISPLAY_ISSUER,
                DatabaseSchema.COLUMN_ACCOUNT_NAME,
                DatabaseSchema.COLUMN_DISPLAY_ACCOUNT_NAME,
                DatabaseSchema.COLUMN_IMAGE_URL,
                DatabaseSchema.COLUMN_BACKGROUND_COLOR,
                DatabaseSchema.COLUMN_TIME_ADDED,
                DatabaseSchema.COLUMN_POLICIES,
                DatabaseSchema.COLUMN_LOCKING_POLICY,
                DatabaseSchema.COLUMN_IS_LOCKED
        };
        
        Cursor cursor = databaseHelper.query(
                DatabaseSchema.TABLE_ACCOUNTS,
                projection,
                null,
                null,
                null,
                null,
                null
        );
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Account account = createAccountFromCursor(cursor);
                if (account != null) {
                    accountList.add(account);
                }
            }
            cursor.close();
        }
        
        return accountList;
    }

    @Override
    public boolean removeAccount(Account account) {
        String selection = DatabaseSchema.COLUMN_ID + " = ?";
        String[] selectionArgs = {account.getId()};
        
        int deletedRows = databaseHelper.delete(DatabaseSchema.TABLE_ACCOUNTS, selection, selectionArgs);
        return deletedRows > 0;
    }

    @Override
    public boolean setAccount(Account account) {
        // Use column-based approach
        ContentValues values = createContentValuesFromAccount(account);
        
        // Check if account already exists
        Account existingAccount = getAccount(account.getId());
        
        long result;
        if (existingAccount == null) {
            // Insert new account
            result = databaseHelper.insert(DatabaseSchema.TABLE_ACCOUNTS, null, values);
        } else {
            // Update existing account
            String selection = DatabaseSchema.COLUMN_ID + " = ?";
            String[] selectionArgs = {account.getId()};
            result = databaseHelper.update(DatabaseSchema.TABLE_ACCOUNTS, values, selection, selectionArgs);
        }
        
        return result != -1;
    }

    @Override
    public List<Mechanism> getMechanismsForAccount(Account account) {
        Logger.debug(TAG, "Retrieving mechanisms for account: Issuer=%s, AccountName=%s", 
                account.getIssuer(), account.getAccountName());
        List<Mechanism> mechanismList = new ArrayList<>();
        
        // Use query to match mechanisms with this account by issuer and account name
        String selection = DatabaseSchema.COLUMN_ISSUER + " = ? AND " + DatabaseSchema.COLUMN_ACCOUNT_NAME + " = ?";
        String[] selectionArgs = {account.getIssuer(), account.getAccountName()};
        
        String[] projection = {
                DatabaseSchema.COLUMN_ID,
                DatabaseSchema.COLUMN_MECHANISM_UID,
                DatabaseSchema.COLUMN_ISSUER,
                DatabaseSchema.COLUMN_ACCOUNT_NAME,
                DatabaseSchema.COLUMN_TYPE,
                DatabaseSchema.COLUMN_SECRET,
                DatabaseSchema.COLUMN_UID,
                DatabaseSchema.COLUMN_RESOURCE_ID,
                DatabaseSchema.COLUMN_TIME_ADDED,
                DatabaseSchema.COLUMN_MECHANISM_TYPE,
                DatabaseSchema.COLUMN_ALGORITHM,
                DatabaseSchema.COLUMN_DIGITS,
                DatabaseSchema.COLUMN_COUNTER,
                DatabaseSchema.COLUMN_PERIOD,
                DatabaseSchema.COLUMN_REGISTRATION_ENDPOINT,
                DatabaseSchema.COLUMN_AUTHENTICATION_ENDPOINT
        };
        
        Cursor cursor = databaseHelper.query(
                DatabaseSchema.TABLE_MECHANISMS,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Mechanism mechanism = createMechanismFromCursor(cursor);
                if (mechanism != null) {
                    Logger.debug(TAG, "Found mechanism in DB: UID=%s, Issuer=%s, AccountName=%s", mechanism.getMechanismUID(), mechanism.getIssuer(), mechanism.getAccountName());
                    mechanismList.add(mechanism);
                }
            }
            cursor.close();
        }
        Logger.debug(TAG, "Total mechanisms found: %d", mechanismList.size());
        return mechanismList;
    }
    
    /**
     * Get all mechanisms stored in the system.
     *
     * @return The complete list of mechanisms.
     */
    private List<Mechanism> getAllMechanisms() {
        List<Mechanism> mechanismList = new ArrayList<>();
        
        // Define columns to retrieve
        String[] projection = {
                DatabaseSchema.COLUMN_ID,
                DatabaseSchema.COLUMN_MECHANISM_UID,
                DatabaseSchema.COLUMN_ISSUER,
                DatabaseSchema.COLUMN_ACCOUNT_NAME,
                DatabaseSchema.COLUMN_TYPE,
                DatabaseSchema.COLUMN_SECRET,
                DatabaseSchema.COLUMN_UID,
                DatabaseSchema.COLUMN_RESOURCE_ID,
                DatabaseSchema.COLUMN_TIME_ADDED,
                DatabaseSchema.COLUMN_MECHANISM_TYPE,
                DatabaseSchema.COLUMN_ALGORITHM,
                DatabaseSchema.COLUMN_DIGITS,
                DatabaseSchema.COLUMN_COUNTER,
                DatabaseSchema.COLUMN_PERIOD,
                DatabaseSchema.COLUMN_REGISTRATION_ENDPOINT,
                DatabaseSchema.COLUMN_AUTHENTICATION_ENDPOINT
        };
        
        Cursor cursor = databaseHelper.query(
                DatabaseSchema.TABLE_MECHANISMS,
                projection,
                null,
                null,
                null,
                null,
                null
        );
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Mechanism mechanism = createMechanismFromCursor(cursor);
                if (mechanism != null) {
                    mechanismList.add(mechanism);
                }
            }
            cursor.close();
        }
        
        return mechanismList;
    }

    @Override
    public Mechanism getMechanismByUUID(String mechanismUID) {
        // Use direct query
        String selection = DatabaseSchema.COLUMN_MECHANISM_UID + " = ?";
        String[] selectionArgs = {mechanismUID};
        
        String[] projection = {
                DatabaseSchema.COLUMN_ID,
                DatabaseSchema.COLUMN_MECHANISM_UID,
                DatabaseSchema.COLUMN_ISSUER,
                DatabaseSchema.COLUMN_ACCOUNT_NAME,
                DatabaseSchema.COLUMN_TYPE,
                DatabaseSchema.COLUMN_SECRET,
                DatabaseSchema.COLUMN_UID,
                DatabaseSchema.COLUMN_RESOURCE_ID,
                DatabaseSchema.COLUMN_TIME_ADDED,
                DatabaseSchema.COLUMN_MECHANISM_TYPE,
                DatabaseSchema.COLUMN_ALGORITHM,
                DatabaseSchema.COLUMN_DIGITS,
                DatabaseSchema.COLUMN_COUNTER,
                DatabaseSchema.COLUMN_PERIOD,
                DatabaseSchema.COLUMN_REGISTRATION_ENDPOINT,
                DatabaseSchema.COLUMN_AUTHENTICATION_ENDPOINT
        };
        
        Cursor cursor = databaseHelper.query(
                DatabaseSchema.TABLE_MECHANISMS,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        
        Mechanism mechanism = null;
        if (cursor != null && cursor.moveToFirst()) {
            mechanism = createMechanismFromCursor(cursor);
            cursor.close();
        }
        
        return mechanism;
    }

    @Override
    public boolean removeMechanism(Mechanism mechanism) {
        String selection = DatabaseSchema.COLUMN_ID + " = ?";
        String[] selectionArgs = {mechanism.getId()};
        
        int deletedRows = databaseHelper.delete(DatabaseSchema.TABLE_MECHANISMS, selection, selectionArgs);
        return deletedRows > 0;
    }

    @Override
    public boolean setMechanism(Mechanism mechanism) {
        Logger.debug(TAG, "Storing mechanism: UID=%s, Issuer=%s, AccountName=%s, ID=%s", 
                mechanism.getMechanismUID(), mechanism.getIssuer(), mechanism.getAccountName(), mechanism.getId());
        ContentValues values = createContentValuesFromMechanism(mechanism);
        Mechanism existingMechanism = getMechanismById(mechanism.getId());
        long result;
        if (existingMechanism == null) {
            // Insert new mechanism
            result = databaseHelper.insert(DatabaseSchema.TABLE_MECHANISMS, null, values);
            Logger.debug(TAG, "Inserted new mechanism, result=%d", result);
        } else {
            // Update existing mechanism
            String selection = DatabaseSchema.COLUMN_ID + " = ?";
            String[] selectionArgs = {mechanism.getId()};
            result = databaseHelper.update(DatabaseSchema.TABLE_MECHANISMS, values, selection, selectionArgs);
            Logger.debug(TAG, "Updated existing mechanism, result=%d", result);
        }
        
        // Double check that the mechanism was stored correctly
        Mechanism storedMechanism = getMechanismById(mechanism.getId());
        if (storedMechanism != null) {
            Logger.debug(TAG, "Mechanism stored successfully: ID=%s, UID=%s, Issuer=%s, AccountName=%s", 
                    storedMechanism.getId(), storedMechanism.getMechanismUID(), 
                    storedMechanism.getIssuer(), storedMechanism.getAccountName());
        } else {
            Logger.debug(TAG, "Failed to find mechanism after storage: ID=%s", mechanism.getId());
        }
        
        return result != -1;
    }
    
    /**
     * Get a mechanism by its ID.
     *
     * @param mechanismId The ID of the mechanism.
     * @return The mechanism object.
     */
    private Mechanism getMechanismById(String mechanismId) {
        String[] projection = {
                DatabaseSchema.COLUMN_ID,
                DatabaseSchema.COLUMN_MECHANISM_UID,
                DatabaseSchema.COLUMN_ISSUER,
                DatabaseSchema.COLUMN_ACCOUNT_NAME,
                DatabaseSchema.COLUMN_TYPE,
                DatabaseSchema.COLUMN_SECRET,
                DatabaseSchema.COLUMN_UID,
                DatabaseSchema.COLUMN_RESOURCE_ID,
                DatabaseSchema.COLUMN_TIME_ADDED,
                DatabaseSchema.COLUMN_MECHANISM_TYPE,
                DatabaseSchema.COLUMN_ALGORITHM,
                DatabaseSchema.COLUMN_DIGITS,
                DatabaseSchema.COLUMN_COUNTER,
                DatabaseSchema.COLUMN_PERIOD,
                DatabaseSchema.COLUMN_REGISTRATION_ENDPOINT,
                DatabaseSchema.COLUMN_AUTHENTICATION_ENDPOINT
        };
        
        String selection = DatabaseSchema.COLUMN_ID + " = ?";
        String[] selectionArgs = {mechanismId};
        
        Cursor cursor = databaseHelper.query(
                DatabaseSchema.TABLE_MECHANISMS,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        
        Mechanism mechanism = null;
        if (cursor != null && cursor.moveToFirst()) {
            mechanism = createMechanismFromCursor(cursor);
            cursor.close();
        }
        
        return mechanism;
    }

    @Override
    public List<PushNotification> getAllNotifications() {
        List<PushNotification> pushNotificationList = new ArrayList<>();
        
        String[] projection = {
                DatabaseSchema.COLUMN_ID,
                DatabaseSchema.COLUMN_MECHANISM_UID,
                DatabaseSchema.COLUMN_MESSAGE_ID,
                DatabaseSchema.COLUMN_MESSAGE,
                DatabaseSchema.COLUMN_CHALLENGE,
                DatabaseSchema.COLUMN_AMLB_COOKIE,
                DatabaseSchema.COLUMN_TIME_ADDED,
                DatabaseSchema.COLUMN_TIME_EXPIRED,
                DatabaseSchema.COLUMN_TTL,
                DatabaseSchema.COLUMN_IS_APPROVED,
                DatabaseSchema.COLUMN_IS_PENDING,
                DatabaseSchema.COLUMN_CUSTOM_PAYLOAD,
                DatabaseSchema.COLUMN_NUMBERS_CHALLENGE,
                DatabaseSchema.COLUMN_CONTEXT_INFO,
                DatabaseSchema.COLUMN_PUSH_TYPE
        };
        
        Cursor cursor = databaseHelper.query(
                DatabaseSchema.TABLE_NOTIFICATIONS,
                projection,
                null,
                null,
                null,
                null,
                null
        );
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                PushNotification pushNotification = createNotificationFromCursor(cursor);
                if (pushNotification != null) {
                    pushNotificationList.add(pushNotification);
                }
            }
            cursor.close();
        }
        
        return pushNotificationList;
    }

    @Override
    public List<PushNotification> getAllNotificationsForMechanism(Mechanism mechanism) {
        List<PushNotification> pushNotificationList = new ArrayList<>();
        
        // Use direct query
        String selection = DatabaseSchema.COLUMN_MECHANISM_UID + " = ?";
        String[] selectionArgs = {mechanism.getMechanismUID()};
        
        String[] projection = {
                DatabaseSchema.COLUMN_ID,
                DatabaseSchema.COLUMN_MECHANISM_UID,
                DatabaseSchema.COLUMN_MESSAGE_ID,
                DatabaseSchema.COLUMN_MESSAGE,
                DatabaseSchema.COLUMN_CHALLENGE,
                DatabaseSchema.COLUMN_AMLB_COOKIE,
                DatabaseSchema.COLUMN_TIME_ADDED,
                DatabaseSchema.COLUMN_TIME_EXPIRED,
                DatabaseSchema.COLUMN_TTL,
                DatabaseSchema.COLUMN_IS_APPROVED,
                DatabaseSchema.COLUMN_IS_PENDING,
                DatabaseSchema.COLUMN_CUSTOM_PAYLOAD,
                DatabaseSchema.COLUMN_NUMBERS_CHALLENGE,
                DatabaseSchema.COLUMN_CONTEXT_INFO,
                DatabaseSchema.COLUMN_PUSH_TYPE
        };
        
        Cursor cursor = databaseHelper.query(
                DatabaseSchema.TABLE_NOTIFICATIONS,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                PushNotification pushNotification = createNotificationFromCursor(cursor);
                if (pushNotification != null) {
                    pushNotification.setPushMechanism(mechanism);
                    pushNotificationList.add(pushNotification);
                }
            }
            cursor.close();
        }
        
        return pushNotificationList;
    }

    @Override
    public boolean removeNotification(PushNotification pushNotification) {
        String selection = DatabaseSchema.COLUMN_ID + " = ?";
        String[] selectionArgs = {pushNotification.getId()};
        
        int deletedRows = databaseHelper.delete(DatabaseSchema.TABLE_NOTIFICATIONS, selection, selectionArgs);
        return deletedRows > 0;
    }

    @Override
    public boolean setNotification(PushNotification pushNotification) {
        // Use column-based approach
        ContentValues values = createContentValuesFromNotification(pushNotification);
        
        // Check if notification already exists
        PushNotification existingNotification = getNotification(pushNotification.getId());
        
        long result;
        if (existingNotification == null) {
            // Insert new notification
            result = databaseHelper.insert(DatabaseSchema.TABLE_NOTIFICATIONS, null, values);
        } else {
            // Update existing notification
            String selection = DatabaseSchema.COLUMN_ID + " = ?";
            String[] selectionArgs = {pushNotification.getId()};
            result = databaseHelper.update(DatabaseSchema.TABLE_NOTIFICATIONS, values, selection, selectionArgs);
        }
        
        return result != -1;
    }

    @Override
    public PushNotification getNotification(String notificationId) {
        String[] projection = {
                DatabaseSchema.COLUMN_ID,
                DatabaseSchema.COLUMN_MECHANISM_UID,
                DatabaseSchema.COLUMN_MESSAGE_ID,
                DatabaseSchema.COLUMN_MESSAGE,
                DatabaseSchema.COLUMN_CHALLENGE,
                DatabaseSchema.COLUMN_AMLB_COOKIE,
                DatabaseSchema.COLUMN_TIME_ADDED,
                DatabaseSchema.COLUMN_TIME_EXPIRED,
                DatabaseSchema.COLUMN_TTL,
                DatabaseSchema.COLUMN_IS_APPROVED,
                DatabaseSchema.COLUMN_IS_PENDING,
                DatabaseSchema.COLUMN_CUSTOM_PAYLOAD,
                DatabaseSchema.COLUMN_NUMBERS_CHALLENGE,
                DatabaseSchema.COLUMN_CONTEXT_INFO,
                DatabaseSchema.COLUMN_PUSH_TYPE
        };
        
        String selection = DatabaseSchema.COLUMN_ID + " = ?";
        String[] selectionArgs = {notificationId};
        
        Cursor cursor = databaseHelper.query(
                DatabaseSchema.TABLE_NOTIFICATIONS,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        
        PushNotification pushNotification = null;
        if (cursor != null && cursor.moveToFirst()) {
            pushNotification = createNotificationFromCursor(cursor);
            cursor.close();
        }
        
        return pushNotification;
    }

    @Override
    public PushNotification getNotificationByMessageId(String messageId) {
        // Use direct query
        String selection = DatabaseSchema.COLUMN_MESSAGE_ID + " = ?";
        String[] selectionArgs = {messageId};
        
        String[] projection = {
                DatabaseSchema.COLUMN_ID,
                DatabaseSchema.COLUMN_MECHANISM_UID,
                DatabaseSchema.COLUMN_MESSAGE_ID,
                DatabaseSchema.COLUMN_MESSAGE,
                DatabaseSchema.COLUMN_CHALLENGE,
                DatabaseSchema.COLUMN_AMLB_COOKIE,
                DatabaseSchema.COLUMN_TIME_ADDED,
                DatabaseSchema.COLUMN_TIME_EXPIRED,
                DatabaseSchema.COLUMN_TTL,
                DatabaseSchema.COLUMN_IS_APPROVED,
                DatabaseSchema.COLUMN_IS_PENDING,
                DatabaseSchema.COLUMN_CUSTOM_PAYLOAD,
                DatabaseSchema.COLUMN_NUMBERS_CHALLENGE,
                DatabaseSchema.COLUMN_CONTEXT_INFO,
                DatabaseSchema.COLUMN_PUSH_TYPE
        };
        
        Cursor cursor = databaseHelper.query(
                DatabaseSchema.TABLE_NOTIFICATIONS,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        
        PushNotification pushNotification = null;
        if (cursor != null && cursor.moveToFirst()) {
            pushNotification = createNotificationFromCursor(cursor);
            cursor.close();
        }
        
        return pushNotification;
    }

    @Override
    public boolean setPushDeviceToken(PushDeviceToken pushDeviceToken) {
        // Use column-based approach
        ContentValues values = new ContentValues();
        values.put(DatabaseSchema.COLUMN_ID, DEVICE_TOKEN_ID);
        values.put(DatabaseSchema.COLUMN_TOKEN_ID, pushDeviceToken.getTokenId());
        values.put(DatabaseSchema.COLUMN_TIME_ADDED, pushDeviceToken.getTimeAdded().getTimeInMillis());
        
        // Check if device token already exists
        PushDeviceToken existingDeviceToken = getPushDeviceToken();
        
        long result;
        if (existingDeviceToken == null) {
            // Insert new device token
            result = databaseHelper.insert(DatabaseSchema.TABLE_DEVICE_TOKEN, null, values);
        } else {
            // Update existing device token
            String selection = DatabaseSchema.COLUMN_ID + " = ?";
            String[] selectionArgs = {DEVICE_TOKEN_ID};
            result = databaseHelper.update(DatabaseSchema.TABLE_DEVICE_TOKEN, values, selection, selectionArgs);
        }
        
        return result != -1;
    }

    @Override
    public PushDeviceToken getPushDeviceToken() {
        String[] projection = {
                DatabaseSchema.COLUMN_ID,
                DatabaseSchema.COLUMN_TOKEN_ID,
                DatabaseSchema.COLUMN_TIME_ADDED
        };
        
        String selection = DatabaseSchema.COLUMN_ID + " = ?";
        String[] selectionArgs = {DEVICE_TOKEN_ID};
        
        Cursor cursor = databaseHelper.query(
                DatabaseSchema.TABLE_DEVICE_TOKEN,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        
        PushDeviceToken pushDeviceToken = null;
        if (cursor != null && cursor.moveToFirst()) {
            String tokenId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_TOKEN_ID));
            long timeAdded = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_TIME_ADDED));
            
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timeAdded);
            
            pushDeviceToken = new PushDeviceToken(tokenId, calendar);
            cursor.close();
        }
        
        return pushDeviceToken;
    }

    @Override
    public boolean isEmpty() {
        // Check if accounts table is empty
        Cursor cursor = databaseHelper.query(
                DatabaseSchema.TABLE_ACCOUNTS,
                new String[]{DatabaseSchema.COLUMN_ID},
                null,
                null,
                null,
                null,
                null
        );
        
        boolean isEmpty = true;
        if (cursor != null) {
            isEmpty = !cursor.moveToFirst();
            cursor.close();
        }
        
        // Check other tables if accounts is empty
        if (isEmpty) {
            cursor = databaseHelper.query(
                    DatabaseSchema.TABLE_MECHANISMS,
                    new String[]{DatabaseSchema.COLUMN_ID},
                    null,
                    null,
                    null,
                    null,
                    null
            );
            if (cursor != null) {
                isEmpty = !cursor.moveToFirst();
                cursor.close();
            }
        }
        
        if (isEmpty) {
            cursor = databaseHelper.query(
                    DatabaseSchema.TABLE_NOTIFICATIONS,
                    new String[]{DatabaseSchema.COLUMN_ID},
                    null,
                    null,
                    null,
                    null,
                    null
            );
            if (cursor != null) {
                isEmpty = !cursor.moveToFirst();
                cursor.close();
            }
        }
        
        if (isEmpty) {
            cursor = databaseHelper.query(
                    DatabaseSchema.TABLE_DEVICE_TOKEN,
                    new String[]{DatabaseSchema.COLUMN_ID},
                    null,
                    null,
                    null,
                    null,
                    null
            );
            if (cursor != null) {
                isEmpty = !cursor.moveToFirst();
                cursor.close();
            }
        }
        
        return isEmpty;
    }

    /**
     * Remove all the stored data.
     */
    public void removeAll() {
        databaseHelper.delete(DatabaseSchema.TABLE_ACCOUNTS, null, null);
        databaseHelper.delete(DatabaseSchema.TABLE_MECHANISMS, null, null);
        databaseHelper.delete(DatabaseSchema.TABLE_NOTIFICATIONS, null, null);
        databaseHelper.delete(DatabaseSchema.TABLE_DEVICE_TOKEN, null, null);
    }

    /**
     * Creates an Account object from database cursor.
     * 
     * @param cursor The database cursor positioned at the account record.
     * @return The Account object or null if creation failed.
     */
    private Account createAccountFromCursor(Cursor cursor) {
        try {
            String issuer = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ISSUER));
            String accountName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ACCOUNT_NAME));
            
            // Get optional fields
            String displayIssuer = null;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_DISPLAY_ISSUER))) {
                displayIssuer = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_DISPLAY_ISSUER));
            }
            
            String displayAccountName = null;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_DISPLAY_ACCOUNT_NAME))) {
                displayAccountName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_DISPLAY_ACCOUNT_NAME));
            }
            
            String imageURL = null;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_IMAGE_URL))) {
                imageURL = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_IMAGE_URL));
            }
            
            String backgroundColor = null;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_BACKGROUND_COLOR))) {
                backgroundColor = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_BACKGROUND_COLOR));
            }
            
            Calendar timeAdded = null;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_TIME_ADDED))) {
                timeAdded = Calendar.getInstance();
                timeAdded.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_TIME_ADDED)));
            }
            
            String policies = null;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_POLICIES))) {
                policies = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_POLICIES));
            }
            
            String lockingPolicy = null;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_LOCKING_POLICY))) {
                lockingPolicy = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_LOCKING_POLICY));
            }
            
            boolean lock = false;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_IS_LOCKED))) {
                lock = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_IS_LOCKED)) == 1;
            }
            
            // Build the account
            return Account.builder()
                    .setIssuer(issuer)
                    .setAccountName(accountName)
                    .setDisplayIssuer(displayIssuer)
                    .setDisplayAccountName(displayAccountName)
                    .setImageURL(imageURL)
                    .setBackgroundColor(backgroundColor)
                    .setTimeAdded(timeAdded)
                    .setPolicies(policies)
                    .setLockingPolicy(lockingPolicy)
                    .setLock(lock)
                    .build();
        } catch (Exception e) {
            Logger.error(TAG, "Error creating Account from cursor", e);
            return null;
        }
    }

    /**
     * Creates a ContentValues object from an Account for database storage.
     * 
     * @param account The Account object to convert.
     * @return ContentValues for database insert/update.
     */
    private ContentValues createContentValuesFromAccount(Account account) {
        ContentValues values = new ContentValues();
        values.put(DatabaseSchema.COLUMN_ID, account.getId());
        values.put(DatabaseSchema.COLUMN_ISSUER, account.getIssuer());
        values.put(DatabaseSchema.COLUMN_ACCOUNT_NAME, account.getAccountName());
        
        if (account.getDisplayIssuer() != null && !account.getDisplayIssuer().equals(account.getIssuer())) {
            values.put(DatabaseSchema.COLUMN_DISPLAY_ISSUER, account.getDisplayIssuer());
        }
        
        if (account.getDisplayAccountName() != null && !account.getDisplayAccountName().equals(account.getAccountName())) {
            values.put(DatabaseSchema.COLUMN_DISPLAY_ACCOUNT_NAME, account.getDisplayAccountName());
        }
        
        if (account.getImageURL() != null) {
            values.put(DatabaseSchema.COLUMN_IMAGE_URL, account.getImageURL());
        }
        
        if (account.getBackgroundColor() != null) {
            values.put(DatabaseSchema.COLUMN_BACKGROUND_COLOR, account.getBackgroundColor());
        }
        
        if (account.getTimeAdded() != null) {
            values.put(DatabaseSchema.COLUMN_TIME_ADDED, account.getTimeAdded().getTimeInMillis());
        } else {
            values.put(DatabaseSchema.COLUMN_TIME_ADDED, Calendar.getInstance().getTimeInMillis());
        }
        
        if (account.getPolicies() != null) {
            values.put(DatabaseSchema.COLUMN_POLICIES, account.getPolicies());
        }
        
        if (account.getLockingPolicy() != null) {
            values.put(DatabaseSchema.COLUMN_LOCKING_POLICY, account.getLockingPolicy());
        }
        
        values.put(DatabaseSchema.COLUMN_IS_LOCKED, account.isLocked() ? 1 : 0);
        
        return values;
    }
    
    /**
     * Creates a Mechanism object from database cursor.
     * 
     * @param cursor The database cursor positioned at the mechanism record.
     * @return The Mechanism object or null if creation failed.
     */
    private Mechanism createMechanismFromCursor(Cursor cursor) {
        try {
            String mechanismUID = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_MECHANISM_UID));
            String issuer = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ISSUER));
            String accountName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ACCOUNT_NAME));
            String type = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_TYPE));
            String secret = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_SECRET));
            
            // Get optional fields
            String uid = null;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_UID))) {
                uid = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_UID));
            }
            
            String resourceId = null;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_RESOURCE_ID))) {
                resourceId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_RESOURCE_ID));
            }
            
            Calendar timeAdded = null;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_TIME_ADDED))) {
                timeAdded = Calendar.getInstance();
                timeAdded.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_TIME_ADDED)));
            }
            
            // Determine mechanism type
            String mechanismType = null;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_MECHANISM_TYPE))) {
                mechanismType = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_MECHANISM_TYPE));
            }
            
            // Create the appropriate mechanism based on type
            if (mechanismType != null) {
                if (mechanismType.equals("HOTP")) {
                    // Create HOTP mechanism
                    String algorithm = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ALGORITHM));
                    int digits = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_DIGITS));
                    long counter = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_COUNTER));
                    
                    return HOTPMechanism.builder()
                            .setMechanismUID(mechanismUID)
                            .setIssuer(issuer)
                            .setAccountName(accountName)
                            .setSecret(secret)
                            .setUid(uid)
                            .setResourceId(resourceId)
                            .setAlgorithm(algorithm)
                            .setDigits(digits)
                            .setCounter(counter)
                            .setTimeAdded(timeAdded)
                            .build();
                } else if (mechanismType.equals("TOTP")) {
                    // Create TOTP mechanism
                    String algorithm = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ALGORITHM));
                    int digits = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_DIGITS));
                    int period = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_PERIOD));
                    
                    return TOTPMechanism.builder()
                            .setMechanismUID(mechanismUID)
                            .setIssuer(issuer)
                            .setAccountName(accountName)
                            .setSecret(secret)
                            .setUid(uid)
                            .setResourceId(resourceId)
                            .setAlgorithm(algorithm)
                            .setDigits(digits)
                            .setPeriod(period)
                            .setTimeAdded(timeAdded)
                            .build();
                } else if (mechanismType.equals("PUSH")) {
                    // Create Push mechanism
                    String registrationEndpoint = null;
                    if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_REGISTRATION_ENDPOINT))) {
                        registrationEndpoint = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_REGISTRATION_ENDPOINT));
                    }
                    
                    String authenticationEndpoint = null;
                    if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_AUTHENTICATION_ENDPOINT))) {
                        authenticationEndpoint = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_AUTHENTICATION_ENDPOINT));
                    }
                    
                    try {
                        return PushMechanism.builder()
                                .setMechanismUID(mechanismUID)
                                .setIssuer(issuer)
                                .setAccountName(accountName)
                                .setSecret(secret)
                                .setUid(uid)
                                .setResourceId(resourceId)
                                .setRegistrationEndpoint(registrationEndpoint)
                                .setAuthenticationEndpoint(authenticationEndpoint)
                                .setTimeAdded(timeAdded)
                                .build();
                    } catch (MechanismCreationException e) {
                        Logger.error(TAG, "Error creating PushMechanism", e);
                        return null;
                    }
                }
            }
            
            // If we couldn't determine the type or it's not a known type
            return null;
        } catch (Exception e) {
            Logger.error(TAG, "Error creating Mechanism from cursor", e);
            return null;
        }
    }
    
    /**
     * Creates ContentValues from a Mechanism for database storage.
     * 
     * @param mechanism The Mechanism object to convert.
     * @return ContentValues for database insert/update.
     */
    private ContentValues createContentValuesFromMechanism(Mechanism mechanism) {
        ContentValues values = new ContentValues();
        values.put(DatabaseSchema.COLUMN_ID, mechanism.getId());
        values.put(DatabaseSchema.COLUMN_MECHANISM_UID, mechanism.getMechanismUID());
        values.put(DatabaseSchema.COLUMN_ISSUER, mechanism.getIssuer());
        values.put(DatabaseSchema.COLUMN_ACCOUNT_NAME, mechanism.getAccountName());
        values.put(DatabaseSchema.COLUMN_TYPE, mechanism.getType());
        values.put(DatabaseSchema.COLUMN_SECRET, mechanism.getSecret());
        
        if (mechanism.getUid() != null) {
            values.put(DatabaseSchema.COLUMN_UID, mechanism.getUid());
        }
        
        if (mechanism.getResourceId() != null) {
            values.put(DatabaseSchema.COLUMN_RESOURCE_ID, mechanism.getResourceId());
        }
        
        if (mechanism.getTimeAdded() != null) {
            values.put(DatabaseSchema.COLUMN_TIME_ADDED, mechanism.getTimeAdded().getTimeInMillis());
        }
        
        // Handle specific mechanism types
        if (mechanism instanceof HOTPMechanism) {
            HOTPMechanism hotpMechanism = (HOTPMechanism) mechanism;
            values.put(DatabaseSchema.COLUMN_MECHANISM_TYPE, "HOTP");
            values.put(DatabaseSchema.COLUMN_ALGORITHM, hotpMechanism.getAlgorithm());
            values.put(DatabaseSchema.COLUMN_DIGITS, hotpMechanism.getDigits());
            values.put(DatabaseSchema.COLUMN_COUNTER, hotpMechanism.getCounter());
        } else if (mechanism instanceof TOTPMechanism) {
            TOTPMechanism totpMechanism = (TOTPMechanism) mechanism;
            values.put(DatabaseSchema.COLUMN_MECHANISM_TYPE, "TOTP");
            values.put(DatabaseSchema.COLUMN_ALGORITHM, totpMechanism.getAlgorithm());
            values.put(DatabaseSchema.COLUMN_DIGITS, totpMechanism.getDigits());
            values.put(DatabaseSchema.COLUMN_PERIOD, (int) totpMechanism.getPeriod());
        } else if (mechanism instanceof PushMechanism) {
            PushMechanism pushMechanism = (PushMechanism) mechanism;
            values.put(DatabaseSchema.COLUMN_MECHANISM_TYPE, "PUSH");
            values.put(DatabaseSchema.COLUMN_REGISTRATION_ENDPOINT, pushMechanism.getRegistrationEndpoint());
            values.put(DatabaseSchema.COLUMN_AUTHENTICATION_ENDPOINT, pushMechanism.getAuthenticationEndpoint());
        }
        
        return values;
    }
    
    /**
     * Creates a PushNotification object from database cursor.
     * 
     * @param cursor The database cursor positioned at the notification record.
     * @return The PushNotification object or null if creation failed.
     */
    private PushNotification createNotificationFromCursor(Cursor cursor) {
        try {
            String mechanismUID = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_MECHANISM_UID));
            String messageId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_MESSAGE_ID));
            String challenge = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_CHALLENGE));
            long timeAddedMillis = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_TIME_ADDED));
            
            Calendar timeAdded = Calendar.getInstance();
            timeAdded.setTimeInMillis(timeAddedMillis);
            
            // Optional fields
            String message = null;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_MESSAGE))) {
                message = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_MESSAGE));
            }
            
            String amlbCookie = null;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_AMLB_COOKIE))) {
                amlbCookie = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_AMLB_COOKIE));
            }
            
            Calendar timeExpired = null;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_TIME_EXPIRED))) {
                timeExpired = Calendar.getInstance();
                timeExpired.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_TIME_EXPIRED)));
            }
            
            long ttl = 0;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_TTL))) {
                ttl = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_TTL));
            }
            
            boolean approved = false;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_IS_APPROVED))) {
                approved = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_IS_APPROVED)) == 1;
            }
            
            boolean pending = true;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_IS_PENDING))) {
                pending = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_IS_PENDING)) == 1;
            }
            
            String customPayload = null;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_CUSTOM_PAYLOAD))) {
                customPayload = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_CUSTOM_PAYLOAD));
            }
            
            String numbersChallenge = null;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_NUMBERS_CHALLENGE))) {
                numbersChallenge = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_NUMBERS_CHALLENGE));
            }
            
            String contextInfo = null;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_CONTEXT_INFO))) {
                contextInfo = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_CONTEXT_INFO));
            }
            
            String pushTypeStr = null;
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_PUSH_TYPE))) {
                pushTypeStr = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_PUSH_TYPE));
            }
            
            // Build notification
            return PushNotification.builder()
                    .setMechanismUID(mechanismUID)
                    .setMessageId(messageId)
                    .setMessage(message)
                    .setChallenge(challenge)
                    .setAmlbCookie(amlbCookie)
                    .setTimeAdded(timeAdded)
                    .setTimeExpired(timeExpired)
                    .setTtl(ttl)
                    .setApproved(approved)
                    .setPending(pending)
                    .setCustomPayload(customPayload)
                    .setNumbersChallenge(numbersChallenge)
                    .setContextInfo(contextInfo)
                    .setPushType(pushTypeStr)
                    .build();
        } catch (Exception e) {
            Logger.error(TAG, "Error creating PushNotification from cursor", e);
            return null;
        }
    }
    
    /**
     * Creates ContentValues from a PushNotification for database storage.
     * 
     * @param notification The PushNotification object to convert.
     * @return ContentValues for database insert/update.
     */
    private ContentValues createContentValuesFromNotification(PushNotification notification) {
        ContentValues values = new ContentValues();
        values.put(DatabaseSchema.COLUMN_ID, notification.getId());
        values.put(DatabaseSchema.COLUMN_MECHANISM_UID, notification.getMechanismUID());
        values.put(DatabaseSchema.COLUMN_MESSAGE_ID, notification.getMessageId());
        values.put(DatabaseSchema.COLUMN_CHALLENGE, notification.getChallenge());
        values.put(DatabaseSchema.COLUMN_TIME_ADDED, notification.getTimeAdded().getTimeInMillis());
        
        if (notification.getMessage() != null) {
            values.put(DatabaseSchema.COLUMN_MESSAGE, notification.getMessage());
        }
        
        if (notification.getAmlbCookie() != null) {
            values.put(DatabaseSchema.COLUMN_AMLB_COOKIE, notification.getAmlbCookie());
        }
        
        if (notification.getTimeExpired() != null) {
            values.put(DatabaseSchema.COLUMN_TIME_EXPIRED, notification.getTimeExpired().getTimeInMillis());
        }
        
        values.put(DatabaseSchema.COLUMN_TTL, notification.getTtl());
        values.put(DatabaseSchema.COLUMN_IS_APPROVED, notification.isApproved() ? 1 : 0);
        values.put(DatabaseSchema.COLUMN_IS_PENDING, notification.isPending() ? 1 : 0);
        
        if (notification.getCustomPayload() != null) {
            values.put(DatabaseSchema.COLUMN_CUSTOM_PAYLOAD, notification.getCustomPayload());
        }
        
        // Get the raw string value for numbers challenge from the notification's field
        try {
            java.lang.reflect.Field field = PushNotification.class.getDeclaredField("numbersChallenge");
            field.setAccessible(true);
            String numbersChallengeStr = (String) field.get(notification);
            if (numbersChallengeStr != null) {
                values.put(DatabaseSchema.COLUMN_NUMBERS_CHALLENGE, numbersChallengeStr);
            }
        } catch (Exception e) {
            Logger.warn(TAG, "Unable to access numbersChallenge field: " + e.getMessage());
        }
        
        if (notification.getContextInfo() != null) {
            values.put(DatabaseSchema.COLUMN_CONTEXT_INFO, notification.getContextInfo());
        }
        
        if (notification.getPushType() != null) {
            values.put(DatabaseSchema.COLUMN_PUSH_TYPE, notification.getPushType().name());
        }
        
        return values;
    }

}
