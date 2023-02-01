/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.webauthn

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.forgerock.android.auth.webauthn.FRWebAuthn
import org.forgerock.android.auth.webauthn.PublicKeyCredentialSource

class WebAuthnViewModel(context: Context) : ViewModel() {

    private val webAuthnManager = FRWebAuthn(context)

    val sources = MutableStateFlow(emptyList<PublicKeyCredentialSource>())

    fun delete(publicKeyCredentialSource: PublicKeyCredentialSource) {
        webAuthnManager.deleteCredentials(publicKeyCredentialSource)
        fetch(publicKeyCredentialSource.rpid)
    }

    fun delete(rpId: String) {
        webAuthnManager.deleteCredentials(rpId)
        fetch(rpId)
    }

    fun fetch(rpId: String) {
        sources.update { webAuthnManager.loadAllCredentials(rpId) }
    }

    companion object {
        fun factory(
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WebAuthnViewModel(context.applicationContext) as T
            }
        }
    }
}