/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.example.app.storage

import android.content.Context
import org.forgerock.android.auth.AccessToken
import org.forgerock.android.auth.SSOToken
import org.forgerock.android.auth.storage.Storage

class MyCustomTokenStorage(context: Context) : Storage<AccessToken> {

    override fun save(item: AccessToken) {
        TODO("Not yet implemented")
    }

    override fun get(): AccessToken? {
        TODO("Not yet implemented")
    }

    override fun delete() {
        TODO("Not yet implemented")
    }
}

class MyCustomSSOTokenStorage(context: Context) : Storage<SSOToken> {

    override fun save(item: SSOToken) {
        TODO("Not yet implemented")
    }

    override fun get(): SSOToken? {
        TODO("Not yet implemented")
    }

    override fun delete() {
        TODO("Not yet implemented")
    }

}

class MyCustomCookiesStorage() : Storage<Collection<String>> {
    override fun save(item: Collection<String>) {
        TODO("Not yet implemented")
    }

    override fun get(): Collection<String>? {
        TODO("Not yet implemented")
    }

    override fun delete() {
        TODO("Not yet implemented")
    }
}

