/*
 * Copyright (c) 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.storage;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Interface for database operations.
 * Used to abstract the actual database implementation (SQLCipher or in-memory for tests).
 */
public interface DatabaseHelper {
    
    /**
     * Query the database.
     *
     * @param tableName The table name to compile the query against.
     * @param projection A list of which columns to return. Passing null will return all columns.
     * @param selection A filter declaring which rows to return. Passing null will return all rows.
     * @param selectionArgs You may include ?s in selection, which will be replaced by the values from selectionArgs.
     * @param groupBy A filter declaring how to group rows. Passing null will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include. Passing null will include all row groups.
     * @param orderBy How to order the rows. Passing null will use the default sort order.
     * @return A Cursor over all rows matching the query.
     */
    Cursor query(String tableName, String[] projection, String selection, String[] selectionArgs,
                String groupBy, String having, String orderBy);
    
    /**
     * Insert a row into the database.
     *
     * @param tableName The table to insert the row into.
     * @param nullColumnHack SQL doesn't allow inserting a completely empty row, so this parameter allows inserting an empty row.
     * @param values The values to insert into the row.
     * @return The row ID of the newly inserted row, or -1 if an error occurred.
     */
    long insert(String tableName, String nullColumnHack, ContentValues values);
    
    /**
     * Update rows in the database.
     *
     * @param tableName The table to update.
     * @param values The values to update the row(s) with.
     * @param whereClause The WHERE clause to apply when updating.
     * @param whereArgs Arguments for the WHERE clause.
     * @return The number of rows affected.
     */
    int update(String tableName, ContentValues values, String whereClause, String[] whereArgs);
    
    /**
     * Delete rows from the database.
     *
     * @param tableName The table to delete rows from.
     * @param whereClause The WHERE clause to apply when deleting.
     * @param whereArgs Arguments for the WHERE clause.
     * @return The number of rows affected.
     */
    int delete(String tableName, String whereClause, String[] whereArgs);
    
    /**
     * Execute a single SQL statement that is NOT a SELECT or any other SQL statement that returns data.
     *
     * @param sql The SQL statement to execute.
     */
    void execSQL(String sql);
    
    /**
     * Begin a transaction.
     */
    void beginTransaction();
    
    /**
     * Mark the current transaction as successful.
     */
    void setTransactionSuccessful();
    
    /**
     * End the current transaction.
     */
    void endTransaction();
    
    /**
     * Close the database.
     */
    void close();
    
    /**
     * Creates the database tables if they don't exist.
     * Uses the latest schema version.
     */
    void createTablesIfNeeded();
}
