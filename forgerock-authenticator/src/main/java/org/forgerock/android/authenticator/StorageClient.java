package org.forgerock.android.authenticator;

import java.util.List;

/**
 * Data Access interface used to store and load Accounts, Mechanisms and Notifications.
 * Encapsulates a backing storage mechanism, and provides a standard set of functions for operating
 * on the data.
 */
public interface StorageClient {

    /**
     * Get the Account object with its id
     * @param accountId The account unique ID
     * @return The account object.
     */
    Account getAccount(String accountId);

    /**
     * Get all accounts stored in the system.
     * @return The complete list of accounts.
     */
    List<Account> getAllAccounts();

    /**
     * Delete the Account that was passed in.
     * @param account The account object to delete.
     * @return boolean as result of the operation
     */
    boolean removeAccount(Account account);

    /**
     * Add or Update the Account to the storage system.
     * @param account The Account to store or update.
     * @return boolean as result of the operation
     */
    boolean setAccount(Account account);

    /**
     * Get the mechanisms associated with an account.
     * @param accountId The account object
     * @return The list of mechanisms for the account.
     */
    List<Mechanism> getMechanismsForAccount(String accountId);

    /**
     * Delete the mechanism uniquely identified by an id.
     * @param mechanism The mechanism object to delete.
     * @return boolean as result of the operation
     */
    boolean removeMechanism(Mechanism mechanism);

    /**
     * Add or update the mechanism to the storage system.
     * If the owning Account is not yet stored, store that as well.
     * @param mechanism The mechanism to store or update.
     * @return boolean as result of the operation
     */
    boolean setMechanism(Mechanism mechanism);

    /**
     * Get all notifications for within the mechanism.
     * @param mechanismId The mechanism object
     * @return The list of notifications for the mechanism.
     */
    List<Notification> getAllNotificationsForMechanism(String mechanismId);

    /**
     * Delete the notification uniquely identified by an id.
     * @param notification The notification object to delete.
     */
    boolean removeNotification(Notification notification);

    /**
     * Add or update the notification to the storage system.
     * @param notification The notification to store.
     * @return boolean as result of the operation
     */
    boolean setNotification(Notification notification);

    /**
     * Whether the storage system currently contains any data.
     * @return True if the storage system is empty, false otherwise.
     */
    boolean isEmpty();
}
