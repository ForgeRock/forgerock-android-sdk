/*
 * Copyright (c) 2023 - 2025 Ping Identity Corporation. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import java.net.URI
import java.net.URISyntaxException
import java.text.SimpleDateFormat
import java.util.*

/* Poor mans ternary operator
* Usage:
* val endValue = (3 < 0) then "true value" ?: "false value"`
* println(endValue)  // prints "false value"
*/
infix fun <T> Boolean.then(value: T): T? {
    return if (this) value else null
}

/**
 * Convert the Long to Time
 * @param pattern The pattern to be used to convert the Long to Time
 */
fun Long.convertToTime(pattern: String = "yyyyMMdd HH:mm:ss"): String {
    val date = Date(this)
    val simpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())
    return simpleDateFormat.format(date)
}

/**
 * Check if the String is an Absolute URL
 */
fun String?.isAbsoluteUrl(): Boolean {
    return try {
        this?.let {
            val uri = URI(it)
            uri.isAbsolute && uri.host != null
        } ?: false
    } catch (e: URISyntaxException) {
        false
    }
}