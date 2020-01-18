/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import lombok.RequiredArgsConstructor;

/**
 * Object which represent the User Session
 */
public class FRSession {

    //Hold the current user session.
    private static FRSession current;

    private SessionManager sessionManager;

    private FRSession() {
        sessionManager = SessionManager.builder()
                .build();
    }

    /**
     * Logout the user
     */
    public void logout() {
        current = null;
        sessionManager.close();
    }

    /**
     * Retrieve the existing FRSession instance,
     * <p>
     * If user session does not exist return null, otherwise return the existing User session,
     * this cannot guarantee the existing user session is valid.
     *
     * @return The existing FRSession instance, or null if there is no user session.
     */
    public static FRSession getCurrentSession() {
        if (current != null) {
            return current;
        }

        FRSession session = new FRSession();
        if (session.sessionManager.hasSession()) {
            current = session;
        }
        return current;
    }


    public SSOToken getSessionToken() {
        return sessionManager.getSingleSignOnManager().getToken();
    }

    public static void authenticate(Context context, String serviceName, final NodeListener<FRSession> listener) {
        createFRAuth(context, serviceName)
                .next(context, listener);
    }

    private static FRAuth createFRAuth(Context context, String serviceName) {
        return FRAuth.builder()
                .serviceName(serviceName)
                .context(context)
                .interceptor(new FRSession.SessionInterceptor())
                .build();
    }

    @RequiredArgsConstructor
    private static class SessionInterceptor implements Interceptor {

        @Override
        public void intercept(Chain chain, Object any) {
            current = new FRSession();
            chain.proceed(current);
        }
    }
}
