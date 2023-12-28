/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.setting

sealed class SettingTransitionState {
    object Enabled : SettingTransitionState()
    object Disabled : SettingTransitionState()
    object EnableBinding: SettingTransitionState()

}