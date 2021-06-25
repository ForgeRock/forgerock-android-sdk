/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.callback;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;

import org.forgerock.android.auth.callback.ChoiceCallback;
import org.forgerock.android.auth.callback.NameCallback;
import org.forgerock.android.auth.callback.SelectIdPCallback;
import org.forgerock.android.auth.callback.ValidatedUsernameCallback;
import org.forgerock.android.auth.ui.R;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * UI representation for {@link ChoiceCallback}
 */
public class SelectIdPCallbackFragment extends CallbackFragment<SelectIdPCallback> {

    public static final String LOCAL_AUTHENTICATION = "localAuthentication";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_select_idp_callback, container, false);

        LinearLayout selectAuthenticationLayout = view.findViewById(R.id.selectAuthenticationLayout);
        LinearLayout selectIdpLayout = view.findViewById(R.id.selectIdpLayout);


        for (SelectIdPCallback.IdPValue idp : callback.getProviders()) {
            View v = getView(idp);
            if (v != null) {
                v.setOnClickListener(v1 -> {
                    callback.setValue(idp.getProvider());
                    onDataCollected();
                    next();
                });
                selectIdpLayout.addView(v);
                Space space = new Space(getContext());
                space.setMinimumWidth(20);
                selectIdpLayout.addView(space);
            }
        }
        if (showLocalAuthentication()) {
            suspend();
            View v = getLocalAuthenticationView();
            v.setOnClickListener(v1 -> {
                callback.setValue(LOCAL_AUTHENTICATION);
                onDataCollected();
                next();
            });
            selectAuthenticationLayout.addView(v);
        }
        return view;
    }

    protected View getLocalAuthenticationView() {
        Button localAuthentication = new Button(getContext());
        localAuthentication.setText(R.string.localAuthentication);
        return localAuthentication;
    }

    protected boolean showLocalAuthentication() {
        for (SelectIdPCallback.IdPValue idp : callback.getProviders()) {
            if (idp.getProvider().equals(LOCAL_AUTHENTICATION)) {
                if (node.getCallbacks().size() == 1) {
                    return true;
                } else {
                    return node.getCallback(NameCallback.class) == null &&
                            node.getCallback(ValidatedUsernameCallback.class) == null;
                }
            }
        }
        return false;

    }

    protected View getView(SelectIdPCallback.IdPValue idp) {
        if (idp.getProvider().equals(LOCAL_AUTHENTICATION)) {
            //Special handling of showing local authentication
            callback.setValue(LOCAL_AUTHENTICATION);
            return null;
        }
        if (idp.getProvider().toLowerCase().contains("facebook")) {
            ImageView facebook = new ImageView(getContext());
            facebook.setMaxWidth(100);
            facebook.setMinimumWidth(100);
            facebook.setMaxHeight(100);
            facebook.setMinimumHeight(100);
            facebook.setImageResource(R.drawable.com_facebook_favicon_blue);
            return facebook;
        }

        if (idp.getProvider().toLowerCase().contains("google")) {
            ImageView google = new ImageView(getContext());
            google.setMaxWidth(100);
            google.setMinimumWidth(100);
            google.setMaxHeight(100);
            google.setMinimumHeight(100);
            google.setImageResource(R.drawable.googleg_standard_color_18);
            return google;
        }

        if (idp.getProvider().toLowerCase().contains("apple")) {
            ImageView apple = new ImageView(getContext());
            apple.setMaxWidth(100);
            apple.setMinimumWidth(100);
            apple.setMaxHeight(100);
            apple.setMinimumHeight(100);
            apple.setImageResource(R.drawable.apple_black);
            return apple;
        }

        if (idp.getUiConfig() != null) {
            try {
                JSONObject uiConfig = new JSONObject(idp.getUiConfig());
                Button button = new Button(getContext());
                button.setText(uiConfig.getString("buttonDisplayName"));
                return button;
            } catch (JSONException e) {
                //ignore
            }
        }
        Button button = new Button(getContext());
        button.setText(idp.getProvider());
        return button;

    }

}
