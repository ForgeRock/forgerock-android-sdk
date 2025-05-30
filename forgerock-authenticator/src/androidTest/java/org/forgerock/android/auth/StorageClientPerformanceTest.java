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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Performance comparison tests between DefaultStorageClient and SQLStorageClient
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class StorageClientPerformanceTest {

    private Context context;
    private DefaultStorageClient defaultStorageClient;
    private SQLStorageClient sqlStorageClient;
    private static final int NUM_ACCOUNTS = 10;
    private static final int MECHANISMS_PER_ACCOUNT = 2;
    private static final int NOTIFICATIONS_PER_PUSH = 20;
    private List<Account> testAccounts;
    private Map<Account, List<Mechanism>> testMechanisms;
    private Map<Mechanism, List<PushNotification>> testNotifications;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        defaultStorageClient = new DefaultStorageClient(context);
        sqlStorageClient = new SQLStorageClient(context);
        
        // Clear any existing data
        clearStorage(defaultStorageClient);
        clearStorage(sqlStorageClient);
        
        // Generate test data structures
        generateTestData();
    }

    @After
    public void tearDown() {
        // Clean up after tests
        clearStorage(defaultStorageClient);
        clearStorage(sqlStorageClient);
    }

    @Test
    public void compareAccountCreationPerformance() {
        // Test DefaultStorageClient
        long defaultStart = System.nanoTime();
        for (Account account : testAccounts) {
            defaultStorageClient.setAccount(account);
        }
        long defaultEnd = System.nanoTime();
        long defaultDuration = TimeUnit.MILLISECONDS.convert(defaultEnd - defaultStart, TimeUnit.NANOSECONDS);
        
        // Clear for next test
        clearStorage(defaultStorageClient);
        
        // Test SQLStorageClient
        long sqlStart = System.nanoTime();
        for (Account account : testAccounts) {
            sqlStorageClient.setAccount(account);
        }
        long sqlEnd = System.nanoTime();
        long sqlDuration = TimeUnit.MILLISECONDS.convert(sqlEnd - sqlStart, TimeUnit.NANOSECONDS);
        
        System.out.println("Account Creation Performance (ms):");
        System.out.println("DefaultStorageClient: " + defaultDuration);
        System.out.println("SQLStorageClient: " + sqlDuration);
        
        // Verify data was stored correctly
        assertEquals(NUM_ACCOUNTS, sqlStorageClient.getAllAccounts().size());
    }
    
    @Test
    public void compareMechanismCreationPerformance() {
        // Store accounts first
        for (Account account : testAccounts) {
            defaultStorageClient.setAccount(account);
            sqlStorageClient.setAccount(account);
        }
        
        // Test DefaultStorageClient
        long defaultStart = System.nanoTime();
        for (Account account : testAccounts) {
            List<Mechanism> mechanisms = testMechanisms.get(account);
            for (Mechanism mechanism : mechanisms) {
                defaultStorageClient.setMechanism(mechanism);
            }
        }
        long defaultEnd = System.nanoTime();
        long defaultDuration = TimeUnit.MILLISECONDS.convert(defaultEnd - defaultStart, TimeUnit.NANOSECONDS);
        
        // Test SQLStorageClient
        long sqlStart = System.nanoTime();
        for (Account account : testAccounts) {
            List<Mechanism> mechanisms = testMechanisms.get(account);
            for (Mechanism mechanism : mechanisms) {
                sqlStorageClient.setMechanism(mechanism);
            }
        }
        long sqlEnd = System.nanoTime();
        long sqlDuration = TimeUnit.MILLISECONDS.convert(sqlEnd - sqlStart, TimeUnit.NANOSECONDS);
        
        System.out.println("Mechanism Creation Performance (ms):");
        System.out.println("DefaultStorageClient: " + defaultDuration);
        System.out.println("SQLStorageClient: " + sqlDuration);
        
        // Verify data was stored correctly
        int totalMechanisms = NUM_ACCOUNTS * MECHANISMS_PER_ACCOUNT;
        int mechanismCount = 0;
        for (Account account : sqlStorageClient.getAllAccounts()) {
            mechanismCount += sqlStorageClient.getMechanismsForAccount(account).size();
        }
        assertEquals(totalMechanisms, mechanismCount);
    }
    
    @Test
    public void compareNotificationCreationPerformance() {
        // Store accounts and mechanisms first
        for (Account account : testAccounts) {
            defaultStorageClient.setAccount(account);
            sqlStorageClient.setAccount(account);
            
            List<Mechanism> mechanisms = testMechanisms.get(account);
            for (Mechanism mechanism : mechanisms) {
                defaultStorageClient.setMechanism(mechanism);
                sqlStorageClient.setMechanism(mechanism);
            }
        }
        
        // Test DefaultStorageClient
        long defaultStart = System.nanoTime();
        int notificationCountDefault = 0;
        for (Map.Entry<Mechanism, List<PushNotification>> entry : testNotifications.entrySet()) {
            if (entry.getKey() instanceof PushMechanism) {
                for (PushNotification notification : entry.getValue()) {
                    defaultStorageClient.setNotification(notification);
                    notificationCountDefault++;
                }
            }
        }
        long defaultEnd = System.nanoTime();
        long defaultDuration = TimeUnit.MILLISECONDS.convert(defaultEnd - defaultStart, TimeUnit.NANOSECONDS);
        
        // Test SQLStorageClient
        long sqlStart = System.nanoTime();
        int notificationCountSql = 0;
        for (Map.Entry<Mechanism, List<PushNotification>> entry : testNotifications.entrySet()) {
            if (entry.getKey() instanceof PushMechanism) {
                for (PushNotification notification : entry.getValue()) {
                    sqlStorageClient.setNotification(notification);
                    notificationCountSql++;
                }
            }
        }
        long sqlEnd = System.nanoTime();
        long sqlDuration = TimeUnit.MILLISECONDS.convert(sqlEnd - sqlStart, TimeUnit.NANOSECONDS);
        
        System.out.println("Notification Creation Performance (ms):");
        System.out.println("DefaultStorageClient: " + defaultDuration);
        System.out.println("SQLStorageClient: " + sqlDuration);
        
        // Verify data was stored correctly
        assertEquals(notificationCountDefault, sqlStorageClient.getAllNotifications().size());
    }
    
    @Test
    public void compareAccountRetrievalPerformance() {
        // Store accounts first
        for (Account account : testAccounts) {
            defaultStorageClient.setAccount(account);
            sqlStorageClient.setAccount(account);
        }
        
        // Test DefaultStorageClient
        long defaultStart = System.nanoTime();
        List<Account> defaultAccounts = defaultStorageClient.getAllAccounts();
        long defaultEnd = System.nanoTime();
        long defaultDuration = TimeUnit.MILLISECONDS.convert(defaultEnd - defaultStart, TimeUnit.NANOSECONDS);
        
        // Test SQLStorageClient
        long sqlStart = System.nanoTime();
        List<Account> sqlAccounts = sqlStorageClient.getAllAccounts();
        long sqlEnd = System.nanoTime();
        long sqlDuration = TimeUnit.MILLISECONDS.convert(sqlEnd - sqlStart, TimeUnit.NANOSECONDS);
        
        System.out.println("Account Retrieval Performance (ms):");
        System.out.println("DefaultStorageClient: " + defaultDuration);
        System.out.println("SQLStorageClient: " + sqlDuration);
        
        // Verify data was retrieved correctly
        assertEquals(NUM_ACCOUNTS, sqlAccounts.size());
        assertEquals(NUM_ACCOUNTS, defaultAccounts.size());
    }
    
    @Test
    public void compareMechanismRetrievalPerformance() {
        // Store accounts and mechanisms first
        for (Account account : testAccounts) {
            defaultStorageClient.setAccount(account);
            sqlStorageClient.setAccount(account);
            
            List<Mechanism> mechanisms = testMechanisms.get(account);
            for (Mechanism mechanism : mechanisms) {
                defaultStorageClient.setMechanism(mechanism);
                sqlStorageClient.setMechanism(mechanism);
            }
        }
        
        // Test DefaultStorageClient
        long defaultStart = System.nanoTime();
        int defaultMechanismCount = 0;
        for (Account account : defaultStorageClient.getAllAccounts()) {
            defaultMechanismCount += defaultStorageClient.getMechanismsForAccount(account).size();
        }
        long defaultEnd = System.nanoTime();
        long defaultDuration = TimeUnit.MILLISECONDS.convert(defaultEnd - defaultStart, TimeUnit.NANOSECONDS);
        
        // Test SQLStorageClient
        long sqlStart = System.nanoTime();
        int sqlMechanismCount = 0;
        for (Account account : sqlStorageClient.getAllAccounts()) {
            sqlMechanismCount += sqlStorageClient.getMechanismsForAccount(account).size();
        }
        long sqlEnd = System.nanoTime();
        long sqlDuration = TimeUnit.MILLISECONDS.convert(sqlEnd - sqlStart, TimeUnit.NANOSECONDS);
        
        System.out.println("Mechanism Retrieval Performance (ms):");
        System.out.println("DefaultStorageClient: " + defaultDuration);
        System.out.println("SQLStorageClient: " + sqlDuration);
        
        // Verify data was retrieved correctly
        int expectedMechanismCount = NUM_ACCOUNTS * MECHANISMS_PER_ACCOUNT;
        assertEquals(expectedMechanismCount, sqlMechanismCount);
        assertEquals(expectedMechanismCount, defaultMechanismCount);
    }
    
    @Test
    public void compareNotificationRetrievalPerformance() {
        // Store accounts, mechanisms, and notifications
        setupFullDataSet();
        
        // Test DefaultStorageClient
        long defaultStart = System.nanoTime();
        List<PushNotification> defaultNotifications = defaultStorageClient.getAllNotifications();
        long defaultEnd = System.nanoTime();
        long defaultDuration = TimeUnit.MILLISECONDS.convert(defaultEnd - defaultStart, TimeUnit.NANOSECONDS);
        
        // Test SQLStorageClient
        long sqlStart = System.nanoTime();
        List<PushNotification> sqlNotifications = sqlStorageClient.getAllNotifications();
        long sqlEnd = System.nanoTime();
        long sqlDuration = TimeUnit.MILLISECONDS.convert(sqlEnd - sqlStart, TimeUnit.NANOSECONDS);
        
        System.out.println("Notification Retrieval Performance (ms):");
        System.out.println("DefaultStorageClient: " + defaultDuration);
        System.out.println("SQLStorageClient: " + sqlDuration);
        
        // Verify data was retrieved correctly
        assertEquals(defaultNotifications.size(), sqlNotifications.size());
    }
    
    @Test
    public void compareFullDatabaseScanPerformance() {
        // Store accounts, mechanisms, and notifications
        setupFullDataSet();
        
        // Test DefaultStorageClient
        long defaultStart = System.nanoTime();
        List<Account> defaultAccounts = defaultStorageClient.getAllAccounts();
        for (Account account : defaultAccounts) {
            List<Mechanism> mechanisms = defaultStorageClient.getMechanismsForAccount(account);
            for (Mechanism mechanism : mechanisms) {
                if (mechanism instanceof PushMechanism) {
                    List<PushNotification> notifications = defaultStorageClient.getAllNotificationsForMechanism(mechanism);
                }
            }
        }
        long defaultEnd = System.nanoTime();
        long defaultDuration = TimeUnit.MILLISECONDS.convert(defaultEnd - defaultStart, TimeUnit.NANOSECONDS);
        
        // Test SQLStorageClient
        long sqlStart = System.nanoTime();
        List<Account> sqlAccounts = sqlStorageClient.getAllAccounts();
        for (Account account : sqlAccounts) {
            List<Mechanism> mechanisms = sqlStorageClient.getMechanismsForAccount(account);
            for (Mechanism mechanism : mechanisms) {
                if (mechanism instanceof PushMechanism) {
                    List<PushNotification> notifications = sqlStorageClient.getAllNotificationsForMechanism(mechanism);
                }
            }
        }
        long sqlEnd = System.nanoTime();
        long sqlDuration = TimeUnit.MILLISECONDS.convert(sqlEnd - sqlStart, TimeUnit.NANOSECONDS);
        
        System.out.println("Full Database Scan Performance (ms):");
        System.out.println("DefaultStorageClient: " + defaultDuration);
        System.out.println("SQLStorageClient: " + sqlDuration);
    }
    
    private void clearStorage(StorageClient storageClient) {
        List<Account> accounts = storageClient.getAllAccounts();
        for (Account account : accounts) {
            List<Mechanism> mechanisms = storageClient.getMechanismsForAccount(account);
            for (Mechanism mechanism : mechanisms) {
                if (mechanism instanceof PushMechanism) {
                    List<PushNotification> notifications = storageClient.getAllNotificationsForMechanism(mechanism);
                    for (PushNotification notification : notifications) {
                        storageClient.removeNotification(notification);
                    }
                }
                storageClient.removeMechanism(mechanism);
            }
            storageClient.removeAccount(account);
        }
    }
    
    private void generateTestData() throws Exception {
        testAccounts = new ArrayList<>();
        testMechanisms = new HashMap<>();
        testNotifications = new HashMap<>();
        
        // Generate accounts
        for (int i = 0; i < NUM_ACCOUNTS; i++) {
            Account account = Account.builder()
                    .setIssuer("Test Issuer " + i)
                    .setAccountName("test.user." + i + "@example.com")
                    .setImageURL("https://example.com/image" + i + ".png")
                    .setBackgroundColor("#FFFFFF")
                    .build();
            testAccounts.add(account);
            
            // Generate mechanisms for this account
            List<Mechanism> mechanisms = new ArrayList<>();
            for (int j = 0; j < MECHANISMS_PER_ACCOUNT; j++) {
                Mechanism mechanism;
                if (j % 2 == 0) {
                    // Create OATH mechanism
                    mechanism = TOTPMechanism.builder()
                            .setMechanismUID(UUID.randomUUID().toString())
                            .setIssuer(account.getIssuer())
                            .setAccountName(account.getAccountName())
                            .setAlgorithm("sha1")
                            .setDigits(6)
                            .setPeriod(30)
                            .setSecret("ABCDEFGHIJKLMNOP")
                            .build();
                } else {
                    // Create Push mechanism
                    mechanism = PushMechanism.builder()
                            .setMechanismUID(UUID.randomUUID().toString())
                            .setIssuer(account.getIssuer())
                            .setAccountName(account.getAccountName())
                            .setAuthenticationEndpoint("https://example.com/push" + i + j)
                            .setRegistrationEndpoint("https://example.com/register" + i + j)
                            .setSecret("testKey" + i + j)
                            .build();
                    
                    // Generate notifications for push mechanisms
                    List<PushNotification> notifications = new ArrayList<>();
                    for (int k = 0; k < NOTIFICATIONS_PER_PUSH; k++) {
                        Calendar timeAdded = Calendar.getInstance();
                        timeAdded.add(Calendar.HOUR, -k);
                        
                        PushNotification notification = PushNotification.builder()
                                .setMechanismUID(mechanism.getMechanismUID())
                                .setMessage("Authenticate?")
                                .setMessageId("message" + i + j + k)
                                .setChallenge("challenge" + k)
                                .setAmlbCookie("amlbCookie" + k)
                                .setTimeAdded(timeAdded)
                                .setTimeExpired(timeAdded)
                                .setTtl(120)
                                .build();
                        notifications.add(notification);
                    }
                    testNotifications.put(mechanism, notifications);
                }
                mechanisms.add(mechanism);
            }
            testMechanisms.put(account, mechanisms);
        }
    }
    
    private void setupFullDataSet() {
        // Store accounts, mechanisms, and notifications for testing retrieval
        for (Account account : testAccounts) {
            defaultStorageClient.setAccount(account);
            sqlStorageClient.setAccount(account);
            
            List<Mechanism> mechanisms = testMechanisms.get(account);
            for (Mechanism mechanism : mechanisms) {
                defaultStorageClient.setMechanism(mechanism);
                sqlStorageClient.setMechanism(mechanism);
                
                if (mechanism instanceof PushMechanism) {
                    List<PushNotification> notifications = testNotifications.get(mechanism);
                    if (notifications != null) {
                        for (PushNotification notification : notifications) {
                            defaultStorageClient.setNotification(notification);
                            sqlStorageClient.setNotification(notification);
                        }
                    }
                }
            }
        }
    }
}
