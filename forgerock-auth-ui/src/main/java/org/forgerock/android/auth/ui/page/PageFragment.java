/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.page;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.exception.AuthenticationException;
import org.forgerock.android.auth.ui.AuthHandler;
import org.forgerock.android.auth.ui.AuthenticationExceptionListener;
import org.forgerock.android.auth.ui.CallbackFragmentFactory;

/**
 * Fragment which represent a Page Node with Stage attribute
 */
public abstract class PageFragment extends Fragment implements AuthenticationExceptionListener {

    /** The node response from the Auth Service */
    protected Node node;

    private AuthHandler authHandler;

    public PageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            node = (Node) getArguments().getSerializable(CallbackFragmentFactory.NODE);
        }
        setAuthHandler(getParentFragment());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        setAuthHandler(context);
    }

    private void setAuthHandler(Object o) {
        if (o instanceof AuthHandler) {
            this.authHandler = (AuthHandler) o;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        authHandler = null;
    }

    public void onDataCollected() {
        authHandler.next(node);
    }

    public void cancel(Exception e) {
        authHandler.cancel(e);
    }

    @Override
    public void onAuthenticationException(AuthenticationException e) {

    }
}
