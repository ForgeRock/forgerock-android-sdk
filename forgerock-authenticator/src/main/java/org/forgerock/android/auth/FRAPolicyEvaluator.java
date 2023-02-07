/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import androidx.annotation.NonNull;

import org.forgerock.android.auth.policy.FRAPolicy;
import org.forgerock.android.auth.policy.BiometricAvailablePolicy;
import org.forgerock.android.auth.policy.DeviceTamperingPolicy;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The Policy Evaluator is used by the SDK to enforce Policy rules, such as Device Tampering Policy.
 * It consist of one or more {@link FRAPolicy} objects. Each Policy contain instructions that
 * determine whether it comply to a particular condition at a particular time.
 */
public class FRAPolicyEvaluator {

    private final List<FRAPolicy> policies;

    private static final String TAG = FRAPolicyEvaluator.class.getSimpleName();

    public static final List<FRAPolicy> DEFAULT_POLICIES = new ArrayList<>();

    static {
        DEFAULT_POLICIES.add(new DeviceTamperingPolicy());
        DEFAULT_POLICIES.add(new BiometricAvailablePolicy());
    }

    private FRAPolicy nonCompliancePolicy;
    private List<FRAPolicy> targetedPolicies = new ArrayList<>();

    private FRAPolicyEvaluator(List<FRAPolicy> policies) {
        this.policies = policies;
    }

    /**
     * Obtain FRAPolicyEvaluator builder. Policies can be configured in the builder, like setting a
     * new custom Policy.
     * @return FRAPolicyEvaluatorBuilder the builder to initialize the FRAClient
     * */
    public static FRAPolicyEvaluator.FRAPolicyEvaluatorBuilder builder() {
        return new FRAPolicyEvaluator.FRAPolicyEvaluatorBuilder();
    }

    /**
     * The asynchronous PolicyEvaluator builder.
     */
    public static class FRAPolicyEvaluatorBuilder {
        private List<FRAPolicy> policies;

        /** Sets a custom list of policies.
         * @param policies The list with custom policies.
         * @return this builder
         */
        public FRAPolicyEvaluatorBuilder withPolicies(@NonNull List<FRAPolicy> policies) {
            this.policies = policies;
            return this;
        }

        /**
         * Produces the FRAPolicyEvaluator object that was being constructed.
         * If no list of policies is provided, it uses the DEFAULT_POLICIES.
         * @return The FRAPolicyEvaluator.
         */
        public FRAPolicyEvaluator build() {
            if (this.policies == null) {
                Logger.warn(TAG, "No custom policies provided, using DEFAULT_POLICIES.");
                this.policies = DEFAULT_POLICIES;
            }

            return new FRAPolicyEvaluator(this.policies);
        }
    }

    /**
     * Evaluate all registered Policies against an Account.
     *
     * @return {@code true}, if all Policies are complying, {@code false} if any policy fail.
     */
    public boolean evaluate(Context context, Account account) {
        return processPolicies(context, account.getPolicies());
    }

    /**
     * Evaluate all registered Policies against an URI.
     *
     * @return true, if all Policies are complying, false if any policy fail.
     */
    public boolean evaluate(Context context, String uri) {
        String policies = getPoliciesFromURI(uri);
        return processPolicies(context, policies);
    }

    /**
     * Return the Policy that fail to complain.
     *
     * @return The Policy object.
     */
    public FRAPolicy getNonCompliancePolicy() {
        return nonCompliancePolicy;
    }

    /**
     * Return the list of polices evaluated.
     *
     * @return The List of Policy objects.
     */
    public List<FRAPolicy> getTargetedPolicies() {
        return this.targetedPolicies;
    }

    private boolean processPolicies(Context context, String policies) {
        if (policies != null) {
            this.targetedPolicies = getPoliciesToVerify(policies);

            for(FRAPolicy policy : this.targetedPolicies) {
                if(!policy.evaluate(context)) {
                    nonCompliancePolicy = policy;
                    return false;
                }
            }
        }
        return true;
    }

    private String getPoliciesFromURI(String uri) {
        Logger.debug(TAG, "Obtaining policies from URI.");
        String base64String = MechanismParser.getUriParameters(uri).get(MechanismParser.POLICIES);
        String policies = null;
        if (base64String != null && !base64String.isEmpty()) {
            policies = MechanismParser.getBase64DecodedString(base64String);
        }
        return policies;
    }

    private List<FRAPolicy> getPoliciesToVerify(String policies) {
        List<FRAPolicy> targetedPolicies = new ArrayList<>();
        try {
            JSONObject policiesJson = new JSONObject(policies);
            if(policiesJson.keys().hasNext()) {
                targetedPolicies = new ArrayList<>();
                for(FRAPolicy policy : this.policies) {
                    if(policiesJson.has(policy.getName())) {
                        policy.setData(policiesJson.getJSONObject(policy.getName()));
                        targetedPolicies.add(policy);
                    }
                }
            } else {
                Logger.debug(TAG, "No policies to be verified.");
            }
        } catch (JSONException e) {
            throw new RuntimeException("Error parsing policies data.", e);
        }
        return targetedPolicies;
    }

}
