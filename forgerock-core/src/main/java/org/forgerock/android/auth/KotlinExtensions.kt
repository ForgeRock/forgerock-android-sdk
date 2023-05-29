/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

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

fun Long.convertToTime(pattern: String = "yyyyMMdd HH:mm:ss"): String {
    val date = Date(this)
    val simpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())
    return simpleDateFormat.format(date)
}