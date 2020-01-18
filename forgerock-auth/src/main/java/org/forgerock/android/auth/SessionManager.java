/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import androidx.annotation.WorkerThread;

import lombok.Builder;
import lombok.Getter;

import org.forgerock.android.auth.exception.AuthenticationRequiredException;

import java.util.Arrays;
import java.util.List;

/**
 * Manage the user session
 */
class SessionManager {

    @Getter
    private TokenManager tokenManager;
    @Getter
    private SingleSignOnManager singleSignOnManager;
    @Getter
    private OAuth2Client oAuth2Client;
    private List<Interceptor> interceptors;

    @Builder
    public SessionManager(TokenManager tokenManager, SingleSignOnManager singleSignOnManager, OAuth2Client oAuth2Client) {
        Config config = Config.getInstance();

        this.tokenManager = config.applyDefaultIfNull(tokenManager);
        this.singleSignOnManager = config.applyDefaultIfNull(singleSignOnManager);
        this.oAuth2Client = config.applyDefaultIfNull(oAuth2Client);

        this.interceptors = Arrays.asList(
                new RetrieveSSOTokenInterceptor(this.singleSignOnManager),
                new RetrieveAccessTokenInterceptor(this.tokenManager),
                new OAuthInterceptor(this.oAuth2Client),
                new AccessTokenStoreInterceptor(this.tokenManager));
    }

    @WorkerThread
    public AccessToken getAccessToken() throws AuthenticationRequiredException {
        FRListenerFuture<AccessToken> listener = new FRListenerFuture<>();
        getAccessToken(listener);
        try {
            return listener.get();
        } catch (Exception e1) {
            throw new AuthenticationRequiredException(e1);
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
    void close() {
        tokenManager.revoke(null);
        singleSignOnManager.revoke(null);
    }

}
