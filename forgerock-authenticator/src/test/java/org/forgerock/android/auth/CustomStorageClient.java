/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.forgerock.android.auth.FRABaseTest.TEST_SHARED_PREFERENCES_DATA_ACCOUNT;
import static org.forgerock.android.auth.FRABaseTest.TEST_SHARED_PREFERENCES_DATA_MECHANISM;
import static org.forgerock.android.auth.FRABaseTest.TEST_SHARED_PREFERENCES_DATA_NOTIFICATIONS;

/**
 * Test class which implements StorageClient interface and uses SharedPreferences to store
 * and load Accounts, Mechanisms and Notifications.
 */
public class CustomStorageClient implements StorageClient {

    public CustomStorageClient(Context context) {

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
    public boolean setAccount(Account account) {
        return false;
    }

    @Override
    public List<Mechanism> getMechanismsForAccount(Account account) {
        return null;
    }

    @Override
    public Mechanism getMechanismByUUID(String mechanismUID) {
        return null;
    }

    @Override
    public boolean removeMechanism(Mechanism mechanism) {
        return false;
    }

    @Override
    public boolean setMechanism(Mechanism mechanism) {
        return false;
    }

    @Override
    public List<PushNotification> getAllNotificationsForMechanism(Mechanism mechanism) {
        return null;
    }

    @Override
    public boolean removeNotification(PushNotification pushNotification) {
        return false;
    }

    @Override
    public boolean setNotification(PushNotification pushNotification) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

}
