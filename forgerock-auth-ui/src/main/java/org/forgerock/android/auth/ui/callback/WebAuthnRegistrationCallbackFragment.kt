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
import com.google.android.gms.fido.fido2.api.common.ResidentKeyRequirement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.forgerock.android.auth.callback.WebAuthnRegistrationCallback
import org.forgerock.android.auth.ui.databinding.FragmentWebauthnRegistrationCallbackBinding

/**
 * UI representation for [WebAuthnRegistrationCallback]
 */
open class WebAuthnRegistrationCallbackFragment : CallbackFragment<WebAuthnRegistrationCallback>() {

    private lateinit var binding: FragmentWebauthnRegistrationCallbackBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment

        binding =
            FragmentWebauthnRegistrationCallbackBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            try {
                callback.setResidentKeyRequirement(ResidentKeyRequirement.RESIDENT_KEY_DISCOURAGED)
                callback.register(requireContext(), node = node)
                next()
            } catch (e: CancellationException) {
                //ignore
            } catch (e: Exception) {
                //ignore
                next()
            }
            binding.progress.visibility = View.GONE
        }
    }

}