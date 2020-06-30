/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import java.util.List;

/**
 * Follow the {@link Interceptor.Chain} and executes each {@link Interceptor}
 * in the chain.
 */
class NodeInterceptorHandler extends InterceptorHandler implements NodeListener<SSOToken> {

    NodeInterceptorHandler(Context context, List<Interceptor<?>> interceptors, NodeListener<?> listener, int index) {
        super(context, interceptors, listener, index);
    }

    @Override
    public void onCallbackReceived(Node node) {
        ((NodeListener<?>)getListener()).onCallbackReceived(node);
    }

    @Override
    public void onSuccess(SSOToken result) {
        proceed(result);
    }

    @Override
    public void onException(Exception e) {
        getListener().onException(e);
    }


}
