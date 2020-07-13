/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.page;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.forgerock.android.auth.callback.NameCallback;
import org.forgerock.android.auth.callback.PasswordCallback;
import org.forgerock.android.auth.exception.AuthenticationException;
import org.forgerock.android.auth.ui.R;

/**
 * UI representation for Stage UsernamePassword
 */
public class UsernamePasswordPageFragment extends PageFragment {

    private LinearLayout errorLayout;

    @Override
    public void onAuthenticationException(AuthenticationException e) {
        errorLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_username_password_page, container, false);
        errorLayout = view.findViewById(R.id.error);

        TextView header = view.findViewById(R.id.header);
        TextView description = view.findViewById(R.id.description);
        if (!TextUtils.isEmpty(node.getHeader())) {
            header.setText(node.getHeader());
        }
        if (!TextUtils.isEmpty(node.getDescription())) {
            description.setText(node.getDescription());
        }

        view.findViewById(R.id.signIn).setOnClickListener(v -> {
            errorLayout.setVisibility(View.GONE);
            (node.getCallback(NameCallback.class)).setName(((EditText) view.findViewById(R.id.username)).getText().toString());
            (node.getCallback(PasswordCallback.class)).setPassword(((EditText) view.findViewById(R.id.password)).getText().toString().toCharArray());
            onDataCollected();
        });

        return view;
    }
}
