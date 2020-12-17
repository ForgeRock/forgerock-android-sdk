/*
 * Copyright (c) 2019 - 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import lombok.RequiredArgsConstructor;

/**
 * Interceptor to intercept the received token and exchange to {@link AccessToken}.
 */
@RequiredArgsConstructor
class RetrieveSSOTokenInterceptor implements Interceptor<Void> {

    private final SingleSignOnManager singleSignOnManager;

    @Override
    public void intercept(final Chain chain, final Void input) {
        SSOToken ssoToken = singleSignOnManager.getToken();
        chain.proceed(ssoToken);
    }

}
