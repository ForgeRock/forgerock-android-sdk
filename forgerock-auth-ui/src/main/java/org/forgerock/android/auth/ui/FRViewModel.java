/*
 * Copyright (c) 2019 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.forgerock.android.auth.FRUser;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.PolicyAdvice;

/**
 * {@link ViewModel} Wrapper for {@link FRUser}
 */
public abstract class FRViewModel<T> extends ViewModel {

    private MutableLiveData<SingleLiveEvent<Node>> nodeLiveData = new MutableLiveData<>();
    private MutableLiveData<T> resultLiveData = new MutableLiveData<>();

    public MutableLiveData<SingleLiveEvent<Node>> getNodeLiveData() {
        return nodeLiveData;
    }

    public MutableLiveData<T> getResultLiveData() {
        return resultLiveData;
    }

    public MutableLiveData<SingleLiveEvent<Exception>> getExceptionLiveData() {
        return exceptionLiveData;
    }

    private MutableLiveData<SingleLiveEvent<Exception>> exceptionLiveData = new MutableLiveData<>();

    private NodeListener nodeListener;

    public FRViewModel() {
        nodeListener = new NodeListener<T>() {
            @Override
            public void onCallbackReceived(Node node) {
                nodeLiveData.postValue(new SingleLiveEvent<>(node));
            }

            @Override
            public void onSuccess(T result) {
                resultLiveData.postValue(result);
            }

            @Override
            public void onException(Exception e) {
                exceptionLiveData.postValue(new SingleLiveEvent<>(e));
            }
        };
    }

    public abstract void authenticate(Context context);

    public abstract void authenticate(Context context, PolicyAdvice advice);

    public abstract void authenticate(Context context, Uri resumeUri);

    public abstract void register(Context context);

    public void next(Context context, Node node) {
        node.next(context, nodeListener);
    }

}
