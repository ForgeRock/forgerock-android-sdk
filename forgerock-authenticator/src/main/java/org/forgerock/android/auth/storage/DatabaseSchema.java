/*
 * Copyright (c) 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.storage;

/**
 * Database schema constants used throughout the application.
 * This class contains table and column names, as well as SQL statements
 * for creating the database tables.
 */
public final class DatabaseSchema {

    private DatabaseSchema() {
        // Private constructor to prevent instantiation
    }
    
    // Table Names
    public static final String TABLE_ACCOUNTS = "accounts";
    public static final String TABLE_MECHANISMS = "mechanisms";
    public static final String TABLE_NOTIFICATIONS = "notifications";
    public static final String TABLE_DEVICE_TOKEN = "device_token";
    
    // Common Column Names
    public static final String COLUMN_ID = "id";
    
    // Account Columns
    public static final String COLUMN_ISSUER = "issuer";
    public static final String COLUMN_DISPLAY_ISSUER = "display_issuer";
    public static final String COLUMN_ACCOUNT_NAME = "account_name";
    public static final String COLUMN_DISPLAY_ACCOUNT_NAME = "display_account_name";
    public static final String COLUMN_IMAGE_URL = "image_url";
    public static final String COLUMN_BACKGROUND_COLOR = "background_color";
    public static final String COLUMN_TIME_ADDED = "time_added";
    public static final String COLUMN_POLICIES = "policies";
    public static final String COLUMN_LOCKING_POLICY = "locking_policy";
    public static final String COLUMN_IS_LOCKED = "is_locked";
    
    // Mechanism Columns
    public static final String COLUMN_MECHANISM_UID = "mechanism_uid";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_SECRET = "secret";
    public static final String COLUMN_UID = "uid";
    public static final String COLUMN_RESOURCE_ID = "resource_id";
    public static final String COLUMN_MECHANISM_TYPE = "mechanism_type";
    public static final String COLUMN_ALGORITHM = "algorithm";
    public static final String COLUMN_DIGITS = "digits";
    public static final String COLUMN_COUNTER = "counter";
    public static final String COLUMN_PERIOD = "period";
    public static final String COLUMN_REGISTRATION_ENDPOINT = "registration_endpoint";
    public static final String COLUMN_AUTHENTICATION_ENDPOINT = "authentication_endpoint";
    
    // Notification Columns
    public static final String COLUMN_MESSAGE_ID = "message_id";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_CHALLENGE = "challenge";
    public static final String COLUMN_AMLB_COOKIE = "amlb_cookie";
    public static final String COLUMN_TIME_EXPIRED = "time_expired";
    public static final String COLUMN_TTL = "ttl";
    public static final String COLUMN_IS_APPROVED = "is_approved";
    public static final String COLUMN_IS_PENDING = "is_pending";
    public static final String COLUMN_CUSTOM_PAYLOAD = "custom_payload";
    public static final String COLUMN_NUMBERS_CHALLENGE = "numbers_challenge";
    public static final String COLUMN_CONTEXT_INFO = "context_info";
    public static final String COLUMN_PUSH_TYPE = "push_type";
    
    // Device Token Columns
    public static final String COLUMN_TOKEN_ID = "token_id";
    
    // SQL for creating tables with constraints and indices
    public static final String SQL_CREATE_ACCOUNTS_TABLE = 
            "CREATE TABLE " + TABLE_ACCOUNTS + " (" +
                    COLUMN_ID + " TEXT PRIMARY KEY," +
                    COLUMN_ISSUER + " TEXT NOT NULL," +
                    COLUMN_DISPLAY_ISSUER + " TEXT," +
                    COLUMN_ACCOUNT_NAME + " TEXT NOT NULL," +
                    COLUMN_DISPLAY_ACCOUNT_NAME + " TEXT," +
                    COLUMN_IMAGE_URL + " TEXT," +
                    COLUMN_BACKGROUND_COLOR + " TEXT," +
                    COLUMN_TIME_ADDED + " INTEGER," +
                    COLUMN_POLICIES + " TEXT," +
                    COLUMN_LOCKING_POLICY + " TEXT," +
                    COLUMN_IS_LOCKED + " INTEGER DEFAULT 0)";
    
