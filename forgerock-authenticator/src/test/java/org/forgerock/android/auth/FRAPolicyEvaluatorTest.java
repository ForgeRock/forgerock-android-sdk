/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.forgerock.android.auth.policy.BiometricAvailablePolicy;
import org.forgerock.android.auth.policy.DeviceTamperingPolicy;
import org.forgerock.android.auth.policy.FRAPolicy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class FRAPolicyEvaluatorTest extends FRABaseTest {

    private Context context;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testSingleCustomPolicySuccessAgainstMultiplePoliciesFromAccount() {
        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .setPolicies(POLICIES)
                .build();

        BiometricAvailablePolicy policy = spy(new BiometricAvailablePolicy());

        List<FRAPolicy> policyList = new ArrayList<>();
        policyList.add(policy);

        FRAPolicyEvaluator customPolicyEvaluator = spy(FRAPolicyEvaluator.builder()
                .withPolicies(policyList)
                .build());

        doReturn(true).when(policy).evaluate(context);

        boolean complaint = customPolicyEvaluator.evaluate(context, account);

        assertTrue(complaint);
        assertNull(customPolicyEvaluator.getNonCompliancePolicy());
        assertEquals(customPolicyEvaluator.getTargetedPolicies().size(), 1);
    }

    @Test
    public void testSingleCustomPolicyFailureAgainstMultiplePoliciesFromAccount() {
        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .setPolicies(POLICIES)
                .build();

        BiometricAvailablePolicy policy = spy(new BiometricAvailablePolicy());

        List<FRAPolicy> policyList = new ArrayList<>();
        policyList.add(policy);

        FRAPolicyEvaluator customPolicyEvaluator = spy(new FRAPolicyEvaluator
                .FRAPolicyEvaluatorBuilder()
                .withPolicies(policyList)
                .build());

        doReturn(false).when(policy).evaluate(context);

        boolean complaint = customPolicyEvaluator.evaluate(context, account);

        assertFalse(complaint);
        assertEquals(customPolicyEvaluator.getNonCompliancePolicy().getName(), policy.getName());
        assertEquals(customPolicyEvaluator.getTargetedPolicies().size(), 1);
    }

    @Test
    public void testSingleCustomPolicySuccessAgainstZeroPoliciesFromAccount() {
        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .build();

        BiometricAvailablePolicy policy = spy(new BiometricAvailablePolicy());

        List<FRAPolicy> policyList = new ArrayList<>();
        policyList.add(policy);

        FRAPolicyEvaluator customPolicyEvaluator = spy(new FRAPolicyEvaluator
                .FRAPolicyEvaluatorBuilder()
                .withPolicies(policyList)
                .build());

        doReturn(true).when(policy).evaluate(context);

        boolean complaint = customPolicyEvaluator.evaluate(context, account);

        assertTrue(complaint);
        assertTrue(customPolicyEvaluator.getTargetedPolicies().isEmpty());
    }

    @Test
    public void testSingleCustomPolicySuccessAgainstNoMatchingPolicyFromAccount() {
        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .setPolicies("{\"deviceTampering\": {\"score\": 0.8}}")
                .build();

        BiometricAvailablePolicy policy = spy(new BiometricAvailablePolicy());

        List<FRAPolicy> policyList = new ArrayList<>();
        policyList.add(policy);

        FRAPolicyEvaluator customPolicyEvaluator = spy(new FRAPolicyEvaluator
                .FRAPolicyEvaluatorBuilder()
                .withPolicies(policyList)
                .build());

        doReturn(true).when(policy).evaluate(context);

        boolean complaint = customPolicyEvaluator.evaluate(context, account);

        assertTrue(complaint);
        assertTrue(customPolicyEvaluator.getTargetedPolicies().isEmpty());
    }


    @Test
    public void testMultipleCustomPolicySuccessAgainstMultiplePoliciesFromAccount() {
        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .setPolicies(POLICIES)
                .build();

        BiometricAvailablePolicy biometricAvailablePolicy = spy(new BiometricAvailablePolicy());
        DeviceTamperingPolicy deviceTamperingPolicy = spy(new DeviceTamperingPolicy());

        List<FRAPolicy> policyList = new ArrayList<>();
        policyList.add(biometricAvailablePolicy);
        policyList.add(deviceTamperingPolicy);

        FRAPolicyEvaluator customPolicyEvaluator = spy(new FRAPolicyEvaluator
                .FRAPolicyEvaluatorBuilder()
                .withPolicies(policyList)
                .build());

        doReturn(true).when(biometricAvailablePolicy).evaluate(context);
        doReturn(true).when(deviceTamperingPolicy).evaluate(context);

        boolean complaint = customPolicyEvaluator.evaluate(context, account);

        assertTrue(complaint);
        assertNull(customPolicyEvaluator.getNonCompliancePolicy());
        assertEquals(customPolicyEvaluator.getTargetedPolicies().size(), 2);
    }

    @Test
    public void testMultipleCustomPolicySuccessAgainstMultiplePoliciesFromURI() {
        String combinedUri = "mfauth://mfa/Forgerock:demo?" +
                "a=aHR0cHM6Ly9mb3JnZXJvY2suZXhhbXBsZS5jb20vb3BlbmFtL2pzb24vcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPWF1dGhlbnRpY2F0ZQ&" +
                "image=aHR0cDovL3NlYXR0bGV3cml0ZXIuY29tL3dwLWNvbnRlbnQvdXBsb2Fkcy8yMDEzLzAxL3dlaWdodC13YXRjaGVycy1zbWFsbC5naWY&" +
                "b=ff00ff&" +
                "r=aHR0cHM6Ly9mb3JnZXJvY2suZXhhbXBsZS5jb20vb3BlbmFtL2pzb24vcHVzaC9zbnMvbWVzc2FnZT9fYWN0aW9uPXJlZ2lzdGVy&" +
                "s=ryJkqNRjXYd_nX523672AX_oKdVXrKExq-VjVeRKKTc&" +
                "c=Daf8vrc8onKu-dcptwCRS9UHmdui5u16vAdG2HMU4w0&" +
                "l=YW1sYmNvb2tpZT0wMQ==&" +
                "m=9326d19c-4d08-4538-8151-f8558e71475f1464361288472&" +
                "policies=eyJiaW9tZXRyaWNBdmFpbGFibGUiOiB7IH0sImRldmljZVRhbXBlcmluZyI6IHsic2NvcmUiOiAwLjh9fQ&" +
                "digits=6&" +
                "secret=R2PYFZRISXA5L25NVSSYK2RQ6E======&" +
                "period=30&" +
                "type=totp";

        BiometricAvailablePolicy biometricAvailablePolicy = spy(new BiometricAvailablePolicy());
        DeviceTamperingPolicy deviceTamperingPolicy = spy(new DeviceTamperingPolicy());

        List<FRAPolicy> policyList = new ArrayList<>();
        policyList.add(biometricAvailablePolicy);
        policyList.add(deviceTamperingPolicy);

        FRAPolicyEvaluator customPolicyEvaluator = spy(new FRAPolicyEvaluator
                .FRAPolicyEvaluatorBuilder()
                .withPolicies(policyList)
                .build());

        doReturn(true).when(biometricAvailablePolicy).evaluate(context);
        doReturn(true).when(deviceTamperingPolicy).evaluate(context);

        boolean complaint = customPolicyEvaluator.evaluate(context, combinedUri);

        assertTrue(complaint);
        assertNull(customPolicyEvaluator.getNonCompliancePolicy());
        assertEquals(customPolicyEvaluator.getTargetedPolicies().size(), 2);
    }

}