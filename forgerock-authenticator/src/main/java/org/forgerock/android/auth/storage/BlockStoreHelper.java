/*
 * Copyright (c) 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.storage;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.blockstore.Blockstore;
import com.google.android.gms.auth.blockstore.BlockstoreClient;
import com.google.android.gms.auth.blockstore.DeleteBytesRequest;
import com.google.android.gms.auth.blockstore.RetrieveBytesRequest;
import com.google.android.gms.auth.blockstore.RetrieveBytesResponse;
import com.google.android.gms.auth.blockstore.StoreBytesData;
import com.google.android.gms.tasks.Tasks;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * This class is used store and retrieve data from Google Block Store. Data is automatically
 * encrypted by Google and can be backed up to the cloud and restored across devices if the user
 * has backup enabled on their Google account.
 */
public class BlockStoreHelper {

    private static final String TAG = BlockStoreHelper.class.getSimpleName();
    private final BlockstoreClient client;
    private final boolean defaultShouldBackupToCloud = true;
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();


    /**
     * Constructor for BlockStoreHelper.
     * Initializes the BlockstoreClient using the provided application context.
     *
     * @param context The application context.
     */
    public BlockStoreHelper(@NonNull Context context) {
        this.client = Blockstore.getClient(context.getApplicationContext());
    }

