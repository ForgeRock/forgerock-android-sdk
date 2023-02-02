/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SecurityPolicyTest extends FRABaseTest {

    private Context context;
    private SecurityPolicy securityPolicy;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        securityPolicy = spy(new SecurityPolicy());
    }

    @Test
    public void testNoSecurityPolicyViolation() {
        doReturn(true).when(securityPolicy).isBiometricCapable(any());
        doReturn(false).when(securityPolicy).isDeviceRooted(any(), anyDouble());

        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .setEnforceBiometricAuthentication(true)
                .setEnforceDeviceTamperingDetection(true)
                .setDeviceTamperingScoreThreshold(0.5)
                .build();

        assertTrue(account.isBiometricAuthenticationEnforced());
        assertTrue(account.isDeviceTamperingDetectionEnforced());
        assertEquals(account.getDeviceTamperingScoreThreshold(), 0.5, 0.0);

        assertFalse(securityPolicy.violatePolicies(context, account));
        assertFalse(securityPolicy.violateBiometricAuthenticationPolicy(context, account));
        assertFalse(securityPolicy.violateDeviceTamperingPolicy(context, account));
    }

    @Test
    public void testBiometricAuthenticationSecurityPolicyViolation() {
        doReturn(false).when(securityPolicy).isBiometricCapable(any());
        doReturn(false).when(securityPolicy).isDeviceRooted(any(), anyDouble());

        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .setEnforceBiometricAuthentication(true)
                .setEnforceDeviceTamperingDetection(true)
                .setDeviceTamperingScoreThreshold(0.5)
                .build();

        assertTrue(account.isBiometricAuthenticationEnforced());
        assertTrue(account.isDeviceTamperingDetectionEnforced());
        assertEquals(account.getDeviceTamperingScoreThreshold(), 0.5, 0.0);

        assertTrue(securityPolicy.violatePolicies(context, account));
        assertTrue(securityPolicy.violateBiometricAuthenticationPolicy(context, account));
        assertFalse(securityPolicy.violateDeviceTamperingPolicy(context, account));
    }

    @Test
    public void testDeviceTamperingSecurityPolicyViolation() {
        doReturn(true).when(securityPolicy).isBiometricCapable(any());
        doReturn(true).when(securityPolicy).isDeviceRooted(any(), anyDouble());

        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .setEnforceBiometricAuthentication(true)
                .setEnforceDeviceTamperingDetection(true)
                .setDeviceTamperingScoreThreshold(0.5)
                .build();

        assertTrue(account.isBiometricAuthenticationEnforced());
        assertTrue(account.isDeviceTamperingDetectionEnforced());
        assertEquals(account.getDeviceTamperingScoreThreshold(), 0.5, 0.0);

        assertTrue(securityPolicy.violatePolicies(context, account));
        assertFalse(securityPolicy.violateBiometricAuthenticationPolicy(context, account));
        assertTrue(securityPolicy.violateDeviceTamperingPolicy(context, account));
    }

    @Test
    public void testNoSecurityPolicyViolationWithEnforcementDisabled() {
        doReturn(false).when(securityPolicy).isBiometricCapable(any());
        doReturn(true).when(securityPolicy).isDeviceRooted(any(), anyDouble());

        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .setEnforceBiometricAuthentication(false)
                .setEnforceDeviceTamperingDetection(false)
                .setDeviceTamperingScoreThreshold(0.0)
                .build();

        assertFalse(account.isBiometricAuthenticationEnforced());
        assertFalse(account.isDeviceTamperingDetectionEnforced());
        assertEquals(account.getDeviceTamperingScoreThreshold(), 0.0, 0.0);

        assertFalse(securityPolicy.violatePolicies(context, account));
        assertFalse(securityPolicy.violateBiometricAuthenticationPolicy(context, account));
        assertFalse(securityPolicy.violateDeviceTamperingPolicy(context, account));
    }

    @Test
    public void testSecurityPolicyViolationForAllPolicies() {
        doReturn(false).when(securityPolicy).isBiometricCapable(any());
        doReturn(true).when(securityPolicy).isDeviceRooted(any(), anyDouble());

        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .setEnforceBiometricAuthentication(true)
                .setEnforceDeviceTamperingDetection(true)
                .setDeviceTamperingScoreThreshold(0.5)
                .build();

        assertTrue(account.isBiometricAuthenticationEnforced());
        assertTrue(account.isDeviceTamperingDetectionEnforced());
        assertEquals(account.getDeviceTamperingScoreThreshold(), 0.5, 0.0);

        assertTrue(securityPolicy.violatePolicies(context, account));
        assertTrue(securityPolicy.violateBiometricAuthenticationPolicy(context, account));
        assertTrue(securityPolicy.violateDeviceTamperingPolicy(context, account));
    }

}
