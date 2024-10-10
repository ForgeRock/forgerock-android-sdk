/*
 * Copyright (c) 2019 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import android.content.SharedPreferences

/**
 * Manage SSO Token with [SharedPreferences] as the storage.
 */
internal class SharedPreferencesSignOnManager(context: Context,
                                              sharedPreferences: SharedPreferences? = null) :
    SingleSignOnManager {
    //The SharedPreferences to store the token
    private val sharedPreferences: SharedPreferences = sharedPreferences
        ?: SecuredSharedPreferences(context,
            ORG_FORGEROCK_V_1_SSO_TOKENS,
            ORG_FORGEROCK_V_1_KEYS)

    override fun persist(token: SSOToken) {
        sharedPreferences.edit()
            .putString(SSO_TOKEN, token.value)
            .commit()
    }

    override fun persist(cookies: Collection<String>) {
        if (cookies.isEmpty()) {
            sharedPreferences.edit().remove(COOKIES).commit()
        } else {
            val set: Set<String> = HashSet(cookies)
            sharedPreferences.edit()
                .putStringSet(COOKIES, set)
                .commit()
        }
    }

    override fun clear() {
        sharedPreferences.edit().clear().commit()
    }

    fun clearCookies() {
        sharedPreferences.edit().remove(COOKIES).commit()
    }

    fun clearToken() {
        sharedPreferences.edit().remove(SSO_TOKEN).commit()
    }

    override fun getToken(): SSOToken? {
        val token = sharedPreferences.getString(SSO_TOKEN, null)
        if (token != null) {
            return SSOToken(token)
        }
        return null
    }

    override fun getCookies(): Collection<String> {
        return sharedPreferences.getStringSet(COOKIES, emptySet()) ?: emptySet()
    }

    override fun hasToken(): Boolean {
        return sharedPreferences.getString(SSO_TOKEN, null) != null
    }

    override fun revoke(listener: FRListener<Void>?) {
        clear()
    }

    companion object {
        //Alias to store keys
        private const val ORG_FORGEROCK_V_1_KEYS = "org.forgerock.v1.KEYS"

        //File name to store tokens
        private const val ORG_FORGEROCK_V_1_SSO_TOKENS = "org.forgerock.v1.SSO_TOKENS"

        private const val SSO_TOKEN = "org.forgerock.v1.SSO_TOKEN"
        private const val COOKIES = "org.forgerock.v1.COOKIES"
    }
}
