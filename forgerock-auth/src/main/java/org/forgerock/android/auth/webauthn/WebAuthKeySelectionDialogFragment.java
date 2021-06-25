/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.webauthn;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.OperationCanceledException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Listener;
import org.forgerock.android.auth.R;

import java.util.ArrayList;
import java.util.List;

/**
 * A Simple Dialog to display key selection using a spinner
 */
public class WebAuthKeySelectionDialogFragment extends DialogFragment {

    private static final String PUBLIC_KEY_CREDENTIAL_SOURCE = "PUBLIC_KEY_CREDENTIAL_SOURCE";
    private boolean isCancel = true;
    private List<PublicKeyCredentialSource> sources;
    private FRListener<PublicKeyCredentialSource> listener;

    /**
     * Default Constructor to construct WebAuthKeySelectionDialogFragment
     */
    public WebAuthKeySelectionDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Create a new instance of {@link WebAuthKeySelectionDialogFragment}
     *
     * @param sources  The list of stored {@link PublicKeyCredentialSource}
     * @param listener Listener to listen for event after  {@link PublicKeyCredentialSource} is selected.
     * @return A instance of {@link WebAuthKeySelectionDialogFragment}
     */
    public static WebAuthKeySelectionDialogFragment newInstance(List<PublicKeyCredentialSource> sources, FRListener<PublicKeyCredentialSource> listener) {
        WebAuthKeySelectionDialogFragment fragment = new WebAuthKeySelectionDialogFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(PUBLIC_KEY_CREDENTIAL_SOURCE, new ArrayList<>(sources));
        fragment.setArguments(args);
        fragment.listener = listener;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            sources = getArguments().getParcelableArrayList(PUBLIC_KEY_CREDENTIAL_SOURCE);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (isCancel) {
            Listener.onException(listener, new OperationCanceledException());
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_user_select, container, false);
        Spinner user = view.findViewById(R.id.user);

        ArrayAdapter<PublicKeyCredentialSource> adapter = new ArrayAdapter(
                getContext(), android.R.layout.simple_spinner_dropdown_item, sources) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                View view = inflater.inflate(R.layout.spinner_item, null);
                TextView names = view.findViewById(R.id.textView);
                names.setText(sources.get(position).getOtherUI());
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                return getView(position, convertView, parent);
            }
        };

        user.setAdapter(adapter);
        final int[] selected = {0};
        user.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selected[0] = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Button submit = view.findViewById(R.id.submit);
        submit.setOnClickListener(v -> {
            Listener.onSuccess(listener, sources.get(selected[0]));
            isCancel = false;
            dismiss();
        });
        return view;
    }

}
