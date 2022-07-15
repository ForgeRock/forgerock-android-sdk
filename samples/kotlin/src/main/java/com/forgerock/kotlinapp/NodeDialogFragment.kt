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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.forgerock.android.auth.Node
import org.forgerock.android.auth.callback.ChoiceCallback
import org.forgerock.android.auth.callback.NameCallback
import org.forgerock.android.auth.callback.PasswordCallback


class NodeDialogFragment: DialogFragment() {
    private var listener: MainActivity? = null
    private var listenerSession: FRSessionActivity? = null
    private var node: Node? = null
    companion object {
        fun newInstance(node: Node?): NodeDialogFragment {
            return NodeDialogFragment().apply {
                arguments = Bundle().apply {
                    this.putSerializable("NODE", node)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.fragment_node, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        node = arguments?.getSerializable("NODE") as Node?
        val choiceNode: View = view.findViewById(R.id.choice_node)
        val choiceCallBack = node?.getCallback(ChoiceCallback::class.java)
        choiceCallBack?.let {
            val yesRadioButton: RadioButton = choiceNode.findViewById(R.id.yes_radio_button)
            val noRadioButton: RadioButton = choiceNode.findViewById(R.id.no_radio_button)
            noRadioButton.text = choiceCallBack.choices[1]
            choiceCallBack.choices.takeIf { it1 -> it1.count() > 1 }.apply {
                yesRadioButton.text = choiceCallBack.choices[0] ?: "Yes"
                noRadioButton.text = choiceCallBack.choices[1] ?: "No"
            }
            val title: AppCompatTextView = choiceNode.findViewById(R.id.title)
            title.text = choiceCallBack.prompt
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
        node?.callbacks?.forEach {
            when (it.type) {
                "NameCallback" -> {
                    (view.findViewById(R.id.usernameLayout) as? TextInputLayout)?.visibility = View.VISIBLE
                }
                "PasswordCallback" -> {
                    (view.findViewById(R.id.passwordLayout) as? TextInputLayout)?.visibility = View.VISIBLE
                }
                "ChoiceCallback" -> {
                    choiceNode.visibility = View.VISIBLE
                }
            }
        }
        val username: TextInputEditText = view.findViewById(R.id.username)
        val password: TextInputEditText = view.findViewById(R.id.password)
        val next: Button = view.findViewById(R.id.next)
        next.setOnClickListener {
            dismiss()
            node?.getCallback(NameCallback::class.java)?.setName(username.text.toString())
            node?.getCallback(PasswordCallback::class.java)?.setPassword(password.text.toString().toCharArray())
            if(listener != null) {
                node?.next(context, listener)
            }
            if(listenerSession != null) {
                node?.next(context, listenerSession)
            }
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
        if (context is FRSessionActivity) {
            listenerSession = context
        }
    }
}
