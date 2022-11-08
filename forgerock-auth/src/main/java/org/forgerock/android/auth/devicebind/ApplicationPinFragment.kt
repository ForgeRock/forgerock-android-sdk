/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import org.forgerock.android.auth.databinding.FragmentApplicationPinBinding

/*
Default Dialog fragment to request user for Application Pin
 */
class ApplicationPinFragment : DialogFragment() {

    private var _binding: FragmentApplicationPinBinding? = null
    private val binding get() = _binding!!

    var onPinReceived: ((String) -> Unit?)? = null
    var onCancelled: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = FragmentApplicationPinBinding.inflate(layoutInflater, container, false)
        val view = binding.root;
        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnPositive.setOnClickListener {
            onPinReceived?.let { it(binding.pin.text.toString()) }
            dismiss()
        }
        binding.btnNegative.setOnClickListener {
            onCancelled?.let { it() }
            dismiss()
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onCancelled?.let { it() }
    }

    companion object {
        fun newInstance(): ApplicationPinFragment {
            return ApplicationPinFragment()
        }

        const val TAG: String = "ApplicationPinFragment"
    }
}