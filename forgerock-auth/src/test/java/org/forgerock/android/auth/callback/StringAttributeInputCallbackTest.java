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

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class StringAttributeInputCallbackTest {

    @Test
    public void basicTest() throws JSONException {
        JSONObject raw = new JSONObject("{\n" +
                "      \"type\": \"StringAttributeInputCallback\",\n" +
                "      \"output\": [\n" +
                "        {\n" +
                "          \"name\": \"name\",\n" +
                "          \"value\": \"sn\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"prompt\",\n" +
                "          \"value\": \"Last Name\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"required\",\n" +
                "          \"value\": false\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"policies\",\n" +
                "          \"value\": {\n" +
                "            \"policyRequirements\": [\n" +
                "              \"REQUIRED\",\n" +
                "              \"VALID_TYPE\",\n" +
                "              \"MIN_LENGTH\",\n" +
                "              \"MAX_LENGTH\"\n" +
                "            ],\n" +
                "            \"fallbackPolicies\": null,\n" +
                "            \"name\": \"sn\",\n" +
                "            \"policies\": [\n" +
                "              {\n" +
                "                \"policyRequirements\": [\n" +
                "                  \"REQUIRED\"\n" +
                "                ],\n" +
                "                \"policyId\": \"required\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"policyRequirements\": [\n" +
                "                  \"VALID_TYPE\"\n" +
                "                ],\n" +
                "                \"policyId\": \"valid-type\",\n" +
                "                \"params\": {\n" +
                "                  \"types\": [\n" +
                "                    \"string\"\n" +
                "                  ]\n" +
                "                }\n" +
                "              },\n" +
                "              {\n" +
                "                \"policyId\": \"minimum-length\",\n" +
                "                \"params\": {\n" +
                "                  \"minLength\": 1\n" +
                "                },\n" +
                "                \"policyRequirements\": [\n" +
                "                  \"MIN_LENGTH\"\n" +
                "                ]\n" +
                "              },\n" +
                "              {\n" +
                "                \"policyId\": \"maximum-length\",\n" +
                "                \"params\": {\n" +
                "                  \"maxLength\": 255\n" +
                "                },\n" +
                "                \"policyRequirements\": [\n" +
                "                  \"MAX_LENGTH\"\n" +
                "                ]\n" +
                "              }\n" +
                "            ],\n" +
                "            \"conditionalPolicies\": null\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"failedPolicies\",\n" +
                "          \"value\": []\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"validateOnly\",\n" +
                "          \"value\": true\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"value\",\n" +
                "          \"value\": \"test\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"input\": [\n" +
                "        {\n" +
                "          \"name\": \"IDToken3\",\n" +
                "          \"value\": \"\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"IDToken3validateOnly\",\n" +
                "          \"value\": true\n" +
                "        }\n" +
                "      ]\n" +
                "    }");
        StringAttributeInputCallback callback = new StringAttributeInputCallback(raw, 0);
        Assertions.assertThat(callback.getName()).isEqualTo("sn");
        Assertions.assertThat(callback.getPrompt()).isEqualTo("Last Name");
        Assertions.assertThat(callback.isRequired()).isFalse();
        Assertions.assertThat(callback.getPolicies().getString("name")).isEqualTo("sn");
        Assertions.assertThat(callback.getFailedPolicies()).isEmpty();
        Assertions.assertThat(callback.getValidateOnly()).isTrue();
        Assertions.assertThat(callback.getValue()).isEqualTo("test");
        Assertions.assertThat((boolean) callback.getInputValue(1)).isTrue();
        callback.setValidateOnly(false);
        Assertions.assertThat((boolean) callback.getInputValue(1)).isFalse();


    }
}