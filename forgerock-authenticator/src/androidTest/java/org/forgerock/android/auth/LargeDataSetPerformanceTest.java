/*
 * Copyright (c) 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Performance tests for large data sets to compare DefaultStorageClient and SQLStorageClient
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LargeDataSetPerformanceTest {

    private Context context;
    private DefaultStorageClient defaultStorageClient;
    private SQLStorageClient sqlStorageClient;
    
    // Large dataset configuration - adjust based on test device capability
    private static final int NUM_ACCOUNTS = 30;
    private static final int NUM_PUSH_MECHANISMS = 1;
    private static final int NUM_NOTIFICATIONS = 100; // 3000 notifications total

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        defaultStorageClient = new DefaultStorageClient(context);
        sqlStorageClient = new SQLStorageClient(context);
        
        // Clear any existing data
        clearStorage(defaultStorageClient);
        clearStorage(sqlStorageClient);
    }

    @After
    public void tearDown() {
        // Clean up after tests
        clearStorage(defaultStorageClient);
        clearStorage(sqlStorageClient);
    }

    @Test
    public void testLargeNotificationDataSetPerformance() throws Exception {
        System.out.println("=== Testing Large Notification Dataset Performance ===");
        System.out.println("Accounts: " + NUM_ACCOUNTS);
        System.out.println("Push Mechanisms per Account: " + NUM_PUSH_MECHANISMS);
        System.out.println("Notifications per Mechanism: " + NUM_NOTIFICATIONS);
        System.out.println("Total Notifications: " + (NUM_ACCOUNTS * NUM_PUSH_MECHANISMS * NUM_NOTIFICATIONS));
        
        // Generate test data
        List<Account> accounts = generateAccounts(NUM_ACCOUNTS);
        List<Mechanism> mechanisms = new ArrayList<>();
        List<PushNotification> notifications = new ArrayList<>();
        
        for (Account account : accounts) {
            List<Mechanism> accountMechanisms = generatePushMechanisms(account, NUM_PUSH_MECHANISMS);
            mechanisms.addAll(accountMechanisms);
            
            for (Mechanism mechanism : accountMechanisms) {
                notifications.addAll(generateNotifications(mechanism, NUM_NOTIFICATIONS));
            }
        }
        
        // Test write performance for DefaultStorageClient
        System.out.println("Writing data to DefaultStorageClient...");
        long defaultWriteStart = System.nanoTime();
        
        for (Account account : accounts) {
            defaultStorageClient.setAccount(account);
        }
        
        for (Mechanism mechanism : mechanisms) {
            defaultStorageClient.setMechanism(mechanism);
        }
        
        for (PushNotification notification : notifications) {
            defaultStorageClient.setNotification(notification);
        }
        
        long defaultWriteEnd = System.nanoTime();
        long defaultWriteDuration = TimeUnit.MILLISECONDS.convert(defaultWriteEnd - defaultWriteStart, TimeUnit.NANOSECONDS);
        
        // Test write performance for SQLStorageClient
        System.out.println("Writing data to SQLStorageClient...");
        long sqlWriteStart = System.nanoTime();
        
        for (Account account : accounts) {
            sqlStorageClient.setAccount(account);
        }
        
        for (Mechanism mechanism : mechanisms) {
            sqlStorageClient.setMechanism(mechanism);
        }
        
        for (PushNotification notification : notifications) {
            sqlStorageClient.setNotification(notification);
        }
        
        long sqlWriteEnd = System.nanoTime();
        long sqlWriteDuration = TimeUnit.MILLISECONDS.convert(sqlWriteEnd - sqlWriteStart, TimeUnit.NANOSECONDS);
        
        // Test read performance
        System.out.println("Reading all notifications from DefaultStorageClient...");
        long defaultReadStart = System.nanoTime();
        List<PushNotification> defaultNotifications = defaultStorageClient.getAllNotifications();
        long defaultReadEnd = System.nanoTime();
        long defaultReadDuration = TimeUnit.MILLISECONDS.convert(defaultReadEnd - defaultReadStart, TimeUnit.NANOSECONDS);
        
        System.out.println("Reading all notifications from SQLStorageClient...");
        long sqlReadStart = System.nanoTime();
        List<PushNotification> sqlNotifications = sqlStorageClient.getAllNotifications();
        long sqlReadEnd = System.nanoTime();
        long sqlReadDuration = TimeUnit.MILLISECONDS.convert(sqlReadEnd - sqlReadStart, TimeUnit.NANOSECONDS);
        
        // Test notification filtering performance
        System.out.println("Filtering notifications by mechanism in DefaultStorageClient...");
        long defaultFilterStart = System.nanoTime();
        for (Mechanism mechanism : mechanisms) {
            List<PushNotification> mechanismNotifications = defaultStorageClient.getAllNotificationsForMechanism(mechanism);
        }
        long defaultFilterEnd = System.nanoTime();
        long defaultFilterDuration = TimeUnit.MILLISECONDS.convert(defaultFilterEnd - defaultFilterStart, TimeUnit.NANOSECONDS);
        
        System.out.println("Filtering notifications by mechanism in SQLStorageClient...");
        long sqlFilterStart = System.nanoTime();
        for (Mechanism mechanism : mechanisms) {
            List<PushNotification> mechanismNotifications = sqlStorageClient.getAllNotificationsForMechanism(mechanism);
        }
        long sqlFilterEnd = System.nanoTime();
        long sqlFilterDuration = TimeUnit.MILLISECONDS.convert(sqlFilterEnd - sqlFilterStart, TimeUnit.NANOSECONDS);
        
        // Print results
        System.out.println("\n=== Performance Results (ms) ===");
        System.out.println("Write Performance:");
        System.out.println("  DefaultStorageClient: " + defaultWriteDuration);
        System.out.println("  SQLStorageClient: " + sqlWriteDuration);
        System.out.println("  Improvement: " + calculateImprovement(defaultWriteDuration, sqlWriteDuration) + "%");
        
        System.out.println("\nRead Performance:");
        System.out.println("  DefaultStorageClient: " + defaultReadDuration);
        System.out.println("  SQLStorageClient: " + sqlReadDuration);
        System.out.println("  Improvement: " + calculateImprovement(defaultReadDuration, sqlReadDuration) + "%");
        
        System.out.println("\nFilter Performance:");
        System.out.println("  DefaultStorageClient: " + defaultFilterDuration);
        System.out.println("  SQLStorageClient: " + sqlFilterDuration);
        System.out.println("  Improvement: " + calculateImprovement(defaultFilterDuration, sqlFilterDuration) + "%");
        
        System.out.println("\nRetrieved Notification Count:");
        System.out.println("  DefaultStorageClient: " + defaultNotifications.size());
        System.out.println("  SQLStorageClient: " + sqlNotifications.size());
    }
    
    @Test
    public void testTransactionPerformance() throws Exception {
        // This test evaluates the performance advantage of SQL transactions
        System.out.println("=== Testing Transaction Performance ===");
        
        int numNotifications = 500;
        Account account = generateAccounts(1).get(0);
        Mechanism mechanism = generatePushMechanisms(account, 1).get(0);
        List<PushNotification> notifications = generateNotifications(mechanism, numNotifications);
        
        // Set up account and mechanism in both storage clients
        defaultStorageClient.setAccount(account);
        defaultStorageClient.setMechanism(mechanism);
        sqlStorageClient.setAccount(account);
        sqlStorageClient.setMechanism(mechanism);
        
        System.out.println("Adding " + numNotifications + " notifications individually...");
        
        // DefaultStorageClient - individual writes
        long defaultStart = System.nanoTime();
        for (PushNotification notification : notifications) {
            defaultStorageClient.setNotification(notification);
        }
        long defaultEnd = System.nanoTime();
        long defaultDuration = TimeUnit.MILLISECONDS.convert(defaultEnd - defaultStart, TimeUnit.NANOSECONDS);
        
        // SQLStorageClient - individual writes
        long sqlStart = System.nanoTime();
        for (PushNotification notification : notifications) {
            sqlStorageClient.setNotification(notification);
        }
        long sqlEnd = System.nanoTime();
        long sqlDuration = TimeUnit.MILLISECONDS.convert(sqlEnd - sqlStart, TimeUnit.NANOSECONDS);
        
        // Clean up notifications
        clearNotifications(defaultStorageClient);
        clearNotifications(sqlStorageClient);
        
        System.out.println("\nResults for individual writes (ms):");
        System.out.println("  DefaultStorageClient: " + defaultDuration);
        System.out.println("  SQLStorageClient: " + sqlDuration);
        System.out.println("  Improvement: " + calculateImprovement(defaultDuration, sqlDuration) + "%");
    }
    
    private void clearStorage(StorageClient storageClient) {
        List<Account> accounts = storageClient.getAllAccounts();
        for (Account account : accounts) {
            List<Mechanism> mechanisms = storageClient.getMechanismsForAccount(account);
            for (Mechanism mechanism : mechanisms) {
                if (mechanism instanceof PushMechanism) {
                    clearNotificationsForMechanism(storageClient, mechanism);
                }
                storageClient.removeMechanism(mechanism);
            }
            storageClient.removeAccount(account);
        }
    }
    
    private void clearNotifications(StorageClient storageClient) {
        List<PushNotification> notifications = storageClient.getAllNotifications();
        for (PushNotification notification : notifications) {
            storageClient.removeNotification(notification);
        }
    }
    
    private void clearNotificationsForMechanism(StorageClient storageClient, Mechanism mechanism) {
        List<PushNotification> notifications = storageClient.getAllNotificationsForMechanism(mechanism);
        for (PushNotification notification : notifications) {
            storageClient.removeNotification(notification);
        }
    }
    
    private List<Account> generateAccounts(int count) {
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Account account = Account.builder()
                    .setIssuer("Performance Test Issuer " + i)
                    .setAccountName("perf.test." + i + "@example.com")
                    .setImageURL("https://example.com/perf" + i + ".png")
                    .setBackgroundColor("#EFEFEF")
                    .build();
            accounts.add(account);
        }
        return accounts;
    }
    
    private List<Mechanism> generatePushMechanisms(Account account, int count) throws Exception {
        List<Mechanism> mechanisms = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            PushMechanism mechanism = PushMechanism.builder()
                    .setMechanismUID(UUID.randomUUID().toString())
                    .setIssuer(account.getIssuer())
                    .setAccountName(account.getAccountName())
                    .setAuthenticationEndpoint("https://example.com/push" + i)
                    .setRegistrationEndpoint("https://example.com/register" + i)
                    .setSecret("testKey" + i)
                    .build();
            
            mechanisms.add(mechanism);
        }
        return mechanisms;
    }
    
    private List<PushNotification> generateNotifications(Mechanism mechanism, int count) throws Exception {
        List<PushNotification> notifications = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Calendar timeAdded = Calendar.getInstance();
            timeAdded.add(Calendar.MINUTE, -i);
            
            PushNotification notification = PushNotification.builder()
                    .setMechanismUID(mechanism.getMechanismUID())
                    .setMessageId("perf_message_" + UUID.randomUUID().toString())
                    .setChallenge("perf_challenge_" + i)
                    .setAmlbCookie("perf_cookie_" + i)
                    .setTimeAdded(timeAdded)
                    .setTimeExpired(timeAdded) // 2 minutes later
                    .setTtl(120)
                    .build();
            
            notifications.add(notification);
        }
        return notifications;
    }
    
    private double calculateImprovement(long oldValue, long newValue) {
        if (oldValue == 0) {
            return 0;
        }
        return Math.round(((double)(oldValue - newValue) / oldValue) * 10000) / 100.0;
    }
}
