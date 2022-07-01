/*
 * Copyright (c) 2019 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.ViewModel;

import org.forgerock.android.auth.Config;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.PolicyAdvice;

import static org.forgerock.android.auth.ui.LoginFragment.TREE_NAME;

/**
 * {@link ViewModel} Wrapper for {@link org.forgerock.android.auth.FRSession}
 */
public class FRSessionViewModel extends FRViewModel<FRSession> {

    private NodeListener<FRSession> nodeListener;

    public FRSessionViewModel() {
        nodeListener = new NodeListener<FRSession>() {
            @Override
            public void onCallbackReceived(Node node) {
                getNodeLiveData().postValue(new SingleLiveEvent<>(node));
            }

            @Override
            public void onSuccess(FRSession result) {
                getResultLiveData().postValue(result);
            }

            @Override
            public void onException(Exception e) {
                getExceptionLiveData().postValue(new SingleLiveEvent<>(e));
            }
        };
    }

    public void authenticate(Context context) {
        String tree = Config.getInstance().getAuthServiceName();
        if (context instanceof Activity) {
            String treeName = ((Activity) context).getIntent().getStringExtra(TREE_NAME);
            if (treeName != null) {
                tree = treeName;
            }
        }
        FRSession.authenticate(context, tree, nodeListener);
    }

    @Override
    public void authenticate(Context context, PolicyAdvice advice) {
        FRSession.getCurrentSession().authenticate(context, advice, nodeListener);
    }

    @Override
    public void authenticate(Context context, Uri resumeUri) {
        FRSession.authenticate(context, resumeUri, nodeListener);
    }

    public void register(Context context) {
        FRSession.authenticate(context, Config.getInstance().getRegistrationServiceName(), nodeListener);
    }

    public void next(Context context, Node node) {
        node.next(context, nodeListener);
    }

}
