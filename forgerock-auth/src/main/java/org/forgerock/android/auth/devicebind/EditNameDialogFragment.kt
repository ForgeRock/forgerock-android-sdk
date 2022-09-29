package org.forgerock.android.auth.devicebind


import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import org.forgerock.android.auth.R


class EditNameDialogFragment : DialogFragment() {

    private var mEditText: EditText? = null

    var getText: ((String) -> Unit?)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.custom_dialog, container)
    }

    override fun onResume() {
        super.onResume()
        val params: ViewGroup.LayoutParams? = dialog?.window?.attributes
        params?.width = ViewGroup.LayoutParams.MATCH_PARENT
        params?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog?.window?.attributes = params as WindowManager.LayoutParams
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(activity)
        builder.setTitle("Enter Application PIN")

// Set up the input
        val input = EditText(activity)
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

// Set up the buttons
        builder.setPositiveButton("OK") { dialog, which ->
            // Here you get get input text from the Edittext
            var m_Text = input.text.toString()

            getText?.let { it(m_Text) }
        }
        builder.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })

       return builder.create()
    }


    companion object {
        fun newInstance(title: String?): EditNameDialogFragment {
            val frag = EditNameDialogFragment()
            val args = Bundle()
            args.putString("title", title)
            frag.arguments = args
            return frag
        }

        const val TAG: String = "EditNameDialogFragment"
    }
}