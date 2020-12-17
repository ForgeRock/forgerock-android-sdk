/*
 * Copyright (c) 2019 - 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

/**
 * Follow the {@link Interceptor.Chain} and executes each {@link Interceptor}
 * in the chain.
 */
@Builder
public class InterceptorHandler implements Interceptor.Chain {

    private static final String TAG = InterceptorHandler.class.getSimpleName();

    @Getter
    private Context context;
    @Singular
    private List<? extends Interceptor<?>> interceptors;
    @Getter
    private FRListener listener;
    private int index;

    @Override
    public void proceed(Object data) {
        if (index >= interceptors.size()) {
            //end of the Chain, execute the caller Listener
            Listener.onSuccess(listener, data);
        } else {
            //process the next interceptor in the Chain
            try {
                Interceptor interceptor = interceptors.get(index);
                Logger.debug(TAG, "Processing interceptor: %s", interceptor.getClass().getSimpleName());
                interceptor.intercept(new InterceptorHandler(context, interceptors, listener, index + 1), data);
            } catch (ClassCastException e) { // The Interceptor cannot handle the data
                //skip the interceptor
                index++;
                proceed(data);
            } catch (Exception e) {
                listener.onException(e);
            }
        }
    }

}
