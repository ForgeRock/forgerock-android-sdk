package org.forgerock.android.auth

/* Poor mans ternary operator
* Usage:
* val endValue = (3 < 0) then "true value" ?: "false value"`
* println(endValue)  // prints "false value"
*/
infix fun <T> Boolean.then(value: T): T? {
    return if (this) value else null
}