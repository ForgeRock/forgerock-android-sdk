/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import org.forgerock.android.auth.callback.IdPCallback
import org.forgerock.android.auth.idp.IdPHandler
import org.json.JSONObject
import java.lang.IllegalArgumentException
import java.util.*

class IdPCallbackMock : IdPCallback {

    constructor() {}
    constructor(jsonObject: JSONObject?, index: Int) : super(jsonObject, index) {}

    override fun getIdPHandler(): IdPHandler {
        if (provider.lowercase(Locale.getDefault()).contains("apple")) {
            return AppleSignInHandlerMock()
        }
        throw IllegalArgumentException()
    }
}