/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

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
        getAccessToken(new FRListener<AccessToken>() {
            @Override
            public void onSuccess(AccessToken result) {
                tokenManager.refresh(result, listener);
            }

            @Override
            public void onException(@NonNull Exception e) {
                Listener.onException(listener, e);
            }
        });
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
        tokenManager.revoke(null);
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

    @VisibleForTesting
    public void close(FRListener<Void> listener) {
        tokenManager.revoke(new FRListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                closeSession(listener);
            }

            @Override
            public void onException(Exception e) {
                closeSession(listener);
            }
        });
    }

    private void closeSession(FRListener<Void> listener) {
        singleSignOnManager.revoke(listener);
    }

}
