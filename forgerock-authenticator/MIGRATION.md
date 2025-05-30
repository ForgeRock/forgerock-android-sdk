# Storage Migration Guide

This guide explains how to migrate data from `DefaultStorageClient` to `SQLStorageClient` in the ForgeRock Android SDK.

## Overview

The ForgeRock Android SDK supports two storage implementations:

1. **DefaultStorageClient**: The original storage implementation using SharedPreferences
2. **SQLStorageClient**: A more reliable storage implementation using SQLCipher encrypted database

The migration functionality helps developers move data from DefaultStorageClient to SQLStorageClient seamlessly while providing options for both automatic and manual migration approaches.

## Automatic Migration

By default, the SDK will automatically detect and migrate data when you switch from `DefaultStorageClient` to `SQLStorageClient`.

### How Automatic Migration Works

1. When `FRAClient` is initialized with `SQLStorageClient`, it checks if there's data in the legacy `DefaultStorageClient`
2. If data is found, it's automatically migrated to the new storage
3. The migration status is stored so it won't be performed again
4. Source data is preserved during automatic migration

### Example: Basic Initialization with Automatic Migration

```java
// Initialize FRAClient with SQLStorageClient
FRAClient fraClient = FRAClient.builder()
        .withContext(context)
        .withStorage(new SQLStorageClient(context))
        .start();
```

### Disabling Automatic Migration

If you want to disable automatic migration, use the `disableAutoMigration()` method in the builder:

```java
// Initialize FRAClient with automatic migration disabled
FRAClient fraClient = FRAClient.builder()
        .withContext(context)
        .withStorage(new SQLStorageClient(context))
        .disableAutoMigration() // Disable automatic migration
        .start();
```

## Manual Migration

For more control over the migration process, you can perform manual migration using the `migrateFromDefaultStorageClient()` method.

### Example: Manual Migration

```java
try {
    // Perform manual migration
    boolean deleteSourceData = true; // Set to true to delete data from DefaultStorageClient after migration
    boolean success = fraClient.migrateFromDefaultStorageClient(deleteSourceData);
    
    if (success) {
        // Migration successful
        Log.d(TAG, "Migration completed successfully");
    } else {
        // Migration failed or was not needed
        Log.d(TAG, "Migration failed or was not needed");
    }
} catch (AuthenticatorException e) {
    // Handle exception (e.g., current storage is not SQLStorageClient)
    Log.e(TAG, "Error during migration", e);
}
```

## Migration Process

The migration process handles the following data types:

1. **Accounts**: All user accounts
2. **Mechanisms**: OATH and Push mechanisms for each account
3. **Notifications**: Push notifications associated with Push mechanisms
4. **Device Token**: The FCM device token

## Best Practices

1. **Test thoroughly**: Before deploying the migration in production, thoroughly test the migration process with your app's data patterns
2. **Handle errors**: Implement proper error handling for both automatic and manual migration
3. **User communication**: Consider informing users when a migration is taking place, especially if it might take some time
4. **Data verification**: After migration, verify that all data has been correctly migrated
5. **Cleanup**: Consider cleaning up the old storage data once you're confident the migration was successful

## Troubleshooting

### Migration Not Happening

- Check that you're properly initializing with `SQLStorageClient`
- Ensure you haven't called `disableAutoMigration()`
- Verify that there's data in the legacy storage that needs migration

### Migration Errors

- Enable debug logging to see detailed migration logs
- Verify that both storage systems have the necessary permissions
- Check if the device has sufficient storage space

## Sample Code

See the `java-authenticator` sample app for a complete example demonstrating both automatic and manual migration approaches.

## API Reference

### FRAClient.Builder

- `disableAutoMigration()`: Disables automatic migration during initialization

### FRAClient

- `migrateFromDefaultStorageClient(boolean deleteOldStorage)`: Manually migrates data from DefaultStorageClient to the current SQLStorageClient
  - `deleteOldStorage`: If true, deletes data from the source storage after successful migration
  - Returns: `boolean` indicating success or failure
  - Throws: `AuthenticatorException` if the current storage is not SQLStorageClient
