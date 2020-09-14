/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.auth;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Listener;

/**
 * Reference implementation of handing Advice with {@link DialogFragment}
 */
public class TreeDialogFragment extends DialogFragment {

    private MainActivity listener;

    public static TreeDialogFragment newInstance() {
        return new TreeDialogFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_tree, container, false);
        Button start = view.findViewById(R.id.start);
        start.setOnClickListener(v -> {
            dismiss();
            TextView treeName = view.findViewById(R.id.treeName);
            listener.launchTree(treeName.getText().toString());
        });
        Button launchBrowser = view.findViewById(R.id.launchBrowser);
        launchBrowser.setOnClickListener(v -> {
            dismiss();
            listener.launchBrowser();
        });

        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity)  {
            listener = (MainActivity) context;
        }
    }
}
