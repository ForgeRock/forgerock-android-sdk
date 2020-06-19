/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.page;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.forgerock.android.auth.callback.PasswordCallback;
import org.forgerock.android.auth.exception.AuthenticationException;
import org.forgerock.android.auth.ui.R;

/**
 * UI representation for Stage OneTimePassword
 */
public class OneTimePasswordPageFragment extends PageFragment {

    private LinearLayout errorLayout;
    private EditText text;

    @Override
    public void onAuthenticationException(AuthenticationException e) {
        text.setText("");
        errorLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_one_time_password_page, container, false);
        errorLayout = view.findViewById(R.id.error);
        text = view.findViewById(R.id.password);
        final PasswordCallback passwordCallback = node.getCallback(PasswordCallback.class);
        text.setHint(passwordCallback.getPrompt());
        text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                errorLayout.setVisibility(View.GONE);
                if (s.length() == 8) {
                    passwordCallback.setPassword(s.toString().toCharArray());
                    onDataCollected();
                }

            }
        });
        return view;
    }
}
