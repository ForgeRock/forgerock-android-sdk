/*
 * Copyright (c) 2022 - 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.webauthn

import android.os.Build
import android.os.Bundle
import android.os.OperationCanceledException
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.parcelize.Parcelize
import org.forgerock.android.auth.InitProvider
import org.forgerock.android.auth.R
import org.forgerock.android.auth.convertToTime
import org.forgerock.android.auth.databinding.FragmentUserSelectBinding
import org.forgerock.android.auth.databinding.FragmentUserSelectBinding.inflate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * A Simple Dialog to display key selection using a spinner
 */
private const val PUBLIC_KEY_CREDENTIAL_SOURCE = "PUBLIC_KEY_CREDENTIAL_SOURCE"

class WebAuthKeySelectionFragment : DialogFragment() {
    private lateinit var sources: List<PublicKeyCredentialSource>
    private lateinit var binding: FragmentUserSelectBinding

    var continuation: CancellableContinuation<PublicKeyCredentialSource?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
        arguments?.let {
            sources = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(PUBLIC_KEY_CREDENTIAL_SOURCE,
                    PublicKeyCredentialSources::class.java)?.items ?: emptyList()
            } else {
                it.getParcelable<PublicKeyCredentialSources>(PUBLIC_KEY_CREDENTIAL_SOURCE)?.items ?: emptyList()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val params: ViewGroup.LayoutParams? = dialog?.window?.attributes
        params?.width = ViewGroup.LayoutParams.MATCH_PARENT
        params?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog?.window?.attributes = params as WindowManager.LayoutParams
    }

    override fun onDestroy() {
        super.onDestroy()
        //throw exception to end the coroutine scope
        continuation?.takeUnless { it.isCompleted }?.cancel()
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = inflate(layoutInflater, container, false)
        val user = binding.userSpinner
        val adapter: ArrayAdapter<PublicKeyCredentialSource> =
            object : ArrayAdapter<PublicKeyCredentialSource>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, sources) {

                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.spinner_item, parent, false)
                    val names = view?.findViewById<TextView>(R.id.textView)
                    val publicKeyCredentialSource = this@WebAuthKeySelectionFragment.sources[position]
                    val displayKey = "${publicKeyCredentialSource.otherUI} (${publicKeyCredentialSource.created.convertToTime()})"
                    names?.text = displayKey
                    return view
                }

                override fun getDropDownView(position: Int,
                                             convertView: View?,
                                             parent: ViewGroup): View {
                    return getView(position, convertView, parent)
                }
            }
        user.adapter = adapter
        var selected = -1
        user.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?,
                                        view: View?,
                                        position: Int,
                                        id: Long) {
                selected = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selected = -1
            }
        }
        binding.btnPositive.setOnClickListener {
            if (selected < 0)
                continuation?.resume(null)
            else
                continuation?.resume(sources[selected])
            dismiss()
        }
        binding.btnNegative.setOnClickListener {
            continuation?.resumeWithException(OperationCanceledException("No Key Selected"))
            dismiss()
        }
        return binding.root
    }

    @Parcelize
    data class PublicKeyCredentialSources(
        val items: List<PublicKeyCredentialSource>
    ) : Parcelable

    companion object {

        private val TAG = WebAuthKeySelectionFragment::class.java.name

        suspend fun launch(fragmentManager: FragmentManager = InitProvider.getCurrentActivityAsFragmentActivity().supportFragmentManager,
                           sources: List<PublicKeyCredentialSource>): PublicKeyCredentialSource? =
            suspendCancellableCoroutine { continuation ->
                val existing =
                    fragmentManager.findFragmentByTag(TAG) as? WebAuthKeySelectionFragment
                existing?.apply {
                    this.continuation?.cancel()
                    this.continuation = null
                }?.also {
                    fragmentManager.beginTransaction().remove(it).commitNow()
                }
                val fragment = newInstance(PublicKeyCredentialSources(sources))
                fragment.continuation = continuation
                fragmentManager.beginTransaction().add(fragment, TAG).commit()
            }

        private fun newInstance(sources: PublicKeyCredentialSources) =
            WebAuthKeySelectionFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(PUBLIC_KEY_CREDENTIAL_SOURCE, sources)
                }
            }
    }
}