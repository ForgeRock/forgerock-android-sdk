/*
 * Copyright (c) 2019 - 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.ViewModel;

import org.forgerock.android.auth.FRUser;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.PolicyAdvice;

/**
 *
 * {@link ViewModel} Wrapper for {@link FRUser}
 * @deprecated As of release 1.1, replaced by {@link FRSessionViewModel} ()}
 */
@Deprecated
public class FRUserViewModel extends FRViewModel<FRUser> {

    private NodeListener<FRUser> nodeListener;

    public FRUserViewModel() {
        nodeListener = new NodeListener<FRUser>() {
            @Override
            public void onCallbackReceived(Node node) {
                getNodeLiveData().postValue(new SingleLiveEvent<>(node));
            }

            @Override
            public void onSuccess(FRUser result) {
                getResultLiveData().postValue(result);
            }

            @Override
            public void onException(Exception e) {
                getExceptionLiveData().postValue(new SingleLiveEvent<>(e));
            }
        };
    }

    public void authenticate(Context context) {
        FRUser.login(context, nodeListener);
    }

    @Override
    public void authenticate(Context context, PolicyAdvice advice) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void authenticate(Context context, Uri resumeUri) {
        throw new UnsupportedOperationException();
    }

    public void register(Context context) {
        FRUser.register(context, nodeListener);
    }

    public void next(Context context, Node node) {
        node.next(context, nodeListener);
    }

}
