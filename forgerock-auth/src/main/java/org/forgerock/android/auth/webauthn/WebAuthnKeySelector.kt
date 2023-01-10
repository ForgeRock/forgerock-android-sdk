/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.webauthn

/**
 * A Selector for credential key selection.
 */
interface WebAuthnKeySelector {

    /**
     * Select the [PublicKeyCredentialSource] that used for usernameless authentication.
     *
     * @param sourceList      The stored PublicKeyCredentialSource
     * for the selected PublicKeyCredentialSource
     */
    suspend fun select(sourceList: List<PublicKeyCredentialSource>) : PublicKeyCredentialSource? {
        if (sourceList.size == 1) {
            return sourceList[0]
        }
        return WebAuthKeySelectionFragment.launch(sources = sourceList)
    }


    companion object {
        @JvmField
        val DEFAULT: WebAuthnKeySelector = object : WebAuthnKeySelector {}
    }
}