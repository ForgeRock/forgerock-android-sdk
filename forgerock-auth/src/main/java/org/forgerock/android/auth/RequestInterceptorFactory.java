/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import lombok.Getter;

/**
 * Factory to manage {@link RequestInterceptor}
 */
public class RequestInterceptorFactory {

    private static final RequestInterceptorFactory INSTANCE = new RequestInterceptorFactory();

    @Getter
    private RequestInterceptor[] requestInterceptors;

    private RequestInterceptorFactory() {
    }

    /**
     * Returns a cached instance {@link RequestInterceptorFactory}
     *
     * @return instance of {@link RequestInterceptorFactory}
     */
    public static RequestInterceptorFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Register new {@link RequestInterceptor}(s)
     *
     * @param requestInterceptors A list of request interceptors
     */
    public void register(RequestInterceptor... requestInterceptors) {
        this.requestInterceptors = requestInterceptors;
    }

}
