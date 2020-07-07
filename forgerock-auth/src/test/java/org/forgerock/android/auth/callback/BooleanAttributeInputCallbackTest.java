/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import org.assertj.core.api.Assertions;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BooleanAttributeInputCallbackTest {

    @Test
    public void basicTest() throws JSONException {
        JSONObject raw = new JSONObject("{\n" +
                "            \"type\": \"BooleanAttributeInputCallback\",\n" +
                "            \"output\": [\n" +
                "                {\n" +
                "                    \"name\": \"name\",\n" +
                "                    \"value\": \"happy\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"prompt\",\n" +
                "                    \"value\": \"Happy\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"required\",\n" +
                "                    \"value\": true\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"policies\",\n" +
                "                    \"value\": {\n" +
                "                        \"policyRequirements\": [\n" +
                "                            \"VALID_TYPE\"\n" +
                "                        ],\n" +
                "                        \"fallbackPolicies\": null,\n" +
                "                        \"name\": \"happy\",\n" +
                "                        \"policies\": [\n" +
                "                            {\n" +
                "                                \"policyRequirements\": [\n" +
                "                                    \"VALID_TYPE\"\n" +
                "                                ],\n" +
                "                                \"policyId\": \"valid-type\",\n" +
                "                                \"params\": {\n" +
                "                                    \"types\": [\n" +
                "                                        \"boolean\"\n" +
                "                                    ]\n" +
                "                                }\n" +
                "                            }\n" +
                "                        ],\n" +
                "                        \"conditionalPolicies\": null\n" +
                "                    }\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"failedPolicies\",\n" +
                "                    \"value\": []\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"validateOnly\",\n" +
                "                    \"value\": false\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"value\",\n" +
                "                    \"value\": false\n" +
                "                }\n" +
                "            ],\n" +
                "            \"input\": [\n" +
                "                {\n" +
                "                    \"name\": \"IDToken2\",\n" +
                "                    \"value\": false\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"IDToken2validateOnly\",\n" +
                "                    \"value\": false\n" +
                "                }\n" +
                "            ]\n" +
                "        }");
        BooleanAttributeInputCallback callback = new BooleanAttributeInputCallback(raw, 0);
        Assertions.assertThat(callback.getName()).isEqualTo("happy");
        Assertions.assertThat(callback.getPrompt()).isEqualTo("Happy");
        Assertions.assertThat(callback.isRequired()).isTrue();
        Assertions.assertThat(callback.getPolicies().getString("name")).isEqualTo("happy");
        Assertions.assertThat(callback.getFailedPolicies()).isEmpty();
        Assertions.assertThat(callback.getValidateOnly()).isFalse();
        Assertions.assertThat(callback.getValue()).isFalse();

    }
}