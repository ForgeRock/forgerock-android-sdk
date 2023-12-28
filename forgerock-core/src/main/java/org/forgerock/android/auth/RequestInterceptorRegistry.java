/*
 * Copyright (c) 2020 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

/**
 * Registry to manage {@link RequestInterceptor}
 */
public class RequestInterceptorRegistry {

    private static final RequestInterceptorRegistry INSTANCE = new RequestInterceptorRegistry();

    private RequestInterceptor[] requestInterceptors;

    private RequestInterceptorRegistry() {
    }

    /**
     * Returns a cached instance {@link RequestInterceptorRegistry}
     *
     * @return instance of {@link RequestInterceptorRegistry}
     */
    public static RequestInterceptorRegistry getInstance() {
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

    public RequestInterceptor[] getRequestInterceptors() {
        return this.requestInterceptors;
    }
}
