/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import org.forgerock.android.auth.databinding.FragmentUserDeviceBindBinding

/**
 * internal Fragment to display the list of users keys
 */
class DeviceBindFragment(private val userKeyList: List<UserKey>) : DialogFragment() {

    private var _binding: FragmentUserDeviceBindBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG: String = "DeviceBindFragment"
    }

    var getUserKey: ((UserKey) -> (Unit))? = null

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
        val arrayAdapter = ArrayAdapter(view.context,
            android.R.layout.simple_list_item_1,
            userKeyList.map { it.userName })
        var selectedView: View? = null
        binding.keyList.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                selectedView = view
                selectedView?.tag = position
                binding.keyList.children.iterator().forEach { it.setBackgroundColor(Color.WHITE) }
                view.setBackgroundColor(Color.LTGRAY)
                arrayAdapter.notifyDataSetChanged()
            }
        binding.keyList.adapter = arrayAdapter
        binding.submit.setOnClickListener {
            selectedView?.let {
                getUserKey?.invoke(userKeyList[it.tag as Int])
            }
            this@DeviceBindFragment.dismiss()
        }
    }
}
