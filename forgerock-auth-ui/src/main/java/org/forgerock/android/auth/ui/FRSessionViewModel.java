/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui;

import android.content.Context;

import androidx.lifecycle.ViewModel;

import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.PolicyAdvice;

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
                getExceptionLiveData().postValue(e);
            }
        };
    }

    public void authenticate(Context context) {
        FRSession.authenticate(context, context.getString(R.string.forgerock_auth_service), nodeListener);
    }

    @Override
    public void authenticate(Context context, PolicyAdvice advice) {
        FRSession.getCurrentSession().authenticate(context, advice, nodeListener);
    }

    public void register(Context context) {
        FRSession.authenticate(context, context.getString(R.string.forgerock_registration_service), nodeListener);
    }

    public void next(Context context, Node node) {
        node.next(context, nodeListener);
    }

}
