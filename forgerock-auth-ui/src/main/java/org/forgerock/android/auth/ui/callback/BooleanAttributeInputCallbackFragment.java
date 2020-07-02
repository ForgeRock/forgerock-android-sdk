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
import android.widget.Switch;

import org.forgerock.android.auth.callback.BooleanAttributeInputCallback;
import org.forgerock.android.auth.ui.R;

/**
 * UI representation for {@link BooleanAttributeInputCallback}
 */
public class BooleanAttributeInputCallbackFragment extends AbstractValidatedCallbackFragment<BooleanAttributeInputCallback> {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_boolean_attribute_input_callback, container, false);
        Switch aSwitch = view.findViewById(R.id.booleanAttribute);

        aSwitch.setChecked(callback.getValue());
        aSwitch.setText(callback.getPrompt());

        aSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            callback.setValue(isChecked);
            onDataCollected();
        });

        return view;
    }

}
