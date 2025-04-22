/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth

import org.forgerock.android.auth.storage.COOKIES
import org.forgerock.android.auth.storage.CookiesStorage
import org.forgerock.android.auth.storage.ORG_FORGEROCK_V_1_KEYS
import org.forgerock.android.auth.storage.ORG_FORGEROCK_V_1_TOKENS
import org.forgerock.android.auth.storage.ORG_FORGEROCK_V_2_COOKIES
import org.forgerock.android.auth.storage.ORG_FORGEROCK_V_2_KEYS
import org.forgerock.android.auth.storage.ORG_FORGEROCK_V_2_SSO_TOKENS
import org.forgerock.android.auth.storage.SSOTokenStorage
import org.forgerock.android.auth.storage.SSO_TOKEN
import org.forgerock.android.auth.storage.Storage
import org.forgerock.android.auth.storage.TokenStorage
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal object Options {

    /**
     * Initialize the Options with the provided FROptions.
     *
     * @param options The FROptions to initialize with.
     */
    fun init(options: FROptions) {
        init(options.store)
    }

    /**
     * Initialize the Options with the provided Store.
     *
     * @param store The Store to initialize with.
     */
    fun init(store: Store) {
        oidcStorage = store.oidcStorage
        ssoTokenStorage = store.ssoTokenStorage
        cookieStorage = store.cookiesStorage
    }

    /**
     * Storage for OIDC tokens.
     */
    var oidcStorage: Storage<AccessToken> by LazyAssignable {
        TokenStorage(ContextProvider.context,
            ORG_FORGEROCK_V_1_TOKENS,
            ORG_FORGEROCK_V_1_KEYS,
            OAuth2.ACCESS_TOKEN)
    }
        private set

    /**
     * Storage for SSO tokens.
     */
    var ssoTokenStorage: Storage<SSOToken> by LazyAssignable {
        SSOTokenStorage(ContextProvider.context,
            ORG_FORGEROCK_V_2_SSO_TOKENS,
            ORG_FORGEROCK_V_2_KEYS,
            SSO_TOKEN)
    }
        private set

    /**
     * Storage for cookies.
     */
    var cookieStorage: Storage<Collection<String>> by LazyAssignable {
        CookiesStorage(ContextProvider.context,
            ORG_FORGEROCK_V_2_COOKIES,
            ORG_FORGEROCK_V_2_KEYS,
            COOKIES)
    }
        private set
}

/**
 * A delegate class for lazy initialization and assignment.
 *
 * @param T The type of object to be stored.
 * @param initializer A function to initialize the value.
 */
class LazyAssignable<T>(private val initializer: () -> T) : ReadWriteProperty<Any?, T> {
    @Volatile
    private var value: T? = null
    private val lock = Any()

    /**
     * Get the value, initializing it if necessary.
     *
     * @param thisRef The reference to the property owner.
     * @param property The property being accessed.
     * @return The value of the property.
     */
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        value?.let { return it }

        @Suppress("UNCHECKED_CAST")
        synchronized(lock) {
            value?.let { return it }
            value = initializer()
            return value as T
        }
    }

    /**
     * Set the value.
     *
     * @param thisRef The reference to the property owner.
     * @param property The property being accessed.
     * @param value The new value to set.
     */
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        synchronized(lock) {
            this.value = value
        }
    }
}