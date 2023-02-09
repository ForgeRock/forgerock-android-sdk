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

import org.forgerock.android.auth.exception.InvalidPolicyException;
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
    public void testRegisterSinglePolicy() throws InvalidPolicyException {
        FRAPolicyEvaluator policyEvaluator = new FRAPolicyEvaluator.FRAPolicyEvaluatorBuilder()
                .withPolicy(new DummyPolicy())
                .build();

        assertEquals(policyEvaluator.getPolicies().size(), 1);
    }

    @Test
    public void testRegisterMultiplePolicies() throws InvalidPolicyException {
        FRAPolicyEvaluator policyEvaluator = new FRAPolicyEvaluator.FRAPolicyEvaluatorBuilder()
                .withPolicy(new BiometricAvailablePolicy())
                .withPolicy(new DeviceTamperingPolicy())
                .withPolicy(new DummyWithDataPolicy())
                .withPolicy(new DummyPolicy())
                .build();

        assertEquals(policyEvaluator.getPolicies().size(), 4);
    }

    @Test
    public void testRegisterMultiplePoliciesBuilderOrderDoesNotMatter() throws InvalidPolicyException {
        FRAPolicyEvaluator policyEvaluator = new FRAPolicyEvaluator.FRAPolicyEvaluatorBuilder()
                .withPolicy(new DummyPolicy())
                .withPolicies(FRAPolicyEvaluator.DEFAULT_POLICIES)
                .build();

        assertEquals(policyEvaluator.getPolicies().size(), 3);
    }

    @Test
    public void testRegisterMultiplePoliciesWhenSamePolicyIsRegisteredTwice() throws InvalidPolicyException {
        FRAPolicyEvaluator policyEvaluator = FRAPolicyEvaluator.builder()
                .withPolicy(new DeviceTamperingPolicy())
                .withPolicy(new DummyWithDataPolicy())
                .withPolicy(new DummyWithDataPolicy())
                .build();

        assertEquals(policyEvaluator.getPolicies().size(), 3);
    }

    @Test
    public void testRegisterMultiplePoliciesWhenInvalidPolicyIsRegistered() {
        try {
            FRAPolicyEvaluator.builder()
                    .withPolicy(new DummyWithDataPolicy())
                    .withPolicy(new InvalidFakePolicy())
                    .build();
        } catch (Exception e) {
            assertTrue(e instanceof InvalidPolicyException);
            assertTrue(e.getLocalizedMessage().contains(InvalidFakePolicy.class.getSimpleName()));
        }
    }

    @Test
    public void testSingleCustomPolicySuccessAgainstMultiplePoliciesFromAccount() throws InvalidPolicyException {
        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .setPolicies("{\"dummy\": { },\"dummyWithData\": { }}")
                .build();

        DummyPolicy policy = new DummyPolicy();

        List<FRAPolicy> policyList = new ArrayList<>();
        policyList.add(policy);

        FRAPolicyEvaluator customPolicyEvaluator = FRAPolicyEvaluator.builder()
                .withPolicies(policyList)
                .build();

        FRAPolicyEvaluator.Result result = customPolicyEvaluator.evaluate(context, account);

        assertTrue(result.isComply());
        assertNull(result.getNonCompliancePolicy());
        assertEquals(customPolicyEvaluator.getPolicies().size(), 1);
    }

    @Test
    public void testSingleCustomPolicyFailureAgainstMultiplePoliciesFromAccount() throws InvalidPolicyException {
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

        FRAPolicyEvaluator.Result result = customPolicyEvaluator.evaluate(context, account);

        assertFalse(result.isComply());
        assertEquals(result.getNonCompliancePolicy().getName(), policy.getName());
        assertEquals(customPolicyEvaluator.getPolicies().size(), 1);
    }

    @Test
    public void testSingleCustomPolicySuccessAgainstZeroPoliciesFromAccount() throws InvalidPolicyException {
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

        FRAPolicyEvaluator.Result result = customPolicyEvaluator.evaluate(context, account);

        assertTrue(result.isComply());
    }

    @Test
    public void testSingleCustomPolicySuccessAgainstNoMatchingPolicyFromAccount() throws InvalidPolicyException {
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

        FRAPolicyEvaluator.Result result = customPolicyEvaluator.evaluate(context, account);

        assertTrue(result.isComply());
    }


    @Test
    public void testMultipleCustomPolicySuccessAgainstMultiplePoliciesFromAccount() throws InvalidPolicyException {
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

        FRAPolicyEvaluator.Result result = customPolicyEvaluator.evaluate(context, account);

        assertTrue(result.isComply());
        assertNull(result.getNonCompliancePolicy());
        assertEquals(customPolicyEvaluator.getPolicies().size(), 2);
    }

    @Test
    public void testMultipleCustomPolicySuccessAgainstMultiplePoliciesFromURI() throws InvalidPolicyException {
        String combinedUri = "mfauth://totp/Forgerock:demo?" +
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
                "period=30&";

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

        FRAPolicyEvaluator.Result result = customPolicyEvaluator.evaluate(context, combinedUri);

        assertTrue(result.isComply());
        assertNull(result.getNonCompliancePolicy());
        assertEquals(customPolicyEvaluator.getPolicies().size(), 2);
    }

}