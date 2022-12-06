/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import org.forgerock.android.auth.CryptoKey


/**
 * Interface to be implemented by objects that want to be aware of [CryptoKey]
 */
internal interface CryptoAware {

    fun setKey(cryptoKey: CryptoKey)

}