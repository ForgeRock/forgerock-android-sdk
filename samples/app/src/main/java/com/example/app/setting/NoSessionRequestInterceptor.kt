/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package com.example.app.setting

import android.net.Uri
import org.forgerock.android.auth.Action
import org.forgerock.android.auth.FRRequestInterceptor
import org.forgerock.android.auth.Request

/**
 * Sample [RequestInterceptor] to add noSession
 */
class NoSessionRequestInterceptor : FRRequestInterceptor<Action> {
    override fun intercept(request: Request, tag: Action): Request {
        return if (tag.type == Action.AUTHENTICATE) {
            request.newBuilder()
                .url(Uri.parse(request.url().toString())
                    .buildUpon()
                    .appendQueryParameter("noSession", "true").toString())
                .build()
        } else request
    }
}
