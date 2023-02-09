/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.forgerock.android.auth.exception.InvalidPolicyException;
import org.forgerock.android.auth.policy.FRAPolicy;
import org.forgerock.android.auth.policy.BiometricAvailablePolicy;
import org.forgerock.android.auth.policy.DeviceTamperingPolicy;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
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

    private FRAPolicyEvaluator(List<FRAPolicy> policies) {
        this.policies = policies;
    }

    /**
     * Return the list of polices to be evaluated.
     * @return The List of Policy objects.
     */
    public List<FRAPolicy> getPolicies() {
        return this.policies;
    }

    /**
     * Result of the FRAPolicyEvaluator.
     */
    public static class Result {
        private final FRAPolicy nonCompliancePolicy;
        private final boolean comply;

        /**
         * Return if all policies were evaluated successfully.
         * @return {@code true}, if all Policies are complying, {@code false} if any policy fail.
         */
        public boolean isComply() {
            return comply;
        }

        /**
         * Return the Policy that fail to comply.
         * @return The Policy object.
         */
        @Nullable
        public FRAPolicy getNonCompliancePolicy() {
            return nonCompliancePolicy;
        }

        Result(boolean comply, FRAPolicy nonCompliancePolicy) {
            this.comply = comply;
            this.nonCompliancePolicy = nonCompliancePolicy;
        }
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

        /**
         * Sets a list of policies to be evaluated.
         * This method does not check if a policy has been added to the list previously.
         * @param policies The list with custom policies.
         * @return this builder
         */
        public FRAPolicyEvaluatorBuilder withPolicies(@NonNull List<FRAPolicy> policies) {
            if (this.policies == null) {
                this.policies = policies;
            } else {
                this.policies.addAll(policies);
            }
            return this;
        }

        /**
         * Sets a single policy to the list of policies to be evaluated.
         * This method does not check if a policy has been added to the list previously.
         * @param policy The list with custom policies.
         * @return this builder
         */
        public FRAPolicyEvaluatorBuilder withPolicy(@NonNull FRAPolicy policy) {
            if (this.policies == null) {
                this.policies = new ArrayList<>();
            }
            this.policies.add(policy);
            return this;
        }

        /**
         * Produces the FRAPolicyEvaluator object that was being constructed.
         * If no list of policies is provided, it uses the DEFAULT_POLICIES.
         * @return The FRAPolicyEvaluator.
         * @throws InvalidPolicyException if a valid policy name is not provided.
         */
        public FRAPolicyEvaluator build() throws InvalidPolicyException {
            if (this.policies == null) {
                Logger.warn(TAG, "No custom policies provided, using DEFAULT_POLICIES.");
                this.policies = DEFAULT_POLICIES;
            } else {
                for (FRAPolicy policy : this.policies) {
                    if (policy.getName() == null || policy.getName().isEmpty()) {
                        throw new InvalidPolicyException("Error with the policy " +
                                 policy.getClass().getSimpleName() +
                                ": The policy name cannot be null or empty.");
                    }

                }
            }

            return new FRAPolicyEvaluator(this.policies);
        }
    }

    /**
     * Evaluate all registered Policies against an Account.
     * @return the result of the Policies evaluation.
     */
    public Result evaluate(@NonNull Context context, @NonNull Account account) {
        return processPolicies(context, account.getPolicies());
    }

    /**
     * Evaluate all registered Policies against an URI.
     * @return the result of the Policies evaluation.
     */
    public Result evaluate(@NonNull Context context, @NonNull String uri) {
        String policies = getPoliciesFromURI(uri);
        return processPolicies(context, policies);
    }

    /**
     * Return if a policy was attached to the {@link Account}
     * @param account the Account object
     * @param policyName the policy name
     * @return {@code true}, if the policy was attached to the Account, {@code false} otherwise.
     */
    public boolean isPolicyAttached(@NonNull Account account, @NonNull String policyName) {
        try {
            if (account.getPolicies() != null) {
                JSONObject policiesJson = new JSONObject(account.getPolicies());
                for (Iterator<String> it = policiesJson.keys(); it.hasNext(); ) {
                    String key = it.next();
                    if(key.equals(policyName)) {
                        return true;
                    }
                }
            }
        } catch (JSONException e) {
            Logger.warn(TAG, "Attempt to parse policies to JSON failed.");
        }
        return false;
    }

    private Result processPolicies(Context context, String policies) {
        if (policies != null) {
            List<FRAPolicy> targetedPolicies = getPoliciesToVerify(policies);
            for(FRAPolicy policy : targetedPolicies) {
                if(!policy.evaluate(context)) {
                    return new Result(false, policy);
                }
            }
        }
        return new Result(true, null);
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
