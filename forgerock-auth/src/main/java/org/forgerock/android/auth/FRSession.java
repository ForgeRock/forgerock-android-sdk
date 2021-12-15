/*
 * Copyright (c) 2019 - 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import lombok.RequiredArgsConstructor;

/**
 * Object which represent the User Session
 */
public class FRSession {

    //Hold the current user session.
    private static final AtomicReference<FRSession> current = new AtomicReference<>();

    static {
        EventDispatcher.TOKEN_REMOVED.addObserver((o, arg) -> current.set(null));
    }

    private final SessionManager sessionManager;

    private FRSession() {
        sessionManager = Config.getInstance().getSessionManager();
    }

    /**
     * Logout the user
     */
    public void logout() {
        current.set(null);
        sessionManager.close();
        FRLifecycle.dispatchLogout();
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
        if (current.get() != null) {
            return current.get();
        }

        FRSession session = new FRSession();
        if (session.sessionManager.hasSession()) {
            current.set(session);
            return current.get();
        } else {
            return null;
        }
    }


    /**
     * Retrieve the stored Session Token.
     *
     * @return The Session Token
     */
    public SSOToken getSessionToken() {
        return sessionManager.getSingleSignOnManager().getToken();
    }

    public Collection<String> getSessionCookies() {
        return sessionManager.getSingleSignOnManager().getCookies();
    }


    /**
     * Trigger the Authentication Tree flow process with the {@link PolicyAdvice}
     *
     * @param context  The Application Context
     * @param advice   Policy Advice for Step up authentication.
     * @param listener Listener to listen for authenticate event.
     *                 <b> {@link NodeListener#onSuccess(Object)} on success login,  {@link FRSession} is returned.
     *                 <b> {@link NodeListener#onCallbackReceived(Node)} step to the next node, {@link Node} is returned.
     *                 <b> throws {@link org.forgerock.android.auth.exception.AuthenticationException when server returns {@link java.net.HttpURLConnection#HTTP_UNAUTHORIZED}
     *                 <b> throws {@link org.forgerock.android.auth.exception.ApiException} When server return errors.
     *                 <b> throws {@link javax.security.auth.callback.UnsupportedCallbackException}
     *                 When {@link org.forgerock.android.auth.callback.Callback} returned from Server is not supported by the SDK.
     *                 <b> throws {@link org.forgerock.android.auth.exception.AuthenticationTimeoutException} When Authentication tree timeout
     *                 <b> throws {@link org.json.JSONException} when failed to parse server response as JSON String.
     *                 <b> throws {@link IOException } When there is any network error.
     *                 <b> throws {@link java.net.MalformedURLException} When failed to parse the URL for API request.
     *                 <b> throws {@link NoSuchMethodException} or {@link SecurityException} When failed to initialize the Callback class.
     */

    public void authenticate(Context context, PolicyAdvice advice, final NodeListener<FRSession> listener) {
        FRAuth.builder()
                .context(context)
                .advice(advice)
                .serverConfig(Config.getInstance().getServerConfig())
                .sessionManager(sessionManager)
                .interceptor(new FRSession.SessionInterceptor())
                .build().next(context, listener);
    }

    /**
     * Trigger the Authentication Tree flow process with the provided tree name
     *
     * @param context     The Application Context
     * @param serviceName Authentication Tree name
     * @param listener    Listener to listen for authenticate event.
     *                    <b> {@link NodeListener#onSuccess(Object)} on success login,  {@link FRSession} is returned.
     *                    <b> {@link NodeListener#onCallbackReceived(Node)} step to the next node, {@link Node} is returned.
     *                    <b> throws {@link org.forgerock.android.auth.exception.AuthenticationException when server returns {@link java.net.HttpURLConnection#HTTP_UNAUTHORIZED}
     *                    <b> throws {@link org.forgerock.android.auth.exception.ApiException} When server return errors.
     *                    <b> throws {@link javax.security.auth.callback.UnsupportedCallbackException}
     *                    When {@link org.forgerock.android.auth.callback.Callback} returned from Server is not supported by the SDK.
     *                    <b> throws {@link org.forgerock.android.auth.exception.AuthenticationTimeoutException} When Authentication tree timeout
     *                    <b> throws {@link org.json.JSONException} when failed to parse server response as JSON String.
     *                    <b> throws {@link IOException } When there is any network error.
     *                    <b> throws {@link java.net.MalformedURLException} When failed to parse the URL for API request.
     *                    <b> throws {@link NoSuchMethodException} or {@link SecurityException} When failed to initialize the Callback class.
     */

    public static void authenticate(Context context, String serviceName, final NodeListener<FRSession> listener) {
        FRAuth.builder()
                .context(context)
                .serviceName(serviceName)
                .serverConfig(Config.getInstance().getServerConfig())
                .sessionManager(Config.getInstance().getSessionManager())
                .interceptor(new FRSession.SessionInterceptor())
                .build().next(context, listener);
    }

    /**
     * Trigger the Authentication Tree flow process with the provided resume URI
     *
     * @param context   The Application Context
     * @param resumeURI Resume URI
     * @param listener  Listener to listen for authenticate event.
     *                  <b> {@link NodeListener#onSuccess(Object)} on success login,  {@link FRSession} is returned.
     *                  <b> {@link NodeListener#onCallbackReceived(Node)} step to the next node, {@link Node} is returned.
     *                  <b> throws {@link org.forgerock.android.auth.exception.AuthenticationException when server returns {@link java.net.HttpURLConnection#HTTP_UNAUTHORIZED}
     *                  <b> throws {@link org.forgerock.android.auth.exception.ApiException} When server return errors.
     *                  <b> throws {@link javax.security.auth.callback.UnsupportedCallbackException}
     *                  When {@link org.forgerock.android.auth.callback.Callback} returned from Server is not supported by the SDK.
     *                  <b> throws {@link org.forgerock.android.auth.exception.SuspendedAuthSessionException} When Suspended ID timeout
     *                  <b> throws {@link org.forgerock.android.auth.exception.AuthenticationTimeoutException} When Authentication tree timeout
     *                  <b> throws {@link org.json.JSONException} when failed to parse server response as JSON String.
     *                  <b> throws {@link IOException } When there is any network error.
     *                  <b> throws {@link java.net.MalformedURLException} When failed to parse the URL for API request.
     *                  <b> throws {@link NoSuchMethodException} or {@link SecurityException} When failed to initialize the Callback class.
     */
    public static void authenticate(Context context, Uri resumeURI, final NodeListener<FRSession> listener) {
        FRAuth.builder()
                .context(context)
                .resumeURI(resumeURI)
                .serverConfig(Config.getInstance().getServerConfig())
                .sessionManager(Config.getInstance().getSessionManager())
                .interceptor(new FRSession.SessionInterceptor())
                .build().next(context, listener);
    }

    @RequiredArgsConstructor
    private static class SessionInterceptor implements Interceptor<SSOToken> {

        @Override
        public void intercept(Chain chain, SSOToken ssoToken) {
            if (ssoToken == null) {
                //We do not set the static session
                chain.proceed(null);
            } else {
                current.set(new FRSession());
                chain.proceed(current.get());
            }
        }
    }
}
