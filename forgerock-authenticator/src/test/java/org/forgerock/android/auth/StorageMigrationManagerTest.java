/*
 * Copyright (c) 2020 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.content.SharedPreferences;

import org.forgerock.android.auth.exception.MechanismCreationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class StorageMigrationManagerTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private SQLStorageClient mockSqlStorage;
    
    @Mock
    private DefaultStorageClient mockDefaultStorage;
    
    @Mock
    private SharedPreferences mockSharedPreferences;
    
    @Mock
    private SharedPreferences.Editor mockEditor;
    
    private StorageMigrationManager migrationManager;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        // Set up SharedPreferences mock
        when(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences);
        when(mockSharedPreferences.edit()).thenReturn(mockEditor);
        when(mockEditor.putBoolean(anyString(), any(Boolean.class))).thenReturn(mockEditor);

        // Custom implementation of StorageMigrationManager for testing
        migrationManager = new StorageMigrationManager(mockContext, mockSqlStorage) {
            @Override
            protected DefaultStorageClient createSourceStorage(Context context) {
                return mockDefaultStorage;
            }
        };
    }

    @Test
    public void testIsMigrationNeeded_AlreadyMigrated() {
        // Setup - migration has been performed
        when(mockSharedPreferences.getBoolean(eq("hasMigrated"), eq(false))).thenReturn(true);

        // Test
        boolean result = migrationManager.isMigrationNeeded();

        // Verify
        assertFalse(result);
        verify(mockDefaultStorage, never()).isEmpty();
    }

    @Test
    public void testIsMigrationNeeded_SourceStorageEmpty() {
        // Setup - migration has not been performed but source is empty
        when(mockSharedPreferences.getBoolean(eq("hasMigrated"), eq(false))).thenReturn(false);
        when(mockDefaultStorage.isEmpty()).thenReturn(true);

        // Test
        boolean result = migrationManager.isMigrationNeeded();

        // Verify
        assertFalse(result);
        verify(mockDefaultStorage).isEmpty();
        verify(mockEditor).putBoolean(eq("hasMigrated"), eq(true));
        verify(mockEditor).apply();
    }

    @Test
    public void testIsMigrationNeeded_MigrationRequired() {
        // Setup - migration has not been performed and source has data
        when(mockSharedPreferences.getBoolean(eq("hasMigrated"), eq(false))).thenReturn(false);
        when(mockDefaultStorage.isEmpty()).thenReturn(false);

        // Test
        boolean result = migrationManager.isMigrationNeeded();

        // Verify
        assertTrue(result);
        verify(mockDefaultStorage).isEmpty();
    }

    @Test
    public void testMigrateData_Success() throws Exception {
        // Setup - configure source data
        Calendar timeAdded = Calendar.getInstance();

        Account account = Account.builder()
                .setAccountName("test-account")
                .setIssuer("test-issuer")
                .build();
        List<Account> accounts = Arrays.asList(account);

        when(mockDefaultStorage.isEmpty()).thenReturn(false);
        when(mockDefaultStorage.getAllAccounts()).thenReturn(accounts);

        OathMechanism mechanism = TOTPMechanism.builder()
                .setMechanismUID("test-mechanism-uid")
                .setIssuer("test-issuer")
                .setAccountName("test-account")
                .setAlgorithm("sha256")
                .setSecret("secret")
                .setDigits(6)
                .setPeriod(30)
                .build();
        List<Mechanism> mechanisms = Arrays.asList(mechanism);
        when(mockDefaultStorage.getMechanismsForAccount(account)).thenReturn(mechanisms);

        List<PushNotification> notifications = new ArrayList<>();
        when(mockDefaultStorage.getAllNotificationsForMechanism(mechanism)).thenReturn(notifications);

        PushDeviceToken deviceToken = new PushDeviceToken("test-token", timeAdded);
        when(mockDefaultStorage.getPushDeviceToken()).thenReturn(deviceToken);

        // Setup success responses for target storage
        when(mockSqlStorage.setAccount(any(Account.class))).thenReturn(true);
        when(mockSqlStorage.setMechanism(any(Mechanism.class))).thenReturn(true);
        when(mockSqlStorage.setPushDeviceToken(any(PushDeviceToken.class))).thenReturn(true);

        // Test
        boolean result = migrationManager.migrateData(true);

        // Verify
        assertTrue(result);
        verify(mockSqlStorage).setAccount(account);
        verify(mockSqlStorage).setMechanism(mechanism);
        verify(mockSqlStorage).setPushDeviceToken(deviceToken);
        verify(mockDefaultStorage).removeAll(); // Should delete source data
        verify(mockEditor).putBoolean(eq("hasMigrated"), eq(true));
        verify(mockEditor).apply();
    }

    @Test
    public void testMigrateData_WithPushNotifications() throws Exception {
        // Setup - configure source data with push notifications
        Calendar timeAdded = Calendar.getInstance();

        Account account = Account.builder()
                .setAccountName("test-account")
                .setIssuer("test-issuer")
                .build();
        List<Account> accounts = Arrays.asList(account);
        when(mockDefaultStorage.isEmpty()).thenReturn(false);
        when(mockDefaultStorage.getAllAccounts()).thenReturn(accounts);

        PushMechanism pushMechanism = PushMechanism.builder()
                .setMechanismUID("test-push-mechanism-uid")
                .setIssuer("test-issuer")
                .setAccountName("test-account")
                .setAuthenticationEndpoint("https://test.com/authenticate")
                .setRegistrationEndpoint("https://test.com/register")
                .build();
        List<Mechanism> mechanisms = Arrays.asList(pushMechanism);
        when(mockDefaultStorage.getMechanismsForAccount(account)).thenReturn(mechanisms);

        PushNotification notification = PushNotification.builder()
                .setMechanismUID("test-push-mechanism-uid")
                .setMessageId("test-message-id")
                .setMessage("Test notification")
                .setTimeAdded(timeAdded)
                .setTtl(120)
                .build();
        List<PushNotification> notifications = Arrays.asList(notification);
        when(mockDefaultStorage.getAllNotificationsForMechanism(pushMechanism)).thenReturn(notifications);

        // Setup success responses for target storage
        when(mockSqlStorage.setAccount(any(Account.class))).thenReturn(true);
        when(mockSqlStorage.setMechanism(any(Mechanism.class))).thenReturn(true);
        when(mockSqlStorage.setNotification(any(PushNotification.class))).thenReturn(true);

        // Test
        boolean result = migrationManager.migrateData(false);

        // Verify
        assertTrue(result);
        verify(mockSqlStorage).setAccount(account);
        verify(mockSqlStorage).setMechanism(pushMechanism);
        verify(mockSqlStorage).setNotification(notification);
        verify(mockDefaultStorage, never()).removeAll(); // Should not delete source data
    }

    @Test
    public void testMigrateData_AccountMigrationFails() {
        // Setup - account migration fails
        Account account = Account.builder()
                .setAccountName("test-account")
                .setIssuer("test-issuer")
                .build();
        List<Account> accounts = Arrays.asList(account);
        when(mockDefaultStorage.isEmpty()).thenReturn(false);
        when(mockDefaultStorage.getAllAccounts()).thenReturn(accounts);
        when(mockSqlStorage.setAccount(any(Account.class))).thenReturn(false);

        // Test
        boolean result = migrationManager.migrateData(true);

        // Verify
        assertFalse(result);
        verify(mockSqlStorage).setAccount(account);
        verify(mockDefaultStorage, never()).removeAll(); // Should not delete source data on failure
        verify(mockEditor, never()).putBoolean(eq("hasMigrated"), eq(true));
    }

    @Test
    public void testMigrateData_MechanismMigrationFails() throws Exception {
        // Setup - mechanism migration fails
        Account account = Account.builder()
                .setAccountName("test-account")
                .setIssuer("test-issuer")
                .build();
        List<Account> accounts = Arrays.asList(account);
        OathMechanism mechanism = TOTPMechanism.builder()
                .setMechanismUID("test-mechanism-uid")
                .setIssuer("test-issuer")
                .setAccountName("test-account")
                .setAlgorithm("sha256")
                .setSecret("secret")
                .setDigits(6)
                .setPeriod(30)
                .build();
        List<Mechanism> mechanisms = Arrays.asList(mechanism);

        when(mockDefaultStorage.isEmpty()).thenReturn(false);
        when(mockDefaultStorage.getAllAccounts()).thenReturn(accounts);
        when(mockDefaultStorage.getMechanismsForAccount(account)).thenReturn(mechanisms);
        when(mockSqlStorage.setAccount(any(Account.class))).thenReturn(true);
        when(mockSqlStorage.setMechanism(any(Mechanism.class))).thenReturn(false);

        // Test
        boolean result = migrationManager.migrateData(true);

        // Verify
        assertFalse(result);
        verify(mockSqlStorage).setAccount(account);
        verify(mockSqlStorage).setMechanism(mechanism);
        verify(mockDefaultStorage, never()).removeAll();
    }

    @Test
    public void testResetMigrationStatus() {
        // Test
        migrationManager.resetMigrationStatus();

        // Verify
        verify(mockEditor).putBoolean(eq("hasMigrated"), eq(false));
        verify(mockEditor).apply();
    }
}
