/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

/**
 * Interceptor interface, generic interface for interceptor pattern.
 */
public interface Interceptor<T> {

    /**
     * Intercept the chain
     *
     * @param chain The interceptor chain
     * @param data  The data received from previous chain
     */
    void intercept(Chain chain, T data);

    /**
     * Chain interface to chain up a list of interceptors.
     */
    interface Chain {

        /**
         * Retrieve the Application context
         *
         * @return The Application Context
         */
        Context getContext();

        /**
         * The listener to listen for the chain processing
         * @return The Listener to listen for chain event
         */
        FRListener getListener();

        /**
         * Proceed and execute the chain.
         * @param object The chain context
         */
        void proceed(Object object);

    }

}
