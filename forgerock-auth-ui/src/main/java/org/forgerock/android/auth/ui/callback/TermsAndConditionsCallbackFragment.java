/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.callback;


import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.forgerock.android.auth.callback.TermsAndConditionsCallback;
import org.forgerock.android.auth.ui.R;

/**
 * UI representation for {@link TermsAndConditionsCallback}
 */
public class TermsAndConditionsCallbackFragment extends CallbackFragment<TermsAndConditionsCallback> {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_terms_and_conditions_callback, container, false);
        TextView version = view.findViewById(R.id.version);
        version.setText(callback.getVersion());

        TextView createDate = view.findViewById(R.id.createDate);
        createDate.setText(callback.getCreateDate());

        TextView terms = view.findViewById(R.id.terms);
        terms.setText(callback.getTerms());

        CheckBox accept = view.findViewById(R.id.acceptTerms);

        accept.setOnCheckedChangeListener((buttonView, isChecked) -> {
            callback.setAccept(isChecked);
            onDataCollected();
        });
        return view;
    }

}
