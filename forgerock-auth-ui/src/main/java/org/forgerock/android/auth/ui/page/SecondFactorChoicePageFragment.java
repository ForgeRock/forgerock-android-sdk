/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.page;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.callback.ChoiceCallback;
import org.forgerock.android.auth.exception.AuthenticationException;
import org.forgerock.android.auth.ui.R;
import org.forgerock.android.auth.ui.page.PageFragment;

import static android.view.View.OnClickListener;

/**
 * UI representation for Stage SecondFactorChoice
 */
public class SecondFactorChoicePageFragment extends PageFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_second_factor_choice_page, container, false);
        LinearLayout linearLayout = view.findViewById(R.id.choiceLayout);

        final ChoiceCallback choiceCallback = node.getCallback(ChoiceCallback.class);

        for (int i = 0; i < choiceCallback.getChoices().size(); i++) {
            Button button = new Button(getContext());
            button.setText(choiceCallback.getChoices().get(i));
            final int finalI = i;
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    choiceCallback.setSelectedIndex(finalI);
                    onDataCollected();
                }
            });
            linearLayout.addView(button, i);
        }
        return view;

    }

}
