package org.forgerock.android.auth.devicebind

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import org.forgerock.android.auth.R

/**
 * internal Fragment to display the list of users keys
 */
class DeviceBindFragment(private val userKeyList: List<UserKey>): DialogFragment() {

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_device_bind, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
            val arrayAdapter = ArrayAdapter(view.context, android.R.layout.simple_list_item_1, userKeyList.map { it.userName })
            val keyListView = view.findViewById<ListView>(R.id.key_list)
            val submitButton = view.findViewById<Button>(R.id.submit)
            var selectedView: View? = null
            keyListView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                selectedView = view
                selectedView?.tag = position
                keyListView.children.iterator().forEach { it.setBackgroundColor(Color.WHITE) }
                view.setBackgroundColor(Color.LTGRAY)
                arrayAdapter.notifyDataSetChanged()
            }
            keyListView.adapter = arrayAdapter
            submitButton.setOnClickListener {
                selectedView?.let {
                    getUserKey?.invoke(userKeyList[it.tag as Int])
                }
                this@DeviceBindFragment.dismiss()
            }
    }
}
