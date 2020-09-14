/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.openid.appauth.AuthorizationResponse;

import org.forgerock.android.auth.exception.AlreadyAuthenticatedException;
import org.forgerock.android.auth.exception.AuthenticationRequiredException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class FRUser {

    //Hold the current login user.
    private static FRUser current;

    private SessionManager sessionManager;

    private FRUser() {
        sessionManager = Config.getInstance().getSessionManager();
    }

    /**
     * Retrieve the existing FRUser instance,
     * <p>
     * If user session does not exist return null, otherwise return the existing FRUser with the associated user session,
     * this cannot guarantee the existing user session is valid.
     *
     * @return The existing FRUser instance, or null if there is no user session.
     */
    public static FRUser getCurrentUser() {
        if (current != null) {
            return current;
        }

        FRUser user = new FRUser();
        if (user.sessionManager.hasSession()) {
            current = user;
        }
        return current;
    }

    /**
     * Logout the user
     */
    public void logout() {
        current = null;
        sessionManager.close();
    }


    /**
     * Retrieve the {@link AccessToken} asynchronously,
     *
     * <p>
     * If the stored {@link AccessToken} is expired, auto refresh the token.
     *
     * @param listener Listener to listen get Access Token event.
     */

    public void getAccessToken(FRListener<AccessToken> listener) {
        sessionManager.getAccessToken(listener);
    }

    @WorkerThread
    public AccessToken getAccessToken() throws AuthenticationRequiredException {
        return sessionManager.getAccessToken();
    }

    /**
     * Handles REST requests to the OpenId Connect userinfo endpoint for retrieving information about the user who granted
     * the authorization for the token.
     *
     * @param listener Listener to listen get UserInfo event.
     */
    public void getUserInfo(final FRListener<UserInfo> listener) {

        UserService.builder()
                .serverConfig(Config.getInstance().getServerConfig())
                .build()
                .userinfo(new FRListener<UserInfo>() {
                    @Override
                    public void onSuccess(UserInfo result) {
                        Listener.onSuccess(listener, result);
                    }

                    @Override
                    public void onException(Exception e) {
                        Listener.onException(listener, e);
                    }
                });
    }

    /**
     * Trigger the user login process, the login service name is defined under <b>string.xml</b> file with
     * <b>forgerock_auth_service</b>
     *
     * @param context  The Application Context
     * @param listener Listener to listen login event.
     *                 <b> Throw {@link AlreadyAuthenticatedException} user session already exists.
     */
    public static void login(Context context, final NodeListener<FRUser> listener) {
        SessionManager sessionManager = Config.getInstance().getSessionManager();

        if (sessionManager.hasSession()) {
            Listener.onException(listener, new AlreadyAuthenticatedException("User is already authenticated"));
            return;
        }

        createFRAuth(context, context.getString(R.string.forgerock_auth_service), sessionManager)
                .next(context, listener);

    }

    private static FRAuth createFRAuth(Context context, String serviceName, SessionManager sessionManager) {
        return FRAuth.builder()
                .serviceName(serviceName)
                .context(context)
                .serverConfig(Config.getInstance().getServerConfig())
                .sessionManager(sessionManager)
                .interceptor(new OAuthInterceptor(sessionManager.getTokenManager()))
                .interceptor(new AccessTokenStoreInterceptor(sessionManager.getTokenManager()))
                .interceptor(new UserInterceptor())
                .build();
    }

    /**
     * Trigger the user registration process, the registration service name is defined under <b>string.xml</b> file with
     * <b>forgerock_registration_service</b>
     *
     * @param context  The Application Context
     * @param listener Listener to listen register event.
     *                 <b> Throw {@link AlreadyAuthenticatedException} user session already exists.
     */
    public static void register(Context context, NodeListener<FRUser> listener) {
        SessionManager sessionManager = Config.getInstance().getSessionManager();

        if (sessionManager.hasSession()) {
            Listener.onException(listener, new AlreadyAuthenticatedException("User is already authenticated"));
            return;
        }

        createFRAuth(context, context.getString(R.string.forgerock_registration_service), sessionManager)
                .next(context, listener);
    }

    @RequiredArgsConstructor
    private static class UserInterceptor implements Interceptor {

        @Override
        public void intercept(Chain chain, Object any) {
            current = new FRUser();
            chain.proceed(current);
        }
    }

    public static Browser browser() {
        return new Browser();
    }

    @Getter(AccessLevel.PACKAGE)
    public static class Browser {

        private FRListener<AuthorizationResponse> listener;
        private AppAuthConfigurer appAuthConfigurer = new AppAuthConfigurer(this);

        public AppAuthConfigurer appAuthConfigurer() {
            return appAuthConfigurer;
        }

        public void login(Fragment fragment, FRListener<FRUser> listener) {
            login(fragment.getContext(), fragment.getFragmentManager(), listener);
        }

        public void login(FragmentActivity activity, FRListener<FRUser> listener) {
            login(activity.getApplicationContext(), activity.getSupportFragmentManager(), listener);
        }

        private void login(Context context, FragmentManager manager, FRListener<FRUser> listener) {

            SessionManager sessionManager = Config.getInstance().getSessionManager();

            if (sessionManager.hasSession()) {
                Listener.onException(listener, new AlreadyAuthenticatedException("User is already authenticated"));
                return;
            }

            this.listener = new FRListener<AuthorizationResponse>() {
                @Override
                public void onSuccess(AuthorizationResponse result) {
                    InterceptorHandler interceptorHandler = InterceptorHandler.builder()
                            .context(context)
                            .listener(listener)
                            .interceptor(new ExchangeAccessTokenInterceptor(sessionManager.getTokenManager()))
                            .interceptor(new AccessTokenStoreInterceptor(sessionManager.getTokenManager()))
                            .interceptor(new UserInterceptor())
                            .build();

                    interceptorHandler.proceed(result);

                }

                @Override
                public void onException(Exception e) {
                    Listener.onException(listener, e);
                }
            };

            AppAuthFragment.init(manager, this);

        }
    }
}
