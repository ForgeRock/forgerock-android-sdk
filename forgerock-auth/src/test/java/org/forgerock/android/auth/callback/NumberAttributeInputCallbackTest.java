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
public class NumberAttributeInputCallbackTest {

    @Test
    public void basicTest() throws JSONException {
        JSONObject raw = new JSONObject("{\n" +
                "            \"type\": \"NumberAttributeInputCallback\",\n" +
                "            \"output\": [\n" +
                "                {\n" +
                "                    \"name\": \"name\",\n" +
                "                    \"value\": \"age\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"prompt\",\n" +
                "                    \"value\": \"Age\"\n" +
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
                "                        \"name\": \"age\",\n" +
                "                        \"policies\": [\n" +
                "                            {\n" +
                "                                \"policyRequirements\": [\n" +
                "                                    \"VALID_TYPE\"\n" +
                "                                ],\n" +
                "                                \"policyId\": \"valid-type\",\n" +
                "                                \"params\": {\n" +
                "                                    \"types\": [\n" +
                "                                        \"number\"\n" +
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
                "                    \"value\": null\n" +
                "                }\n" +
                "            ],\n" +
                "            \"input\": [\n" +
                "                {\n" +
                "                    \"name\": \"IDToken1\",\n" +
                "                    \"value\": null\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"IDToken1validateOnly\",\n" +
                "                    \"value\": false\n" +
                "                }\n" +
                "            ]\n" +
                "        }");
        NumberAttributeInputCallback callback = new NumberAttributeInputCallback(raw, 0);
        Assertions.assertThat(callback.getName()).isEqualTo("age");
        Assertions.assertThat(callback.getPrompt()).isEqualTo("Age");
        Assertions.assertThat(callback.isRequired()).isTrue();
        Assertions.assertThat(callback.getPolicies().getString("name")).isEqualTo("age");
        Assertions.assertThat(callback.getFailedPolicies()).isEmpty();
        Assertions.assertThat(callback.getValidateOnly()).isFalse();
        Assertions.assertThat(callback.getValue()).isNull();


    }

    @Test
    public void basicInteger() throws JSONException {
        JSONObject raw = new JSONObject("{\n" +
                "            \"type\": \"NumberAttributeInputCallback\",\n" +
                "            \"output\": [\n" +
                "                {\n" +
                "                    \"name\": \"name\",\n" +
                "                    \"value\": \"age\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"prompt\",\n" +
                "                    \"value\": \"Age\"\n" +
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
                "                        \"name\": \"age\",\n" +
                "                        \"policies\": [\n" +
                "                            {\n" +
                "                                \"policyRequirements\": [\n" +
                "                                    \"VALID_TYPE\"\n" +
                "                                ],\n" +
                "                                \"policyId\": \"valid-type\",\n" +
                "                                \"params\": {\n" +
                "                                    \"types\": [\n" +
                "                                        \"number\"\n" +
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
                "                    \"value\": 123\n" +
                "                }\n" +
                "            ],\n" +
                "            \"input\": [\n" +
                "                {\n" +
                "                    \"name\": \"IDToken1\",\n" +
                "                    \"value\": null\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"IDToken1validateOnly\",\n" +
                "                    \"value\": false\n" +
                "                }\n" +
                "            ]\n" +
                "        }");
        NumberAttributeInputCallback callback = new NumberAttributeInputCallback(raw, 0);
        Assertions.assertThat(callback.getValue()).isEqualTo(123);
    }

    @Test
    public void basicDecimal() throws JSONException {
        JSONObject raw = new JSONObject("{\n" +
                "            \"type\": \"NumberAttributeInputCallback\",\n" +
                "            \"output\": [\n" +
                "                {\n" +
                "                    \"name\": \"name\",\n" +
                "                    \"value\": \"age\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"prompt\",\n" +
                "                    \"value\": \"Age\"\n" +
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
                "                        \"name\": \"age\",\n" +
                "                        \"policies\": [\n" +
                "                            {\n" +
                "                                \"policyRequirements\": [\n" +
                "                                    \"VALID_TYPE\"\n" +
                "                                ],\n" +
                "                                \"policyId\": \"valid-type\",\n" +
                "                                \"params\": {\n" +
                "                                    \"types\": [\n" +
                "                                        \"number\"\n" +
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
                "                    \"value\": 23.2\n" +
                "                }\n" +
                "            ],\n" +
                "            \"input\": [\n" +
                "                {\n" +
                "                    \"name\": \"IDToken1\",\n" +
                "                    \"value\": null\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"IDToken1validateOnly\",\n" +
                "                    \"value\": false\n" +
                "                }\n" +
                "            ]\n" +
                "        }");
        NumberAttributeInputCallback callback = new NumberAttributeInputCallback(raw, 0);
        Assertions.assertThat(callback.getValue()).isEqualTo(23.2);
    }
}