/*
 * Copyright (c) 2019 - 2025 Ping Identity Corporation. All rights reserved.
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

import org.forgerock.android.auth.callback.ValidatedUsernameCallback;
import org.forgerock.android.auth.ui.R;

/**
 * UI representation for {@link ValidatedUsernameCallback}
 */
public class ValidatedUsernameCallbackFragment extends AbstractValidatedCallbackFragment<ValidatedUsernameCallback> {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_name_callback, container, false);
        EditText text = view.findViewById(R.id.username);
        TextInputLayout textInputLayout = view.findViewById(R.id.usernameInputLayout);
        textInputLayout.setHint(callback.getPrompt());
        setError(textInputLayout);
        text.setText((String)callback.getInputValue());

        text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                callback.setUsername(s.toString());
                onDataCollected();
            }
        });
        return view;
    }

}
