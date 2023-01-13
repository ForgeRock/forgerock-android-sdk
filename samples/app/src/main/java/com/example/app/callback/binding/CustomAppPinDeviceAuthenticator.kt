/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.callback.binding

import android.content.DialogInterface
import android.os.Bundle
import android.os.OperationCanceledException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.forgerock.android.auth.devicebind.ApplicationPinDeviceAuthenticator
import org.forgerock.android.auth.devicebind.PinCollector
import org.forgerock.android.auth.devicebind.Prompt
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CustomAppPinDeviceAuthenticator : ApplicationPinDeviceAuthenticator(object : PinCollector {
    override suspend fun collectPin(prompt: Prompt, fragmentActivity: FragmentActivity): CharArray {
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val existing =
                    fragmentActivity.supportFragmentManager.findFragmentByTag(TAG) as? CustomPinDialogFragment
                existing?.let {
                    existing.continuation = continuation
                } ?: run {
                    val pinDialogFragment = CustomPinDialogFragment()
                    pinDialogFragment.continuation = continuation
                    pinDialogFragment.show(fragmentActivity.supportFragmentManager, TAG)
                }
            }
        }
    }
})

private val TAG = CustomPinDialogFragment::class.java.simpleName

class CustomPinDialogFragment : DialogFragment() {

    var continuation: CancellableContinuation<CharArray>? = null
        set(value) {
            field = value
            field?.invokeOnCancellation {
                if (it is TimeoutCancellationException) {
                    if (this.isVisible) {
                        activity?.supportFragmentManager?.popBackStack()
                    }
                }
            }
        }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                CollectPin(onSubmit = {
                    continuation?.resume(it.toCharArray())
                    dismiss()
                }, onCancel = {
                    continuation?.resumeWithException(OperationCanceledException())
                    dismiss()
                })
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        continuation?.resumeWithException(OperationCanceledException())
    }

    override fun onDestroy() {
        super.onDestroy()
        //throw exception to end the coroutine scope
        continuation?.takeUnless { it.isCompleted }?.cancel()
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectPin(onSubmit: (String) -> Unit, onCancel: () -> Unit) {

    var pin by rememberSaveable { mutableStateOf("") }
    val currentOnSubmit by rememberUpdatedState(onSubmit)
    var pinVisibility by remember { mutableStateOf(false) }

    var showDialog by remember {
        mutableStateOf(true)
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                onCancel()
                showDialog = false
            },

            confirmButton = {
                TextButton(onClick = {
                    currentOnSubmit(pin)
                    showDialog = false
                })
                { Text(text = "Confirm") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onCancel()
                    showDialog = false
                })
                { Text(text = "Cancel") }
            },
            text = {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value ->
                        pin = value
                    },
                    label = { Text("Application Pin") },
                    trailingIcon = {
                        IconButton(onClick = { pinVisibility = !pinVisibility }) {
                            if (pinVisibility)
                                Icon(Icons.Filled.Visibility, contentDescription = null)
                            else
                                Icon(Icons.Filled.VisibilityOff, contentDescription = null)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    visualTransformation = if (pinVisibility) VisualTransformation.None
                    else PasswordVisualTransformation()
                )
            }
        )
    }
}

