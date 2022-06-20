/*
 * Copyright (c) 2019 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.net.Uri;
import android.util.LruCache;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.*;

/**
 * Model of an authentication service.
 * <p>
 * To create a new AuthService object use the static {@link AuthService#builder()}.
 */
public class AuthService {

    private static final String TAG = AuthService.class.getSimpleName();
    public static final String SERVICE = "service";
    public static final String COMPOSITE_ADVICE = "composite_advice";
    public static final String SUSPENDED_ID = "suspendedId";

    @Getter
    private String name;
    @Getter
    private String authServiceId;
    @Getter
    private Uri resumeURI;

    private PolicyAdvice advice;
    private List<Interceptor<?>> interceptors;

    private AuthServiceClient authServiceClient;

    //Cached the last 10 auth tree only.
    private static LruCache<String, AuthService> authServices = new LruCache<>(10);

    @Builder
    private AuthService(String name,
                        PolicyAdvice advice,
                        Uri resumeURI,
                        ServerConfig serverConfig,
                        @Singular List<Interceptor<?>> interceptors) {

        this.name = name;
        this.advice = advice;
        this.resumeURI = resumeURI;
        if (name == null && advice == null && resumeURI == null) {
            throw new IllegalArgumentException("Either Service name or Advice or SuspendedId is required.");
        }
        validateResumeUri();
        authServiceId = UUID.randomUUID().toString();
        authServiceClient = new AuthServiceClient(serverConfig);
        this.interceptors = interceptors;
    }

    /**
     * Check to see if it is resuming the tree.
     *
     * @return true if it is resuming the tree, else false.
     */
    boolean isResume() {
        return resumeURI != null;
    }

    /**
     * Verify if the resume URI contains suspendedID.
     *
     * @throws IllegalArgumentException when resumeUri does not contains suspended ID.
     */
    void validateResumeUri() {
        if (resumeURI != null && resumeURI.getQueryParameter(SUSPENDED_ID) == null) {
            throw new IllegalArgumentException("Suspended Id is missing from the resume URI");
        }
    }

    public static AuthServiceBuilder builder() {
        return new AuthServiceBuilder();
    }

    /**
     * Move on to the next node in the tree.
     *
     * @param context     An Application Context
     * @param currentNode The current state with addition data requested from the Callback
     * @param listener    Listener for receiving {@link AuthService} related changes
     */
    static void goToNext(final Context context, final Node currentNode, final NodeListener<?> listener) {
        final AuthService authService = authServices.get(currentNode.getAuthServiceId());
        if (authService == null) {
            Logger.warn(TAG, "Auth Service id: %s not found.", currentNode.getAuthServiceId());
            throw new IllegalStateException("AuthService Not Found!");
        }
        authService.authServiceClient.authenticate(authService, currentNode,
                new AuthServiceResponseHandler(
                        authService,
                        new NodeInterceptorHandler(
                                context,
                                authService.interceptors,
                                listener, 0)));

    }

    /**
     * Move on to the next node in the tree.
     *
     * @param context  The Application Context
     * @param listener Listener for receiving {@link AuthService} related changes
     */
    public void next(Context context, final NodeListener<?> listener) {
        Logger.debug(TAG, "Journey start: %s", getName());
        authServiceClient.authenticate(this,
                new AuthServiceResponseHandler(this,
                        new NodeInterceptorHandler(context, interceptors, listener, 0)));
    }

    String getAuthIndexType() {
        if (isService()) {
            return SERVICE;
        }
        return COMPOSITE_ADVICE;
    }

    String getAuthIndexValue() {
        if (isService()) {
            return name;
        }
        return advice.toString();
    }

    private boolean isService() {
        return name != null;
    }


    void done() {
        Logger.debug(TAG, "Auth Service %s flow completed or suspended", authServiceId);
        authServices.remove(authServiceId);
    }

    public static class AuthServiceBuilder {

        public AuthService build() {

            List<Interceptor<?>> interceptors;
            switch (this.interceptors == null ? 0 : this.interceptors.size()) {
                case 0:
                    interceptors = Collections.emptyList();
                    break;
                case 1:
                    interceptors = Collections.singletonList(this.interceptors.get(0));
                    break;
                default:
                    interceptors = Collections.unmodifiableList(new ArrayList<>(this.interceptors));
            }

            AuthService authService = new AuthService(name,
                    advice, resumeURI, serverConfig, interceptors);
            authServices.put(authService.authServiceId, authService);
            return authService;

        }
    }

}
