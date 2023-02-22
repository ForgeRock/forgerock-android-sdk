/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.webauthn

import android.app.Activity
import android.app.PendingIntent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.forgerock.android.auth.InitProvider
import org.forgerock.android.auth.exception.WebAuthnException
import org.forgerock.android.auth.exception.WebAuthnResponseException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val PENDING_INTENT = "pendingIntent"

/**
 * Headless Fragment to handle [PendingIntent] from Fido2ApiClient
 */
class WebAuthnFragment : Fragment() {

    private var pendingIntent: PendingIntent? = null
    //Cannot cancel the pending Intent when cancel, the pendingIntent is with com.google.android.gms
    private var continuation: CancellableContinuation<PublicKeyCredential>? = null

    private val signIntentLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult(),
            ::handleSignResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(PENDING_INTENT, PendingIntent::class.java)
            } else {
                it.getParcelable(PENDING_INTENT)
            }
        }
        pendingIntent?.let {
            signIntentLauncher.launch(IntentSenderRequest.Builder(it).build())
        }

    }

    private fun handleSignResult(activityResult: ActivityResult) {
        val bytes = activityResult.data?.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA)
        when {
            activityResult.resultCode != Activity.RESULT_OK -> continuation?.resumeWithException(
                WebAuthnException("error"))
            bytes == null -> continuation?.resumeWithException(WebAuthnException("error"))
            else -> {
                val credential = PublicKeyCredential.deserializeFromBytes(bytes)
                val response = credential.response
                if (response is AuthenticatorErrorResponse) {
                    continuation?.resumeWithException(WebAuthnResponseException(response))
                } else {
                    continuation?.resume(credential)
                }
            }
        }
        parentFragmentManager.beginTransaction().remove(this).commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        continuation?.takeUnless { it.isCompleted }?.cancel()
    }

    companion object {

        private val TAG = WebAuthnFragment::class.java.name

        suspend fun launch(fragmentManager: FragmentManager = InitProvider.getCurrentActivityAsFragmentActivity().supportFragmentManager,
                           pendingIntent: PendingIntent): PublicKeyCredential =
            suspendCancellableCoroutine { continuation ->
                val existing = fragmentManager.findFragmentByTag(TAG) as? WebAuthnFragment
                existing?.apply {
                    this.continuation?.cancel()
                    this.continuation = null
                }?.also {
                    fragmentManager.beginTransaction().remove(it).commitNow()
                }
                val fragment = newInstance(pendingIntent)
                fragment.continuation = continuation
                fragmentManager.beginTransaction().add(fragment, TAG).commit()
            }

        private fun newInstance(intent: PendingIntent) = WebAuthnFragment().apply {
            arguments = Bundle().apply {
                putParcelable(PENDING_INTENT, intent)
            }
        }
    }
}