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
import android.widget.*;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.forgerock.android.auth.callback.KbaCreateCallback;
import org.forgerock.android.auth.ui.R;


/**
 * UI representation for {@link KbaCreateCallback}
 */
public class KbaCreateCallbackFragment extends CallbackFragment<KbaCreateCallback> {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_kba_create_callback, container, false);

        //Setup the questions
        Spinner spinner = view.findViewById(R.id.question);
        spinner.setPrompt(callback.getPrompt());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_dropdown_item, callback.getPredefinedQuestions());

        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                callback.setSelectedQuestion(callback.getPredefinedQuestions().get(position));
                onDataCollected();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        EditText text = view.findViewById(R.id.answer);
        text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                callback.setSelectedAnswer(s.toString());
                onDataCollected();
            }
        });


        return view;
    }

}
