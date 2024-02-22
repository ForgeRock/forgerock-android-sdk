/*
 * Copyright (c) 2019 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.OperationCanceledException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.Listener;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.PolicyAdvice;

import kotlin.Result;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

/**
 * Reference implementation of handing Advice with {@link DialogFragment}
 */
public class AdviceDialogFragment extends DialogFragment implements AuthHandler {

    private static final String CURRENT_EMBEDDED_FRAGMENT = "CURRENT_EMBEDDED_FRAGMENT";
    private static final String ARG_ADVICE = "advice";
    private FRViewModel<FRSession> viewModel;
    private boolean isCancel = true;

    public void setListener(Continuation<? super Unit> listener) {
        this.listener = listener;
    }

    private Continuation<? super Unit> listener;
    private PolicyAdvice advice;

    public static AdviceDialogFragment newInstance(PolicyAdvice advice) {
        AdviceDialogFragment fragment = new AdviceDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ADVICE, advice);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            advice = (PolicyAdvice) getArguments().getSerializable(ARG_ADVICE);
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (isCancel) {
            listener.resumeWith(new Result.Failure(new OperationCanceledException("Cancel Policy Decision Request")));
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_advice_dialog, container, false);
        viewModel = ViewModelProviders.of(this).get(FRSessionViewModel.class);

        viewModel.getNodeLiveData().observe(getViewLifecycleOwner(), node -> {
            Node n = node.getValue();
            if (n != null) {
                Fragment callbackFragment = CallbackFragmentFactory.getInstance().getFragment(n);
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.adviceCallback, callbackFragment, CURRENT_EMBEDDED_FRAGMENT).commit();
            }

        });

        viewModel.getResultLiveData().observe(getViewLifecycleOwner(), frSession ->  {
            isCancel = false;
            dismiss();
            listener.resumeWith(Unit.INSTANCE);
        });

        viewModel.getExceptionLiveData().observe(getViewLifecycleOwner(), e -> {
            isCancel = false;
            dismiss();
            Exception exception = e.getValue();
            if (exception != null) {
                listener.resumeWith(new Result.Failure(exception) );
            }
        });

        viewModel.authenticate(getContext(), advice);

        return view;
    }

    @Override
    public void next(Node node) {
        viewModel.next(getContext(), node);
    }

    @Override
    public void cancel(Exception e) {
        dismiss();
    }
}
