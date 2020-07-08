/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.*;

/**
 * Callbacks that accept user input often need to validate that input either on the client side, the server side
 * or both.  Such callbacks should extend this base class.
 */
@NoArgsConstructor
@Getter
public abstract class AbstractValidatedCallback extends AbstractCallback {

    /**
     * Return the validation policies that should be applied to the input collected by this callback.  These policies
     * are represented by a name string.
     *
     * @return validation policies
     */
    private JSONObject policies;

    /**
     * Return the list of failed policies for this callback.
     *
     * @return list of failed policies
     */
    private List<FailedPolicy> failedPolicies;

    private Boolean validateOnly;

    public AbstractValidatedCallback(JSONObject jsonObject, int index) throws JSONException {
        super(jsonObject, index);
    }

    @Override
    protected void setAttribute(String name, Object value) {
        switch (name) {
            case "policies":
                policies = ((JSONObject) value);
                break;
            case "failedPolicies":
                prepareFailedPolicy((JSONArray) value);
                break;
            case "validateOnly":
                validateOnly = (Boolean) value;
            default:
                //ignore
        }
    }

    public abstract String getPrompt();

    public void setValidateOnly(boolean validateOnly) {
        setValue(validateOnly, 1);
    }

    private void prepareFailedPolicy(JSONArray array) {
        List<AbstractValidatedCallback.FailedPolicy> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject failedPolicy = null;
            try {
                failedPolicy = new JSONObject(array.getString(i));

            JSONObject params = failedPolicy.optJSONObject("params");

            HashMap<String, Object> paramMap = new HashMap<>();
            if (params != null) {
                Iterator<String> iterator = params.keys();
                while (iterator.hasNext()) {
                    String k = iterator.next();
                    Object v = params.opt(k);
                    paramMap.put(k, v);
                }
            }

            list.add(new FailedPolicy(paramMap, failedPolicy.getString("policyRequirement")));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        failedPolicies = Collections.unmodifiableList(list);
    }


    @Getter
    @AllArgsConstructor
    public static class FailedPolicy implements Serializable {
        private HashMap<String, Object> params;
        private String policyRequirement;

        public String format(String prompt, String message) {
            message = message.replace("{prompt}", prompt);
            if (params != null) {
                for (Map.Entry entry : params.entrySet()) {
                    message = message.replace("{" + entry.getKey() + "}", entry.getValue().toString());
                }
            }
            return message;
        }
    }
}
