/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NumberAttributeInputCallbackTest {
    @Test
    @Throws(JSONException::class)
    fun basicTest() {
        val raw = JSONObject("""{
            "type": "NumberAttributeInputCallback",
            "output": [
                {
                    "name": "name",
                    "value": "age"
                },
                {
                    "name": "prompt",
                    "value": "Age"
                },
                {
                    "name": "required",
                    "value": true
                },
                {
                    "name": "policies",
                    "value": {
                        "policyRequirements": [
                            "VALID_TYPE"
                        ],
                        "fallbackPolicies": null,
                        "name": "age",
                        "policies": [
                            {
                                "policyRequirements": [
                                    "VALID_TYPE"
                                ],
                                "policyId": "valid-type",
                                "params": {
                                    "types": [
                                        "number"
                                    ]
                                }
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
                    "value": false
                },
                {
                    "name": "value",
                    "value": null
                }
            ],
            "input": [
                {
                    "name": "IDToken1",
                    "value": null
                },
                {
                    "name": "IDToken1validateOnly",
                    "value": false
                }
            ]
        }""")
        val callback = NumberAttributeInputCallback(raw, 0)
        assertThat(callback.name).isEqualTo("age")
        assertThat(callback.prompt).isEqualTo("Age")
        assertThat(callback.isRequired).isTrue
        assertThat(callback.policies.getString("name")).isEqualTo("age")
        assertThat(callback.failedPolicies).isEmpty()
        assertThat(callback.validateOnly).isFalse
        @Suppress("USELESS_CAST")
        assertThat(callback.value as Double?).isNull()
    }

    @Test
    @Throws(JSONException::class)
    fun basicInteger() {
        val raw = JSONObject("""{
            "type": "NumberAttributeInputCallback",
            "output": [
                {
                    "name": "name",
                    "value": "age"
                },
                {
                    "name": "prompt",
                    "value": "Age"
                },
                {
                    "name": "required",
                    "value": true
                },
                {
                    "name": "policies",
                    "value": {
                        "policyRequirements": [
                            "VALID_TYPE"
                        ],
                        "fallbackPolicies": null,
                        "name": "age",
                        "policies": [
                            {
                                "policyRequirements": [
                                    "VALID_TYPE"
                                ],
                                "policyId": "valid-type",
                                "params": {
                                    "types": [
                                        "number"
                                    ]
                                }
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
                    "value": false
                },
                {
                    "name": "value",
                    "value": 123
                }
            ],
            "input": [
                {
                    "name": "IDToken1",
                    "value": null
                },
                {
                    "name": "IDToken1validateOnly",
                    "value": false
                }
            ]
        }""")
        val callback = NumberAttributeInputCallback(raw, 0)
        assertThat(callback.value).isEqualTo(123.0)
    }

    @Test
    @Throws(JSONException::class)
    fun basicDecimal() {
        val raw = JSONObject("""{
            "type": "NumberAttributeInputCallback",
            "output": [
                {
                    "name": "name",
                    "value": "age"
                },
                {
                    "name": "prompt",
                    "value": "Age"
                },
                {
                    "name": "required",
                    "value": true
                },
                {
                    "name": "policies",
                    "value": {
                        "policyRequirements": [
                            "VALID_TYPE"
                        ],
                        "fallbackPolicies": null,
                        "name": "age",
                        "policies": [
                            {
                                "policyRequirements": [
                                    "VALID_TYPE"
                                ],
                                "policyId": "valid-type",
                                "params": {
                                    "types": [
                                        "number"
                                    ]
                                }
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
                    "value": false
                },
                {
                    "name": "value",
                    "value": 23.2
                }
            ],
            "input": [
                {
                    "name": "IDToken1",
                    "value": null
                },
                {
                    "name": "IDToken1validateOnly",
                    "value": false
                }
            ]
        }""")
        val callback = NumberAttributeInputCallback(raw, 0)
        assertThat(callback.value).isEqualTo(23.2)
    }
}