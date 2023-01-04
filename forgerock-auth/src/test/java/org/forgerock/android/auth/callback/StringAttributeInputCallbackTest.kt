/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StringAttributeInputCallbackTest {
    @Test
    @Throws(JSONException::class)
    fun basicTest() {
        val raw = JSONObject("""{
      "type": "StringAttributeInputCallback",
      "output": [
        {
          "name": "name",
          "value": "sn"
        },
        {
          "name": "prompt",
          "value": "Last Name"
        },
        {
          "name": "required",
          "value": false
        },
        {
          "name": "policies",
          "value": {
            "policyRequirements": [
              "REQUIRED",
              "VALID_TYPE",
              "MIN_LENGTH",
              "MAX_LENGTH"
            ],
            "fallbackPolicies": null,
            "name": "sn",
            "policies": [
              {
                "policyRequirements": [
                  "REQUIRED"
                ],
                "policyId": "required"
              },
              {
                "policyRequirements": [
                  "VALID_TYPE"
                ],
                "policyId": "valid-type",
                "params": {
                  "types": [
                    "string"
                  ]
                }
              },
              {
                "policyId": "minimum-length",
                "params": {
                  "minLength": 1
                },
                "policyRequirements": [
                  "MIN_LENGTH"
                ]
              },
              {
                "policyId": "maximum-length",
                "params": {
                  "maxLength": 255
                },
                "policyRequirements": [
                  "MAX_LENGTH"
                ]
              }
            ],
            "conditionalPolicies": null
          }
        },
        {
          "name": "failedPolicies",
          "value": []
        },
        {
          "name": "validateOnly",
          "value": true
        },
        {
          "name": "value",
          "value": "test"
        }
      ],
      "input": [
        {
          "name": "IDToken3",
          "value": ""
        },
        {
          "name": "IDToken3validateOnly",
          "value": true
        }
      ]
    }""")
        val callback = StringAttributeInputCallback(raw, 0)
        Assertions.assertThat(callback.name).isEqualTo("sn")
        Assertions.assertThat(callback.prompt).isEqualTo("Last Name")
        Assertions.assertThat(callback.isRequired).isFalse
        Assertions.assertThat(callback.policies.getString("name")).isEqualTo("sn")
        Assertions.assertThat(callback.failedPolicies).isEmpty()
        Assertions.assertThat(callback.validateOnly).isTrue
        Assertions.assertThat(callback.value).isEqualTo("test")
        Assertions.assertThat(callback.getInputValue(1) as Boolean).isTrue
        callback.validateOnly = false
        Assertions.assertThat(callback.getInputValue(1) as Boolean).isFalse
    }
}