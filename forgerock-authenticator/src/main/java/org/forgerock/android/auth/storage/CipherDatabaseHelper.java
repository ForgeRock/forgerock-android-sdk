/*
 * Copyright (c) 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

import org.forgerock.android.auth.Logger;

/**
 * SQLCipher database helper that implements the DatabaseHelper interface.
 */
public class CipherDatabaseHelper extends SQLiteOpenHelper implements DatabaseHelper {
    private static final String TAG = CipherDatabaseHelper.class.getSimpleName();
    private final String passphrase;
    private static final String DATABASE_NAME = "forgerock_authenticator.db";
    private static final int DATABASE_VERSION = 1;
    
    public CipherDatabaseHelper(Context context, String passphrase) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.passphrase = passphrase;
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        Logger.debug(TAG, "Creating SQL database");
        // Create tables with the column-based schema
        db.execSQL(DatabaseSchema.SQL_CREATE_ACCOUNTS_TABLE);
        db.execSQL(DatabaseSchema.SQL_CREATE_MECHANISMS_TABLE);
        db.execSQL(DatabaseSchema.SQL_CREATE_NOTIFICATIONS_TABLE);
        db.execSQL(DatabaseSchema.SQL_CREATE_DEVICE_TOKEN_TABLE);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // No migration needed since old schema hasn't been released yet
        // Just recreate tables
        db.execSQL("DROP TABLE IF EXISTS " + DatabaseSchema.TABLE_ACCOUNTS);
        db.execSQL("DROP TABLE IF EXISTS " + DatabaseSchema.TABLE_MECHANISMS);
        db.execSQL("DROP TABLE IF EXISTS " + DatabaseSchema.TABLE_NOTIFICATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + DatabaseSchema.TABLE_DEVICE_TOKEN);
        onCreate(db);
    }
    
    @Override
    public void createTablesIfNeeded() {
        SQLiteDatabase db = getWritableDatabase();
        
        // Check if tables exist
        try {
            Cursor cursor = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                    new String[] { DatabaseSchema.TABLE_ACCOUNTS });
            
            boolean tablesExist = cursor != null && cursor.getCount() > 0;
            
            if (cursor != null) {
                cursor.close();
            }
            
            if (!tablesExist) {
                onCreate(db);
            }
        } catch (Exception e) {
            Logger.warn(TAG, "Error checking if tables exist: " + e.getMessage());
            onCreate(db);
        }
    }
    
    public SQLiteDatabase getWritableDatabase() {
        return super.getWritableDatabase(passphrase);
    }
    
    public SQLiteDatabase getReadableDatabase() {
        return super.getReadableDatabase(passphrase);
    }

    @Override
    public Cursor query(String tableName, String[] projection, String selection, String[] selectionArgs,
                      String groupBy, String having, String orderBy) {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(tableName, projection, selection, selectionArgs, groupBy, having, orderBy);
    }

    @Override
    public long insert(String tableName, String nullColumnHack, ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        return db.insert(tableName, nullColumnHack, values);
    }

    @Override
    public int update(String tableName, ContentValues values, String whereClause, String[] whereArgs) {
        SQLiteDatabase db = getWritableDatabase();
        return db.update(tableName, values, whereClause, whereArgs);
    }

    @Override
    public int delete(String tableName, String whereClause, String[] whereArgs) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(tableName, whereClause, whereArgs);
    }

    @Override
    public void execSQL(String sql) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(sql);
    }
    
    @Override
    public void beginTransaction() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
    }
    
    @Override
    public void setTransactionSuccessful() {
        SQLiteDatabase db = getWritableDatabase();
        db.setTransactionSuccessful();
    }
    
    @Override
    public void endTransaction() {
        SQLiteDatabase db = getWritableDatabase();
        db.endTransaction();
    }
}
