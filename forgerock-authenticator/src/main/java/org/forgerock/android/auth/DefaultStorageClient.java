package org.forgerock.android.auth;

import android.content.Context;
import android.content.SharedPreferences;

import org.forgerock.android.authenticator.Account;
import org.forgerock.android.authenticator.Mechanism;
import org.forgerock.android.authenticator.Notification;
import org.forgerock.android.authenticator.StorageClient;

import java.util.List;

/**
 * Data Access Object which implements StorageClient interface and uses SecureSharedPreferences from
 * forgerock-core SDK to store and load Accounts, Mechanisms and Notifications.
 */
public class DefaultStorageClient implements StorageClient {

    //Alias to store keys
    private static final String ORG_FORGEROCK_SHARED_PREFERENCES_KEYS = "org.forgerock.android.authenticator.KEYS";

    //Settings to store the data
    private static final String ORG_FORGEROCK_SHARED_PREFERENCES_DATA_ACCOUNT = "org.forgerock.android.authenticator.DATA.ACCOUNT";
    private static final String ORG_FORGEROCK_SHARED_PREFERENCES_DATA_MECHANISM = "org.forgerock.android.authenticator.DATA.MECHANISM";
    private static final String ORG_FORGEROCK_SHARED_PREFERENCES_DATA_NOTIFICATIONS = "org.forgerock.android.authenticator.DATA.NOTIFICATIONS";

    //The SharedPreferences to store the data
    private SharedPreferences accountData;
    private SharedPreferences mechanismData;
    private SharedPreferences notificationsData;

    public DefaultStorageClient(Context context) {
        //this.accountData = new SecuredSharedPreferences(context, ORG_FORGEROCK_SHARED_PREFERENCES_DATA_ACCOUNT, ORG_FORGEROCK_SHARED_PREFERENCES_KEYS);
        //this.mechanismData = new SecuredSharedPreferences(context, ORG_FORGEROCK_SHARED_PREFERENCES_DATA_MECHANISM, ORG_FORGEROCK_SHARED_PREFERENCES_KEYS);
        //this.notificationsData = new SecuredSharedPreferences(context, ORG_FORGEROCK_SHARED_PREFERENCES_DATA_NOTIFICATIONS, ORG_FORGEROCK_SHARED_PREFERENCES_KEYS);
        this.accountData = context.getApplicationContext()
                .getSharedPreferences(ORG_FORGEROCK_SHARED_PREFERENCES_DATA_ACCOUNT, Context.MODE_PRIVATE);
        this.mechanismData = context.getApplicationContext()
                .getSharedPreferences(ORG_FORGEROCK_SHARED_PREFERENCES_DATA_MECHANISM, Context.MODE_PRIVATE);
        this.notificationsData = context.getApplicationContext()
                .getSharedPreferences(ORG_FORGEROCK_SHARED_PREFERENCES_DATA_NOTIFICATIONS, Context.MODE_PRIVATE);
    }

    /**
     * Get all mechanisms stored in the system.
     *
     * @return The complete list of mechanisms.
     */
    private List<Mechanism> getAllMechanisms() {
        return null;
    }


    /**
     * Get all notifications stored in the model.
     *
     * @return The complete list of notifications.
     */
    private List<Notification> getAllNotifications() {
        return null;
    }


    @Override
    public boolean setAccount(Account account) {

        return false;
    }

    @Override
    public List<Mechanism> getMechanismsForAccount(String accountId) {
        return null;
    }

    @Override
    public boolean setMechanism(Mechanism mechanism) {

        return false;
    }

    @Override
    public List<Notification> getAllNotificationsForMechanism(String mechanismId) {
        return null;
    }

    @Override
    public boolean setNotification(Notification notification) {

        return false;
    }

    @Override
    public boolean removeMechanism(Mechanism mechanism) {
        return false;
    }

    @Override
    public Account getAccount(String accountId) {
        return null;
    }

    @Override
    public List<Account> getAllAccounts() {
        return null;
    }

    @Override
    public boolean removeAccount(Account account) {
        return false;
    }

    @Override
    public boolean removeNotification(Notification notification) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