    public static final String SQL_CREATE_MECHANISMS_TABLE = 
            "CREATE TABLE " + TABLE_MECHANISMS + " (" +
                    COLUMN_ID + " TEXT PRIMARY KEY," +
                    COLUMN_MECHANISM_UID + " TEXT NOT NULL UNIQUE," +
                    COLUMN_ISSUER + " TEXT NOT NULL," +
                    COLUMN_ACCOUNT_NAME + " TEXT NOT NULL," +
                    COLUMN_TYPE + " TEXT NOT NULL," +
                    COLUMN_SECRET + " TEXT NOT NULL," +
                    COLUMN_UID + " TEXT," +
                    COLUMN_RESOURCE_ID + " TEXT," +
                    COLUMN_TIME_ADDED + " INTEGER," +
                    COLUMN_MECHANISM_TYPE + " TEXT NOT NULL," +
                    COLUMN_ALGORITHM + " TEXT," +
                    COLUMN_DIGITS + " INTEGER," +
                    COLUMN_COUNTER + " INTEGER," +
                    COLUMN_PERIOD + " INTEGER," +
                    COLUMN_REGISTRATION_ENDPOINT + " TEXT," +
                    COLUMN_AUTHENTICATION_ENDPOINT + " TEXT," +
                    "FOREIGN KEY (" + COLUMN_ISSUER + ", " + COLUMN_ACCOUNT_NAME + ") " +
                    "REFERENCES " + TABLE_ACCOUNTS + "(" + COLUMN_ISSUER + ", " + COLUMN_ACCOUNT_NAME + ") " +
                    "ON DELETE CASCADE)";
    
    public static final String SQL_CREATE_NOTIFICATIONS_TABLE = 
            "CREATE TABLE " + TABLE_NOTIFICATIONS + " (" +
                    COLUMN_ID + " TEXT PRIMARY KEY," +
                    COLUMN_MECHANISM_UID + " TEXT NOT NULL," +
                    COLUMN_MESSAGE_ID + " TEXT NOT NULL," +
                    COLUMN_MESSAGE + " TEXT," +
                    COLUMN_CHALLENGE + " TEXT NOT NULL," +
                    COLUMN_AMLB_COOKIE + " TEXT," +
                    COLUMN_TIME_ADDED + " INTEGER NOT NULL," +
                    COLUMN_TIME_EXPIRED + " INTEGER," +
                    COLUMN_TTL + " INTEGER," +
                    COLUMN_IS_APPROVED + " INTEGER DEFAULT 0," +
                    COLUMN_IS_PENDING + " INTEGER DEFAULT 1," +
                    COLUMN_CUSTOM_PAYLOAD + " TEXT," +
                    COLUMN_NUMBERS_CHALLENGE + " TEXT," +
                    COLUMN_CONTEXT_INFO + " TEXT," +
                    COLUMN_PUSH_TYPE + " TEXT," +
                    "FOREIGN KEY (" + COLUMN_MECHANISM_UID + ") " +
                    "REFERENCES " + TABLE_MECHANISMS + "(" + COLUMN_MECHANISM_UID + ") " +
                    "ON DELETE CASCADE)";
    
    public static final String SQL_CREATE_DEVICE_TOKEN_TABLE = 
            "CREATE TABLE " + TABLE_DEVICE_TOKEN + " (" +
                    COLUMN_ID + " TEXT PRIMARY KEY," +
                    COLUMN_TOKEN_ID + " TEXT NOT NULL," +
                    COLUMN_TIME_ADDED + " INTEGER NOT NULL)";
    
    // SQL for creating indices to improve query performance
    public static final String SQL_CREATE_ACCOUNT_INDEX = 
            "CREATE INDEX idx_account_issuer_name ON " + TABLE_ACCOUNTS + 
            "(" + COLUMN_ISSUER + ", " + COLUMN_ACCOUNT_NAME + ")";
    
    public static final String SQL_CREATE_MECHANISM_INDEX = 
            "CREATE INDEX idx_mechanism_uid ON " + TABLE_MECHANISMS + 
            "(" + COLUMN_MECHANISM_UID + ")";
            
    public static final String SQL_CREATE_MECHANISM_ACCOUNT_INDEX = 
            "CREATE INDEX idx_mechanism_account ON " + TABLE_MECHANISMS + 
            "(" + COLUMN_ISSUER + ", " + COLUMN_ACCOUNT_NAME + ")";
    
    public static final String SQL_CREATE_NOTIFICATION_MECHANISM_INDEX = 
            "CREATE INDEX idx_notification_mechanism ON " + TABLE_NOTIFICATIONS + 
            "(" + COLUMN_MECHANISM_UID + ")";
            
    public static final String SQL_CREATE_NOTIFICATION_TIME_INDEX = 
            "CREATE INDEX idx_notification_time ON " + TABLE_NOTIFICATIONS + 
            "(" + COLUMN_TIME_ADDED + ")";
            
    public static final String SQL_CREATE_NOTIFICATION_PENDING_INDEX = 
            "CREATE INDEX idx_notification_pending ON " + TABLE_NOTIFICATIONS + 
            "(" + COLUMN_IS_PENDING + ")";
    
    // Statements to enable foreign key constraints
    public static final String SQL_ENABLE_FOREIGN_KEYS = "PRAGMA foreign_keys = ON;";
    
    // Statement to optimize database after schema creation
    public static final String SQL_ANALYZE = "ANALYZE;";
}
