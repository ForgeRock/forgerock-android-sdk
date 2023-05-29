/*
 * Copyright (c) 2022 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import android.content.DialogInterface
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.OperationCanceledException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.TimeoutCancellationException
import org.forgerock.android.auth.convertToTime
import org.forgerock.android.auth.databinding.FragmentUserDeviceBindBinding
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * internal Fragment to display the list of users keys
 */

private const val ARG_USER_LIST = "userKeys"

class DeviceBindFragment : DialogFragment() {

    private var userKeyItems: List<UserKey>? = null
    private var _binding: FragmentUserDeviceBindBinding? = null
    private val binding get() = _binding!!

    var continuation: CancellableContinuation<UserKey>? = null
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
        arguments?.let { bundle ->
            userKeyItems = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getParcelable(ARG_USER_LIST, UserKeys::class.java)
            } else {
                bundle.getParcelable(ARG_USER_LIST)
            }?.items?.sortedWith(compareBy<UserKey> { userKey -> userKey.userName }
                .thenBy { it.createdAt } )
        }
    }

    override fun onResume() {
        super.onResume()
        val params: ViewGroup.LayoutParams? = dialog?.window?.attributes
        params?.width = ViewGroup.LayoutParams.MATCH_PARENT
        params?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog?.window?.attributes = params as WindowManager.LayoutParams
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = FragmentUserDeviceBindBinding.inflate(layoutInflater, container, false)
        val view = binding.root;
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val arrayAdapter = userKeyItems?.let { userKeys ->
            ArrayAdapter(view.context, android.R.layout.simple_list_item_1, userKeys.map { "${it.userName}-(${it.authType})-(${it.createdAt.convertToTime()})" })
        }
        var selectedView: View? = null
        binding.keyList.onItemClickListener =
            AdapterView.OnItemClickListener { _, view, position, _ ->
                selectedView = view
                selectedView?.tag = position
                binding.keyList.children.iterator().forEach { it.setBackgroundColor(Color.WHITE) }
                view.setBackgroundColor(Color.LTGRAY)
                arrayAdapter?.notifyDataSetChanged()
            }
        binding.keyList.adapter = arrayAdapter
        binding.submit.setOnClickListener {
            selectedView?.let {
                continuation?.resume(userKeyItems!![it.tag as Int])
                this@DeviceBindFragment.dismiss()
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

    companion object {
        fun newInstance(userKeyList: UserKeys,
                        continuation: CancellableContinuation<UserKey>): DeviceBindFragment =
            DeviceBindFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_USER_LIST, userKeyList)
                }
                this.continuation = continuation
            }

        const val TAG: String = "DeviceBindFragment"
    }
}
