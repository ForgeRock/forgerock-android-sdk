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


    /**
     * Retrieve the stored Session Token.
     *
     * @return The Session Token
     */
    public SSOToken getSessionToken() {
        return sessionManager.getSingleSignOnManager().getToken();
    }

    /**
     * Trigger the Authentication Tree flow process with the {@link PolicyAdvice}
     *
     * @param context  The Application Context
     * @param advice   Policy Advice for Step up authentication.
     * @param listener Listener to listen login event.
     */

    public void authenticate(Context context, PolicyAdvice advice, final NodeListener<FRSession> listener) {
        FRAuth.builder()
                .advice(advice)
                .context(context)
                .interceptor(new FRSession.SessionInterceptor())
                .build().next(context, listener);
    }

    /**
     * Trigger the Authentication Tree flow process with the {@link PolicyAdvice}
     *
     * @param context     The Application Context
     * @param serviceName Authentication Tree name
     * @param listener    Listener to listen login event.
     */

    public static void authenticate(Context context, String serviceName, final NodeListener<FRSession> listener) {
        FRAuth.builder()
                .serviceName(serviceName)
                .context(context)
                .interceptor(new FRSession.SessionInterceptor())
                .build().next(context, listener);
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
