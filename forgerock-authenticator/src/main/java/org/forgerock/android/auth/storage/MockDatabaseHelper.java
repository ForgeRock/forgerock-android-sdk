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
import android.database.MatrixCursor;

import org.forgerock.android.auth.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * A mock implementation of DatabaseHelper for test environments that doesn't require native libraries.
 * This class is used as a fallback when SQLCipher is not available in the test environment.
 */
public class MockDatabaseHelper implements DatabaseHelper {
    private static final String TAG = MockDatabaseHelper.class.getSimpleName();

    private final Map<String, Map<String, String>> accountsTable = new HashMap<>();
    private final Map<String, Map<String, String>> mechanismsTable = new HashMap<>();
    private final Map<String, Map<String, String>> notificationsTable = new HashMap<>();
    private final Map<String, Map<String, String>> deviceTokenTable = new HashMap<>();
    
    private boolean inTransaction = false;
    
    public MockDatabaseHelper(Context context, String passphrase) {
        // Initialize empty tables
    }
    
    @Override
    public void execSQL(String sql) {
        // No-op for tests
        Logger.debug(TAG, "Executing SQL: " + sql);
    }
    
    @Override
    public Cursor query(String tableName, String[] projection, String selection, 
                      String[] selectionArgs, String groupBy, String having, String orderBy) {
        Map<String, Map<String, String>> tableMap = getTable(tableName);
        MatrixCursor cursor = new MatrixCursor(projection);
        
        if (selection != null && selection.contains("=")) {
            if (selection.contains(" AND ")) {
                // Compound selection like "issuer = ? AND account_name = ?"
                // Parse the selection string to identify the columns being filtered
                String[] parts = selection.split(" AND ");
                String column1 = parts[0].trim().split(" ")[0];
                String column2 = parts[1].trim().split(" ")[0];
                
                // Get the values for these columns
                String value1 = selectionArgs[0];
                String value2 = selectionArgs[1];
                
                // Find matching rows
                for (Map<String, String> row : tableMap.values()) {
                    if (value1.equals(row.get(column1)) && value2.equals(row.get(column2))) {
                        Object[] rowData = new Object[projection.length];
                        for (int i = 0; i < projection.length; i++) {
                            rowData[i] = row.get(projection[i]);
                        }
                        cursor.addRow(rowData);
                    }
                }
            } else {
                // Simple selection like "id = ?"
                String column = selection.split(" ")[0];
                String value = selectionArgs[0];
                
                if (column.equals(DatabaseSchema.COLUMN_ID)) {
                    // If using ID column, use direct map lookup
                    Map<String, String> row = tableMap.get(value);
                    if (row != null) {
                        Object[] rowData = new Object[projection.length];
                        for (int i = 0; i < projection.length; i++) {
                            rowData[i] = row.get(projection[i]);
                        }
                        cursor.addRow(rowData);
                    }
                } else {
                    // If using another column, scan all rows
                    for (Map<String, String> row : tableMap.values()) {
                        if (value.equals(row.get(column))) {
                            Object[] rowData = new Object[projection.length];
                            for (int i = 0; i < projection.length; i++) {
                                rowData[i] = row.get(projection[i]);
                            }
                            cursor.addRow(rowData);
                        }
                    }
                }
            }
        } else {
            // Get all rows
            for (Map<String, String> row : tableMap.values()) {
                Object[] rowData = new Object[projection.length];
                for (int i = 0; i < projection.length; i++) {
                    rowData[i] = row.get(projection[i]);
                }
                cursor.addRow(rowData);
            }
            Logger.debug(TAG, "  Returning all rows, count: %d", tableMap.size());
        }
        
        return cursor;
    }
    
    @Override
    public long insert(String tableName, String nullColumnHack, ContentValues values) {
        Map<String, Map<String, String>> tableMap = getTable(tableName);
        String id = values.getAsString(DatabaseSchema.COLUMN_ID);
        Map<String, String> row = new HashMap<>();
        
        for (String key : values.keySet()) {
            row.put(key, values.getAsString(key));
        }
        
        tableMap.put(id, row);
        return 1; // Return success
    }
    
    @Override
    public int update(String tableName, ContentValues values, String whereClause, String[] whereArgs) {
        Map<String, Map<String, String>> tableMap = getTable(tableName);
        if (whereClause != null && whereClause.contains("=")) {
            String id = whereArgs[0];
            Map<String, String> row = tableMap.get(id);
            if (row != null) {
                for (String key : values.keySet()) {
                    row.put(key, values.getAsString(key));
                }
                return 1; // Return count of updated rows
            }
        }
        return 0;
    }
    
    @Override
    public int delete(String tableName, String whereClause, String[] whereArgs) {
        Map<String, Map<String, String>> tableMap = getTable(tableName);
        if (whereClause == null) {
            // Delete all rows
            int count = tableMap.size();
            tableMap.clear();
            return count;
        } else if (whereClause.contains("=")) {
            String id = whereArgs[0];
            if (tableMap.remove(id) != null) {
                return 1; // Return count of deleted rows
            }
        }
        return 0;
    }
    
    private Map<String, Map<String, String>> getTable(String tableName) {
        switch (tableName) {
            case DatabaseSchema.TABLE_ACCOUNTS:
                return accountsTable;
            case DatabaseSchema.TABLE_MECHANISMS:
                return mechanismsTable;
            case DatabaseSchema.TABLE_NOTIFICATIONS:
                return notificationsTable;
            case DatabaseSchema.TABLE_DEVICE_TOKEN:
                return deviceTokenTable;
            default:
                throw new IllegalArgumentException("Unknown table: " + tableName);
        }
    }
    
    // Transaction support
    @Override
    public void beginTransaction() {
        inTransaction = true;
    }
    
    @Override
    public void setTransactionSuccessful() {
        // No-op for mock
    }
    
    @Override
    public void endTransaction() {
        inTransaction = false;
    }
    
    @Override
    public void createTablesIfNeeded() {
        // No-op for mock, tables are already created as maps
    }
    
    // No migration functionality needed as we're using the new schema directly
    
    @Override
    public void close() {
        // No-op for tests
    }
}
