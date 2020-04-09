/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
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

import org.forgerock.android.auth.callback.ConfirmationCallback;
import org.forgerock.android.auth.ui.R;

/**
 * UI representation for {@link ConfirmationCallback}
 */
public class ConfirmationCallbackFragment extends CallbackFragment<ConfirmationCallback> {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_confirmation_callback, container, false);
        TextView prompt = view.findViewById(R.id.prompt);
        prompt.setText(callback.getPrompt());
        LinearLayout confirmation = view.findViewById(R.id.confirmation);
        for (int i = 0; i < callback.getOptions().size(); i++) {
            Button button = new Button(getContext());
            button.setText(callback.getOptions().get(i));
            final int finalI = i;
            button.setOnClickListener(v -> {
                callback.setSelectedIndex(finalI);
                next();
            });
            confirmation.addView(button, i);
        }
        return view;
    }

    /*
    Trying to use a Dialog to handle ConfirmationCallback, however, the confirmation message is defined
    as OutputTextCallback instead with the Prompt attribute. Custom UI is require to show as Dialog.
     */
/*
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ConfirmationCallbackDialogFragment fragment = ConfirmationCallbackDialogFragment.newInstance(callback);
        fragment.show(getChildFragmentManager(), null);
    }

    public void onDismiss(ConfirmationCallback result) {
        callback.setSelectedIndex(result.getSelectedIndex());
        next();
    }

    public static class ConfirmationCallbackDialogFragment extends DialogFragment {

        private ConfirmationCallback callback;
        private ConfirmationCallbackFragment confirmationCallbackFragment;

        public static ConfirmationCallbackDialogFragment newInstance(ConfirmationCallback callback) {
            ConfirmationCallbackDialogFragment fragment = new ConfirmationCallbackDialogFragment();
            Bundle args = new Bundle();
            args.putSerializable("CALLBACK", callback);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getArguments() != null) {
                callback = (ConfirmationCallback) getArguments().getSerializable("CALLBACK");
            }
            confirmationCallbackFragment = (ConfirmationCallbackFragment) getParentFragment();
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            final View view = inflater.inflate(R.layout.fragment_confirmation_callback, container, false);
            TextView prompt = view.findViewById(R.id.prompt);
            prompt.setText(callback.getPrompt());
            LinearLayout confirmation = view.findViewById(R.id.confirmation);
            for (int i = 0; i < callback.getOptions().size(); i++) {
                Button button = new Button(getContext());
                button.setText(callback.getOptions().get(i));
                final int finalI = i;
                button.setOnClickListener(v -> {
                    callback.setSelectedIndex(finalI);
                    dismiss();
                });
                confirmation.addView(button, i);
            }
            return view;
        }

        @Override
        public void onDismiss(@NonNull DialogInterface dialog) {
            super.onDismiss(dialog);
            confirmationCallbackFragment.onDismiss(callback);
        }

        @Override
        public void onResume() {
            super.onResume();
            ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
        }

    }
 */
}
