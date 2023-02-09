/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.policy;

import android.content.Context;

import org.json.JSONObject;

/**
 * The FRAPolicy is an abstract Policy that provides general guidance on implementing policies
 * to enforce the security of an Authenticator app.
 *
 * A policy must contain an unique name and instructions to determinate whether a condition is
 * valid at a particular time. The policy may optionally contain some data to be used in the
 * validation procedure.
 *
 * JSON representation of a policy:
 * {"policyName" : { policyData }}
 */
public abstract class FRAPolicy {

    JSONObject data;

    /**
     * Set the data used for policy validation.
     *
     * @param data The policy data
     */
    public void setData(JSONObject data) {
        this.data = data;
    }

    /**
     * Get the data used for policy validation.
     *
     * @return The policy data
     */
    public JSONObject getData() {
        return this.data;
    }

    /**
     * Retrieve the name of the Policy.
     *
     * @return The name of the Policy
     */
    public abstract String getName();

    /**
     * Evaluate the policy compliance.
     *
     * @param context  The Application Context.
     * @return {@code true} if the policy comply, {@code false} otherwise
     */
    public abstract boolean evaluate(Context context);

}
