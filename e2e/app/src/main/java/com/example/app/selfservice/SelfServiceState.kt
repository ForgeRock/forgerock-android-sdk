/*
 * Copyright (c) 2023 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.example.app.selfservice

import org.forgerock.android.auth.selfservice.Device

data class SelfServiceState(
    var devices: List<Device> = emptyList(), var throwable: Throwable? = null)