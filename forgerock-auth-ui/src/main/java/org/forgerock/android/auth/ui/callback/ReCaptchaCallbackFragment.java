/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
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
import android.widget.LinearLayout;
import android.widget.TextView;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.callback.ReCaptchaCallback;
import org.forgerock.android.auth.ui.R;

import static android.view.View.INVISIBLE;

/**
 * UI representation for {@link ReCaptchaCallback}
 */
public class ReCaptchaCallbackFragment extends CallbackFragment<ReCaptchaCallback> {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_recaptcha_callback, container, false);
        Button retry = view.findViewById(R.id.reCaptcha);
        LinearLayout error = view.findViewById(R.id.error);

        FRListener<Void> listener = new FRListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                next();
            }

            @Override
            public void onException(Exception e) {
                retry.setVisibility(View.VISIBLE);
                error.setVisibility(View.VISIBLE);
                TextView error = view.findViewById(R.id.verificationError);
                error.setText(e.getMessage());
            }
        };

        callback.proceed(getContext(), listener);

        retry.setOnClickListener(v -> {
            retry.setVisibility(INVISIBLE);
            error.setVisibility(INVISIBLE);

            callback.proceed(getContext(), listener);
        });

        return view;
    }

}
