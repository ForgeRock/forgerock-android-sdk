/*
 * Copyright (c) 2019 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import net.openid.appauth.AppAuthConfiguration;

import org.forgerock.android.auth.exception.AuthenticationRequiredException;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import lombok.Builder;
import lombok.Getter;

/**
 * Manage the user session
 */
public class SessionManager {

    @Getter
    private TokenManager tokenManager;
    @Getter
    private SingleSignOnManager singleSignOnManager;
    private List<Interceptor<?>> interceptors;

    @Builder
    public SessionManager(TokenManager tokenManager, SingleSignOnManager singleSignOnManager) {

        this.tokenManager = tokenManager;
        this.singleSignOnManager = singleSignOnManager;

        this.interceptors = Arrays.asList(
                new RetrieveSSOTokenInterceptor(this.singleSignOnManager),
                new RetrieveAccessTokenInterceptor(this.singleSignOnManager, this.tokenManager),
                new OAuthInterceptor(this),
                new AccessTokenStoreInterceptor(this.tokenManager));
    }

    /**
     * Refresh the Access Token.
     *
     * @param listener Listener to listen for refresh event.
     */
    @WorkerThread
    public void refresh(FRListener<AccessToken> listener) {
        AccessToken token = tokenManager.getAccessToken();
        if(token == null) {
            Listener.onException(listener, new AuthenticationRequiredException("Access Token does not exists."));
            return;
        }
        tokenManager.refresh(token, listener);
    }

    /**
     * Retrieve the Access Token.
     *
     * @return The Access Token
     * @throws AuthenticationRequiredException Authentication is required to retrieve the Access Token
     */
    @WorkerThread
    public AccessToken getAccessToken() throws AuthenticationRequiredException {
        FRListenerFuture<AccessToken> listener = new FRListenerFuture<>();
        getAccessToken(listener);
        try {
            return listener.get();
        } catch (Exception e) {
            if (e instanceof ExecutionException) {
                /* TODO 3.0
                try {
                    throw ((ExecutionException) e).getCause();
                } catch (AuthenticationRequiredException | IOException | ApiException e1) {
                    throw e1;
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
                 */
                throw new AuthenticationRequiredException(e.getCause());
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve the {@link AccessToken}, if the {@link AccessToken} is expired, {@link AccessToken#getRefreshToken()} will
     * be used.
     *
     * @param listener The Listener to listen for the result
     */
    void getAccessToken(final FRListener<AccessToken> listener) {
        InterceptorHandler interceptorHandler = new InterceptorHandler(null, interceptors, listener, 0);
        interceptorHandler.proceed(null);
    }

    /**
     * Checks if a session exists
     *
     * @return whether there are valid or invalid session stored on this manager.
     */
    boolean hasSession() {
        return (singleSignOnManager.hasToken() || tokenManager.hasToken());
    }

    /**
     * Close the session, all tokens will be removed.
     */
    public void close() {
        close(() -> AppAuthConfiguration.DEFAULT);
    }

    /**
     * Close the user with specific {@link AppAuthConfiguration}, the {@link AppAuthConfiguration}
     * should match the configuration used for {@link FRUser.Browser}.
     * This method should only be used for centralized login.
     *
     * @param appAuthConfiguration The AppAuthConfiguration object
     */
    public void close(AppAuthConfiguration appAuthConfiguration) {
        close(() -> appAuthConfiguration);
    }

    /**
     * Close the session
     * @param appAuthConfiguration Supplier of AppAuthConfiguration, in case of not using centralize login
     *                             AppAuth library may not be included in the project, to avoid runtime exception
     *                             using supplier to provide AppAuthConfiguration.
     */
    private void close(Supplier<AppAuthConfiguration> appAuthConfiguration) {
        tokenManager.revokeAndEndSession(appAuthConfiguration, null);
        singleSignOnManager.revoke(null);
    }

    /**
     * Calling TokenManager to revoke OAuth2.0 tokens
     *
     * @param listener The Listener to listen for the result
     */
    void revokeAccessToken(FRListener<Void> listener) {
        tokenManager.revoke(listener);
    }

}
