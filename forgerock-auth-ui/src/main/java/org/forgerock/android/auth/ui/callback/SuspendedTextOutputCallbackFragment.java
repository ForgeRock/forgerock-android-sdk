/*
 * Copyright (c) 2020 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.callback;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.forgerock.android.auth.callback.TextOutputCallback;
import org.forgerock.android.auth.ui.R;

/**
 * UI representation for {@link org.forgerock.android.auth.callback.SuspendedTextOutputCallback}
 */
public class SuspendedTextOutputCallbackFragment extends CallbackFragment<TextOutputCallback> {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_text_output_callback, container, false);
        TextView message = view.findViewById(R.id.message);
        message.setText(callback.getMessage());

        suspend();

        return view;
    }

}
