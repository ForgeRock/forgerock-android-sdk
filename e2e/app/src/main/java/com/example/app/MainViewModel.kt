/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MainViewModel : ViewModel() {

    var isLoading = MutableStateFlow(true)
        private set

    init {
        viewModelScope.launch {
            delay(2.toDuration(DurationUnit.SECONDS))
            isLoading.value = false

        }
    }
}