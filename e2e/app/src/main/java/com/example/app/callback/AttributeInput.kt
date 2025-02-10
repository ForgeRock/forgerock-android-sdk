/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.example.app.callback

import org.forgerock.android.auth.callback.AttributeInputCallback

val VALIDATION_ERROR = mutableMapOf(
    "VALID_USERNAME" to "Invalid username",
    "UNIQUE" to "Must be unique",
    "MIN_LENGTH" to "{prompt} Must be at least {minLength} characters long",
    "MAX_LENGTH" to "{prompt} Must be less than {maxLength} characters long",
    "AT_LEAST_X_CAPITAL_LETTERS" to "{prompt} Must have at least {numCaps} capital letter(s)",
    "AT_LEAST_X_NUMBERS" to "{prompt} Must have at least {numNums} number(s)",
    "VALID_EMAIL_ADDRESS_FORMAT" to "Invalid email format (example@example.com)",
    "CANNOT_CONTAIN_CHARACTERS" to "{prompt} Cannot contain characters: {forbiddenChars}",
    "CANNOT_CONTAIN_DUPLICATES" to "Cannot contain duplicates: {duplicateValue}",
    "REQUIRED" to "Cannot be blank",
    "MATCH_REGEXP" to "Has to match pattern: {regexp}",
    "VALID_TYPE" to "Must be one of the following types: {validTypes}. Cannot be {invalidType}",
    "VALID_NUMBER" to "{prompt} Invalid number",
    "MINIMUM_NUMBER_VALUE" to "{prompt} Less than minimum number value",
    "MAXIMUM_NUMBER_VALUE" to "{prompt} Greater than maximum number value",
    "VALID_NAME_FORMAT" to "Must have valid name characters",
    "VALID_PHONE_FORMAT" to "Must be a valid phone number",
    "CANNOT_CONTAIN_DUPLICATES" to "{prompt} must not contain duplicates {duplicateValue}",
    "CANNOT_CONTAIN_OTHERS" to "{prompt} Must not share characters with {disallowedFields}",
    "VALID_DATE" to "Must be a valid date",
    "IS_NEW" to "Must not be the same as the previous {historyLength} password(s)",
    "IS_NUMBER" to "Must be a number greater than or equal to zero.",
    "IS_NUMBER_GREATER_ZERO" to "Must be a number greater than or equal to 1.",
    "MIN_ITEMS" to "Must have at least {minItems} value(s)",
    "MUST_CONTAIN_LOWERCASE_CHARACTERS" to "Must have at least {minItems} lowercase letter(s)",
    "MUST_CONTAIN_SPECIAL_CHARACTERS" to "Must have at least {minItems} of these characters: {requiredChars}",
    "VALID_BOOLEAN" to "Value must be of type boolean.",
    "VALID_INT" to "Value must be of type integer.",
)


fun AttributeInputCallback.error(): String {
    return failedPolicies.joinToString("\n") { policy ->
        VALIDATION_ERROR[policy.policyRequirement]?.let { error ->
            policy.format(prompt, error)
        } ?: ""
    }
}

fun AttributeInputCallback.hasError(): Boolean {
    return failedPolicies.isNotEmpty()
}