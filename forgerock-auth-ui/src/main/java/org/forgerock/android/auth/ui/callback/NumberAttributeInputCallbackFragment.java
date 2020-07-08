/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.callback;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;

import org.forgerock.android.auth.callback.NumberAttributeInputCallback;
import org.forgerock.android.auth.ui.R;

/**
 * UI representation for {@link NumberAttributeInputCallback}
 */
public class NumberAttributeInputCallbackFragment extends AbstractValidatedCallbackFragment<NumberAttributeInputCallback> {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_number_attribute_input_callback, container, false);
        EditText text = view.findViewById(R.id.value);
        TextInputLayout textInputLayout = view.findViewById(R.id.inputLayout);
        textInputLayout.setHint(callback.getPrompt());
        setError(textInputLayout);

        if (callback.getValue() != null) {
            text.setText(callback.getValue().toString());
        }
        text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() == 0) {
                    callback.setValue(0d);
                } else {
                    callback.setValue(Double.parseDouble(s.toString()));
                }
                onDataCollected();
            }
        });
        return view;
    }

}
