/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth

import kotlinx.serialization.json.Json

internal val json: Json =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
