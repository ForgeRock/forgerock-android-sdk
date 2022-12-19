/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.os.OperationCanceledException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.TimeoutCancellationException
import org.forgerock.android.auth.databinding.FragmentApplicationPinBinding
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val ARG_PROMPT = "prompt"

/**
 * Default Dialog fragment to request user for Application Pin
 */
class ApplicationPinFragment : DialogFragment() {

    private var prompt: Prompt? = null
    private lateinit var binding: FragmentApplicationPinBinding

    var continuation: CancellableContinuation<CharArray>? = null
        set(value) {
            field = value
            field?.invokeOnCancellation {
                if (it is TimeoutCancellationException) {
                    if (this.isVisible) {
                        dismiss()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            prompt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(ARG_PROMPT, Prompt::class.java)
            } else {
                it.getParcelable(ARG_PROMPT)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentApplicationPinBinding.inflate(layoutInflater, container, false)
        binding.tvTitle.text = prompt?.title
        binding.tvSubTitle.text = prompt?.subtitle
        binding.tvDescription.text = prompt?.description
        val view = binding.root;
        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnPositive.setOnClickListener {
            if (binding.pin.text.toString().isNotEmpty()) {
                continuation?.resume(binding.pin.text.toString().toCharArray())
                dismiss()
            }
        }
        binding.btnNegative.setOnClickListener {
            dismiss()
            continuation?.resumeWithException(OperationCanceledException())
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

    companion object {
        fun newInstance(prompt: Prompt,
                        continuation: CancellableContinuation<CharArray>): ApplicationPinFragment =
            ApplicationPinFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PROMPT, prompt)
                }
                this.continuation = continuation
            }

        const val TAG: String = "ApplicationPinFragment"
    }
}