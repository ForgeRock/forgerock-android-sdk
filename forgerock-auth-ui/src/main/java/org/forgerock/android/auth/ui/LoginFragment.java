/*
 * Copyright (c) 2019 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.Listener;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.exception.AuthenticationException;
import org.forgerock.android.auth.exception.AuthenticationRequiredException;
import org.forgerock.android.auth.exception.AuthenticationTimeoutException;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static org.forgerock.android.auth.AuthService.SUSPENDED_ID;

public class LoginFragment extends Fragment implements AuthHandler {

    private static final String CURRENT_EMBEDDED_FRAGMENT = "CURRENT_EMBEDDED_FRAGMENT";
    public static final String TREE_NAME = "TREE_NAME";
    private boolean loadOnStartup;
    FRViewModel<FRSession> viewModel;
    ProgressBar progressBar;
    //Listener to listener for Login Event
    private FRListener<Void> listener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListener(getParentFragment());
        viewModel = new ViewModelProvider(this).get(FRSessionViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.login_fragment, container, false);
        progressBar = view.findViewById(R.id.progress);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel.getNodeLiveData().observe(getViewLifecycleOwner(), node -> {
            progressBar.setVisibility(INVISIBLE);
            Node n = node.getValue();
            if (n != null) {
                Fragment callbackFragment = CallbackFragmentFactory.getInstance().getFragment(n);
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.container, callbackFragment, CURRENT_EMBEDDED_FRAGMENT).commit();
            }
        });

        viewModel.getResultLiveData().observe(getViewLifecycleOwner(), frUser -> {
            progressBar.setVisibility(INVISIBLE);
            Listener.onSuccess(listener, null);
        });

        viewModel.getExceptionLiveData().observe(getViewLifecycleOwner(), e -> {
            progressBar.setVisibility(INVISIBLE);
            Exception exception = e.getValue();
            if (exception != null && !handleException(exception)) {
                cancel(exception);
            }
        });

        if (savedInstanceState == null) {
            if (loadOnStartup) {
                start();
            }
        }
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
        if (getActivity() != null) {
            Intent intent = getActivity().getIntent();
            Uri data = intent.getData();
            //If the intent contains suspendedId, we resume the flow
            if (data != null && data.getQueryParameter(SUSPENDED_ID) != null) {
                //Resume suspended Tree
                viewModel.authenticate(getContext(), data);
                return;
            }
        }
        viewModel.authenticate(getContext());
    }

    @Override
    public void next(Node current) {
        progressBar.setVisibility(VISIBLE);
        viewModel.next(getContext(), current);
    }

    @Override
    public void cancel(Exception e) {

        //We clean up the child fragment(s), so it won't be recreated with lifecycle method
        FragmentManager fm = getChildFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        for (Fragment fragment : fm.getFragments()) {
            ft.remove(fragment);
        }
        ft.commit();
        Listener.onException(listener, e);
    }

    /**
     * Handle Exception during Intelligent Tree Authentication
     *
     * @param e The Exception
     * @return True if user can continue with the current Node (e.g Invalid password)
     * , False if we cannot continue the flow.
     */
    private boolean handleException(final Exception e) {
        if (e instanceof AuthenticationRequiredException || e instanceof AuthenticationTimeoutException) {
            viewModel.authenticate(getContext());
        } else if (e instanceof AuthenticationException) {
            Fragment fragment = getChildFragmentManager().findFragmentByTag(CURRENT_EMBEDDED_FRAGMENT);
            if (fragment instanceof AuthenticationExceptionListener) {
                ((AuthenticationExceptionListener) fragment).onAuthenticationException((AuthenticationException) e);
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
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LoginFragment);
        this.loadOnStartup = a.getBoolean(R.styleable.LoginFragment_loadOnStartup, true);
        a.recycle();
    }
}