    /**
     * Stores a byte array in Google's Block Store.
     * IMPORTANT: This method uses Tasks.await() and MUST be called from a background thread.
     *
     * @param key                 The key to store the data under.
     * @param data                The byte array to store.
     * @param shouldBackupToCloud If true, data will be backed up.
     * @return True if successful, false otherwise.
     */
    public boolean storeBytes(@NonNull String key, @NonNull byte[] data, boolean shouldBackupToCloud) {
        if (key.trim().isEmpty()) {
            Log.w(TAG, "Key cannot be blank for storeBytes.");
            return false;
        }

        StoreBytesData storeRequest = new StoreBytesData.Builder()
                .setKey(key)
                .setBytes(data)
                .setShouldBackupToCloud(shouldBackupToCloud)
                .build();

        try {
            // Tasks.await() blocks until the task is complete. MUST NOT be on Main Thread.
            Tasks.await(client.storeBytes(storeRequest));
            Log.d(TAG, "Successfully stored bytes for key: " + key);
            return true;
        } catch (ExecutionException e) {
            Log.e(TAG, "ExecutionException storing bytes for key '" + key + "' in Block Store", e.getCause() != null ? e.getCause() : e);
        } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException storing bytes for key '" + key + "' in Block Store", e);
            Thread.currentThread().interrupt(); // Restore the interrupted status
        }
        return false;
    }

    /**
     * Stores a byte array in Google's Block Store with default backup behavior.
     * IMPORTANT: This method uses Tasks.await() and MUST be called from a background thread.
     *
     * @param key  The key to store the data under.
     * @param data The byte array to store.
     * @return True if successful, false otherwise.
     */
    public boolean storeBytes(@NonNull String key, @NonNull byte[] data) {
        return storeBytes(key, data, defaultShouldBackupToCloud);
    }


    /**
     * Retrieves a byte array from Google's Block Store.
     * IMPORTANT: This method uses Tasks.await() and MUST be called from a background thread.
     *
     * @param key The key the data was stored under.
     * @return The retrieved byte array, or null if not found or error.
     */
    @Nullable
    public byte[] retrieveBytes(@NonNull String key) {
        if (key.trim().isEmpty()) {
            Log.w(TAG, "Key cannot be blank for retrieveBytes.");
            return null;
        }

        RetrieveBytesRequest retrieveRequest = new RetrieveBytesRequest.Builder()
                .setKeys(Collections.singletonList(key))
                .build();

        try {
            // Tasks.await() blocks. MUST NOT be on Main Thread.
            RetrieveBytesResponse response = Tasks.await(client.retrieveBytes(retrieveRequest));
            Map<String, RetrieveBytesResponse.BlockstoreData> blockstoreDataMap = response.getBlockstoreDataMap();

            if (blockstoreDataMap.containsKey(key)) {
                RetrieveBytesResponse.BlockstoreData dataRef = blockstoreDataMap.get(key);
                if (dataRef != null) {
                    byte[] retrievedData = dataRef.getBytes();
                    if (retrievedData != null) {
                        Log.d(TAG, "Successfully retrieved bytes for key: " + key);
                        return retrievedData;
                    } else {
                        Log.w(TAG, "Data for key '" + key + "' was null, though key was present.");
                        return null;
                    }
                }
            } else {
                Log.d(TAG, "No data found for key '" + key + "' in Block Store.");
            }
        } catch (ExecutionException e) {
            Log.e(TAG, "ExecutionException retrieving bytes for key '" + key + "' from Block Store", e.getCause() != null ? e.getCause() : e);
        } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException retrieving bytes for key '" + key + "' from Block Store", e);
            Thread.currentThread().interrupt();
        }
        return null;
    }

    /**
     * Checks if a block exists in Google's Block Store for the given key.
     * IMPORTANT: This method uses Tasks.await() and MUST be called from a background thread.
     *
     * @param key The key to check.
     * @return True if data exists for the key, false otherwise.
     */
    public boolean hasBlock(@NonNull String key) {
        if (key.trim().isEmpty()) {
            Log.w(TAG, "Key cannot be blank for hasBlock.");
            return false;
        }
        return retrieveBytes(key) != null;
    }

    /**
     * Deletes a block of data from Google's Block Store.
     * IMPORTANT: This method uses Tasks.await() and MUST be called from a background thread.
     *
     * @param key The key of the block to delete.
     * @return True if successful, false otherwise.
     */
    public boolean deleteBlock(@NonNull String key) {
        if (key.trim().isEmpty()) {
            Log.w(TAG, "Key cannot be blank for deleteBlock.");
            return false;
        }

         DeleteBytesRequest deleteRequest = new DeleteBytesRequest.Builder()
                .setKeys(Collections.singletonList(key))
                .build();

        try {
             Tasks.await(client.deleteBytes(deleteRequest));
            Log.d(TAG, "Successfully submitted deletion for key: " + key);
            return true;
        } catch (ExecutionException e) {
            Log.e(TAG, "ExecutionException deleting block for key '" + key + "' from Block Store", e.getCause() != null ? e.getCause() : e);
        } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException deleting block for key '" + key + "' from Block Store", e);
            Thread.currentThread().interrupt();
        }
        return false;
    }

    /**
     * Deletes multiple blocks of data from Google's Block Store.
     * IMPORTANT: This method uses Tasks.await() and MUST be called from a background thread.
     *
     * @param keys A list of keys for the blocks to delete.
     * @return True if successful, false otherwise.
     */
    public boolean deleteBlocks(@NonNull List<String> keys) {
        if (keys.isEmpty()) {
            Log.w(TAG, "Key list cannot be empty for deleteBlocks.");
            return false;
        }
        for (String key : keys) {
            if (key == null || key.trim().isEmpty()) {
                Log.w(TAG, "One or more keys are null or blank in the list for deleteBlocks.");
                return false;
            }
        }

        DeleteBytesRequest deleteRequest = new DeleteBytesRequest.Builder()
                .setKeys(keys)
                .build();

        try {
             Tasks.await(client.deleteBytes(deleteRequest));
            Log.d(TAG, "Successfully submitted deletion for keys: " + keys);
            return true;
        } catch (ExecutionException e) {
            Log.e(TAG, "ExecutionException deleting blocks for keys '" + keys + "' from Block Store", e.getCause() != null ? e.getCause() : e);
        } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException deleting blocks for keys '" + keys + "' from Block Store", e);
            Thread.currentThread().interrupt();
        }
        return false;
    }


    /**
     * Retrieves all stored key-value pairs for this app from Block Store.
     * IMPORTANT: This method uses Tasks.await() and MUST be called from a background thread.
     * Note: This retrieves all data available to this application from Block Store.
     *
     * @return A map of all key-value pairs, or an empty map if error or none found.
     */
    @NonNull
    public Map<String, byte[]> retrieveAllBlocks() {
        RetrieveBytesRequest retrieveRequest = new RetrieveBytesRequest.Builder()
                .build(); // Requesting without specific keys retrieves all app data

        try {
            RetrieveBytesResponse response = Tasks.await(client.retrieveBytes(retrieveRequest));
            Map<String, byte[]> allData = new HashMap<>();
            for (Map.Entry<String, RetrieveBytesResponse.BlockstoreData> entry : response.getBlockstoreDataMap().entrySet()) {
                if (entry.getValue() != null) {
                    entry.getValue().getBytes();
                    allData.put(entry.getKey(), entry.getValue().getBytes());
                }
            }
            Log.d(TAG, "Retrieved " + allData.size() + " blocks from Block Store.");
            return allData;
        } catch (ExecutionException e) {
            Log.e(TAG, "ExecutionException retrieving all blocks from Block Store", e.getCause() != null ? e.getCause() : e);
        } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException retrieving all blocks from Block Store", e);
            Thread.currentThread().interrupt();
        }
        return Collections.emptyMap();
    }

    // --- Helper for non-blocking Task execution with callbacks (alternative to Tasks.await()) ---

    public interface StoreBytesCallback {
        void onSuccess();
        void onError(Exception e);
    }

    /**
     * Asynchronously stores bytes and uses a callback.
     * This method does NOT block and can be called from the main thread.
     * The callback will be invoked on the main thread.
     */
    public void storeBytesAsync(@NonNull String key, @NonNull byte[] data, boolean shouldBackupToCloud, @NonNull StoreBytesCallback callback) {
        if (key.trim().isEmpty()) {
            Log.w(TAG, "Key cannot be blank for storeBytesAsync.");
            callback.onError(new IllegalArgumentException("Key cannot be blank"));
            return;
        }

        StoreBytesData storeRequest = new StoreBytesData.Builder()
                .setKey(key)
                .setBytes(data)
                .setShouldBackupToCloud(shouldBackupToCloud)
                .build();

        client.storeBytes(storeRequest)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully stored bytes asynchronously for key: " + key);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error storing bytes asynchronously for key '" + key + "'", e);
                    callback.onError(e);
                });
    }

    /**
     * Asynchronously stores bytes with default backup behavior and uses a callback.
     * This method does NOT block and can be called from the main thread.
     * The callback will be invoked on the main thread.
     * 
     * @param key      The key to store the data under.
     * @param data     The byte array to store.
     * @param callback Callback to handle success or failure.
     */
    public void storeBytesAsync(@NonNull String key, @NonNull byte[] data, @NonNull StoreBytesCallback callback) {
        storeBytesAsync(key, data, defaultShouldBackupToCloud, callback);
    }

    public interface RetrieveBytesCallback {
        void onSuccess(@Nullable byte[] data);
        void onError(Exception e);
    }

    /**
     * Asynchronously retrieves bytes and uses a callback.
     * This method does NOT block and can be called from the main thread.
     * The callback will be invoked on the main thread.
     */
    public void retrieveBytesAsync(@NonNull String key, @NonNull RetrieveBytesCallback callback) {
        if (key.trim().isEmpty()) {
            Log.w(TAG, "Key cannot be blank for retrieveBytesAsync.");
            callback.onError(new IllegalArgumentException("Key cannot be blank"));
            return;
        }
        RetrieveBytesRequest retrieveRequest = new RetrieveBytesRequest.Builder()
                .setKeys(Collections.singletonList(key))
                .build();

        client.retrieveBytes(retrieveRequest)
                .addOnSuccessListener(response -> {
                    Map<String, RetrieveBytesResponse.BlockstoreData> blockstoreDataMap = response.getBlockstoreDataMap();
                    if (blockstoreDataMap.containsKey(key)) {
                        RetrieveBytesResponse.BlockstoreData dataRef = blockstoreDataMap.get(key);
                        if (dataRef != null) {
                            Log.d(TAG, "Successfully retrieved bytes asynchronously for key: " + key);
                            callback.onSuccess(dataRef.getBytes());
                            return;
                        }
                    }
                    Log.d(TAG, "No data found asynchronously for key '" + key + "'.");
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error retrieving bytes asynchronously for key '" + key + "'", e);
                    callback.onError(e);
                });
    }

    public interface DeleteBytesCallback {
        void onSuccess();
        void onError(Exception e);
    }

    /**
     * Asynchronously deletes a block of data from Google's Block Store.
     * This method does NOT block and can be called from the main thread.
     * The callback will be invoked on the main thread.
     *
     * @param key      The key of the block to delete.
     * @param callback Callback to handle success or failure.
     */
    public void deleteBlockAsync(@NonNull String key, @NonNull DeleteBytesCallback callback) {
        if (key.trim().isEmpty()) {
            Log.w(TAG, "Key cannot be blank for deleteBlockAsync.");
            callback.onError(new IllegalArgumentException("Key cannot be blank"));
            return;
        }

        DeleteBytesRequest deleteRequest = new DeleteBytesRequest.Builder()
                .setKeys(Collections.singletonList(key))
                .build();

        client.deleteBytes(deleteRequest)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully submitted deletion asynchronously for key: " + key);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting block asynchronously for key '" + key + "'", e);
                    callback.onError(e);
                });
    }

    /**
     * Asynchronously deletes multiple blocks of data from Google's Block Store.
     * This method does NOT block and can be called from the main thread.
     * The callback will be invoked on the main thread.
     *
     * @param keys     A list of keys for the blocks to delete.
     * @param callback Callback to handle success or failure.
     */
    public void deleteBlocksAsync(@NonNull List<String> keys, @NonNull DeleteBytesCallback callback) {
        if (keys.isEmpty()) {
            Log.w(TAG, "Key list cannot be empty for deleteBlocksAsync.");
            callback.onError(new IllegalArgumentException("Key list cannot be empty"));
            return;
        }
        
        for (String key : keys) {
            if (key == null || key.trim().isEmpty()) {
                Log.w(TAG, "One or more keys are null or blank in the list for deleteBlocksAsync.");
                callback.onError(new IllegalArgumentException("One or more keys are null or blank"));
                return;
            }
        }

        DeleteBytesRequest deleteRequest = new DeleteBytesRequest.Builder()
                .setKeys(keys)
                .build();

        client.deleteBytes(deleteRequest)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully submitted deletion asynchronously for keys: " + keys);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting blocks asynchronously for keys '" + keys + "'", e);
                    callback.onError(e);
                });
    }

    public interface RetrieveAllBytesCallback {
        void onSuccess(@NonNull Map<String, byte[]> dataMap);
        void onError(Exception e);
    }

    /**
     * Asynchronously retrieves all stored key-value pairs.
     * This method does NOT block and can be called from the main thread.
     * The callback will be invoked on the main thread.
     *
     * @param callback Callback to handle success or failure.
     */
    public void retrieveAllBlocksAsync(@NonNull RetrieveAllBytesCallback callback) {
        RetrieveBytesRequest retrieveRequest = new RetrieveBytesRequest.Builder()
                .build();

        client.retrieveBytes(retrieveRequest)
                .addOnSuccessListener(response -> {
                    Map<String, byte[]> allData = new HashMap<>();
                    for (Map.Entry<String, RetrieveBytesResponse.BlockstoreData> entry : 
                            response.getBlockstoreDataMap().entrySet()) {
                        if (entry.getValue() != null && entry.getValue().getBytes() != null) {
                            allData.put(entry.getKey(), entry.getValue().getBytes());
                        }
                    }
                    Log.d(TAG, "Retrieved " + allData.size() + " blocks asynchronously from Block Store.");
                    callback.onSuccess(allData);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error retrieving all blocks asynchronously from Block Store", e);
                    callback.onError(e);
                });
    }

    public interface HasBlockCallback {
        void onResult(boolean exists);
        void onError(Exception e);
    }

    /**
     * Asynchronously checks if a block exists in Google's Block Store.
     * This method does NOT block and can be called from the main thread.
     * The callback will be invoked on the main thread.
     *
     * @param key      The key to check.
     * @param callback Callback to handle the result or failure.
     */
    public void hasBlockAsync(@NonNull String key, @NonNull HasBlockCallback callback) {
        if (key.trim().isEmpty()) {
            Log.w(TAG, "Key cannot be blank for hasBlockAsync.");
            callback.onError(new IllegalArgumentException("Key cannot be blank"));
            return;
        }
        
        retrieveBytesAsync(key, new RetrieveBytesCallback() {
            @Override
            public void onSuccess(@Nullable byte[] data) {
                callback.onResult(data != null);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Executes the given Runnable on a background thread.
     * This helper method can be used to safely execute blocking operations
     * like storeBytes, retrieveBytes, etc. that use Tasks.await().
     *
     * @param runnable The Runnable to execute on a background thread.
     */
    public void executeOnBackgroundThread(Runnable runnable) {
        backgroundExecutor.execute(runnable);
    }
}
