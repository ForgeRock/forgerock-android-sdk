/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import org.forgerock.android.auth.*;
import org.forgerock.android.auth.exception.AuthenticationException;
import org.forgerock.android.auth.exception.AuthenticationRequiredException;
import org.forgerock.android.auth.exception.AuthenticationTimeoutException;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class LoginFragment extends Fragment implements AuthHandler {

    private static final String CURRENT_EMBEDDED_FRAGMENT = "CURRENT_EMBEDDED_FRAGMENT";
    private boolean loadOnStartup;
    FRViewModel<FRSession> viewModel;
    ProgressBar progressBar;
    //Listener to listener for Login Event
    private FRListener<Void> listener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.login_fragment, container, false);
        progressBar = view.findViewById(R.id.progress);
        setListener(getParentFragment());

        viewModel = ViewModelProviders.of(this).get(FRSessionViewModel.class);

        if (savedInstanceState == null) {
            if (loadOnStartup) {
                start();
            }
        }

        viewModel.getNodeLiveData().observe(getViewLifecycleOwner(), node -> {
            progressBar.setVisibility(INVISIBLE);
            Node n = node.getValue();
            if (n != null) {
                Fragment callbackFragment = CallbackFragmentFactory.getInstance().getFragment(n);
                getChildFragmentManager().beginTransaction()
                        .replace(getId(), callbackFragment, CURRENT_EMBEDDED_FRAGMENT).commit();
            }

        });

        viewModel.getResultLiveData().observe(getViewLifecycleOwner(), frUser -> {
            progressBar.setVisibility(INVISIBLE);
            Listener.onSuccess(listener, null);
        });

        viewModel.getExceptionLiveData().observe(getViewLifecycleOwner(), e -> {
            progressBar.setVisibility(INVISIBLE);
            if (!handleException(e)) {
                cancel(e);
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        setListener(context);
    }

    private void setListener(Object o) {
        if (o instanceof FRListener) {
            this.listener = (FRListener<Void>) o;
        }
    }

    public void start() {
        progressBar.setVisibility(VISIBLE);
        viewModel.login(getContext());
    }

    @Override
    public void next(Node current) {
        progressBar.setVisibility(VISIBLE);
        viewModel.next(getContext(), current);
    }

    @Override
    public void cancel(Exception e) {
        Listener.onException(listener, e );
    }

    private boolean handleException(final Exception e) {
        if (e instanceof AuthenticationRequiredException || e instanceof AuthenticationTimeoutException) {
            viewModel.login(getContext());
        } else if (e instanceof AuthenticationException) {
            Fragment fragment = getChildFragmentManager().findFragmentByTag(CURRENT_EMBEDDED_FRAGMENT);
            if (fragment instanceof AuthenticationExceptionListener) {
                ((AuthenticationExceptionListener)fragment).onAuthenticationException((AuthenticationException) e);
            } else {
                cancel(e);
            }
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void onInflate(@NonNull Context context, @NonNull AttributeSet attrs, @Nullable Bundle savedInstanceState) {
        super.onInflate(context, attrs, savedInstanceState);
        //Retrieve fragment configuration from attr.xml
        TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.LoginFragment);
        this.loadOnStartup = a.getBoolean(R.styleable.LoginFragment_loadOnStartup, true);
        a.recycle();
    }
}
