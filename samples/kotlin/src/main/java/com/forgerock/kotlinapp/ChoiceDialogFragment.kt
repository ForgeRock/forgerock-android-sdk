/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.forgerock.kotlinapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.RadioButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.DialogFragment
import org.forgerock.android.auth.Node
import org.forgerock.android.auth.callback.ChoiceCallback


class ChoiceDialogFragment: DialogFragment() {
    private var listener: MainActivity? = null
    private var node: Node? = null
    companion object {
        fun newInstance(node: Node?): ChoiceDialogFragment {
            return ChoiceDialogFragment().apply {
                arguments = Bundle().apply {
                    this.putSerializable("NODE", node)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.choice_node, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        node = arguments?.getSerializable("NODE") as Node?
        val choiceCallBack = node?.getCallback(ChoiceCallback::class.java)
        choiceCallBack?.let {
            val yesRadioButton: RadioButton = view.findViewById(R.id.yes_radio_button)
            val title: AppCompatTextView = view.findViewById(R.id.title)
            title.text = node?.getCallback(ChoiceCallback::class.java)?.prompt
            val noRadioButton: RadioButton = view.findViewById(R.id.no_radio_button)
            yesRadioButton.isChecked = it.defaultChoice == 0
            noRadioButton.isChecked = it.defaultChoice == 1
            yesRadioButton.setOnClickListener {
                noRadioButton.isChecked = false
                node?.getCallback(ChoiceCallback::class.java)?.setSelectedIndex(0)
            }
            noRadioButton.setOnClickListener {
                yesRadioButton.isChecked = false
                node?.getCallback(ChoiceCallback::class.java)?.setSelectedIndex(1)
            }
        }
        val next: Button = view.findViewById(R.id.next)
        next.setOnClickListener {
            dismiss()
            node?.next(context, listener)
        }
        val cancel: Button = view.findViewById(R.id.cancel)
        cancel.setOnClickListener { dismiss() }
    }

    override fun onResume() {
        super.onResume()
        val params: ViewGroup.LayoutParams? = dialog?.window?.attributes
        params?.width = ViewGroup.LayoutParams.MATCH_PARENT
        params?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog?.window?.attributes = params as? WindowManager.LayoutParams
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            listener = context
        }
    }
}
