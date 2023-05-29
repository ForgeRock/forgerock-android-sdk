/*
 * Copyright (c) 2022 - 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.os.OperationCanceledException
import com.google.android.gms.fido.fido2.api.common.Attachment
import com.google.android.gms.fido.fido2.api.common.AttestationConveyancePreference
import com.google.android.gms.fido.fido2.api.common.ErrorCode
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.Node
import org.forgerock.android.auth.exception.WebAuthnResponseException

private val TAG = WebAuthnCallback::class.java.simpleName

/**
 * Interface for WebAuthn Related callback
 */
interface WebAuthnCallback {

    /**
     * Set value to the [HiddenValueCallback] which associated with the WebAuthn
     * Callback.
     *
     * @param node  The Node
     * @param value The Value to set to the [HiddenValueCallback]
     */
    fun setHiddenCallbackValue(node: Node, value: String) {
        for (callback in node.callbacks) {
            if (callback is HiddenValueCallback) {
                if (callback.id == "webAuthnOutcome") {
                    callback.value = value
                }
            }
        }
    }

    /**
     * Set the client error and rethrow the exception
     * @param node The current Node
     * @param e: The Exception
     */
    fun setErrorRethrow(node: Node, e: Exception) {
        Logger.warn(TAG, e, e.message)
        when (e) {
            is WebAuthnResponseException -> {
                if (e.errorCode == ErrorCode.NOT_SUPPORTED_ERR)
                    setHiddenCallbackValue(node, "unsupported");
                else
                    setHiddenCallbackValue(node, e.toServerError());
            }
            is OperationCanceledException -> setHiddenCallbackValue(node,
                "ERROR::NotAllowedError:${e.message}")
            is UnsupportedOperationException,
            is Attachment.UnsupportedAttachmentException,
            is java.lang.IllegalArgumentException,
            is AttestationConveyancePreference.UnsupportedAttestationConveyancePreferenceException ->
                setHiddenCallbackValue(node, "unsupported")

            else -> setHiddenCallbackValue(node, "ERROR::UnknownError:${e.message}")
        }
        throw e
    }

}