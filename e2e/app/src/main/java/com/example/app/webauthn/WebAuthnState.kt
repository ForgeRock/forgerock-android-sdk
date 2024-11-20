/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.webauthn

import org.forgerock.android.auth.UserInfo
import org.forgerock.android.auth.webauthn.PublicKeyCredentialSource

data class WebAuthnState(
    var publicKeyCredentialSource: PublicKeyCredentialSource? = null)