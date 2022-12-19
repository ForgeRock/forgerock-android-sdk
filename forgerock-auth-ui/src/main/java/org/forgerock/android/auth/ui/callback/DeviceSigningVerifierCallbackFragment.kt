/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.ui.callback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.forgerock.android.auth.callback.DeviceSigningVerifierCallback
import org.forgerock.android.auth.ui.databinding.FragmentDeviceSigningVerifierCallbackBinding

/**
 * A simple [Fragment] subclass.
 */
class DeviceSigningVerifierCallbackFragment : CallbackFragment<DeviceSigningVerifierCallback>() {
    private lateinit var binding: FragmentDeviceSigningVerifierCallbackBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = FragmentDeviceSigningVerifierCallbackBinding.inflate(layoutInflater, container, false)
        return binding.root
    }


    override fun onStart() {
        super.onStart()
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            try {
                callback?.sign(requireContext())
                binding.message.visibility = View.GONE
                binding.signingProgress.visibility = View.GONE
                next()
            } catch (e: CancellationException) {
                //ignore
            } catch (e: Exception) {
                binding.message.visibility = View.GONE
                binding.signingProgress.visibility = View.GONE
                next()
            }
        }
    }

}