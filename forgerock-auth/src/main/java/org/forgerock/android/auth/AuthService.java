/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.util.LruCache;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;

import org.forgerock.android.auth.callback.*;

import java.util.*;

/**
 * Model of an authentication service.
 * <p>
 * To create a new AuthService object use the static {@link AuthService#builder()}.
 */
public class AuthService {

    private static final String TAG = AuthService.class.getSimpleName();

    private final Map<String, Class<? extends Callback>> callbacks = new HashMap<>();

    @Getter
    private String name;
    @Getter
    private String authServiceId;
    private List<Interceptor> interceptors;

    private AuthServiceClient authServiceClient;

    //Cached the last 10 auth tree only.
    private static LruCache<String, AuthService> authServices = new LruCache<>(10);

    @Builder
    private AuthService(@NonNull String name,
                        ServerConfig serverConfig,
                        @Singular List<Interceptor> interceptors) {

        this.name = name;

        this.callbacks.putAll(CallbackFactory.getInstance().getCallbacks());

        authServiceId = UUID.randomUUID().toString();
        authServiceClient = new AuthServiceClient(serverConfig);
        this.interceptors = interceptors;
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
    static void goToNext(final Context context, final Node currentNode, final NodeListener listener) {
        final AuthService authService = authServices.get(currentNode.getAuthServiceId());
        if (authService == null) {
            Logger.warn(TAG, "Auth Service id: %s not found.", currentNode.getAuthServiceId());
            throw new IllegalStateException("AuthService Not Found!");
        }
        authService.authServiceClient.authenticate(currentNode,
                new AuthServiceResponseHandler(
                        authService,
                        new NodeInterceptorHandler(
                                context,
                                authService.interceptors,
                                listener, 0)
                        , authService.callbacks));

    }

    /**
     * Move on to the next node in the tree.
     *
     * @param context  The Application Context
     * @param listener Listener for receiving {@link AuthService} related changes
     */
    public void next(Context context, final NodeListener listener) {
        authServiceClient.authenticate(this,
                new AuthServiceResponseHandler(this,
                        new NodeInterceptorHandler(context, interceptors, listener, 0), callbacks));
    }

    /**
     * Move on to the next node in the tree with SSO Token, user has been authenticated
     *
     * @param context  The Application Context
     * @param listener Listener for receiving {@link AuthService} related changes
     */
    void next(Context context, Token token, final NodeListener listener) {
        new InterceptorHandler(
                context,
                interceptors,
                listener, 0).proceed(token);
    }

    void done() {
        Logger.debug(TAG, "Auth Service %s flow completed", authServiceId);
        authServices.remove(authServiceId);
    }

    public static class AuthServiceBuilder {

        public AuthService build() {

            List<Interceptor> interceptors;
            switch (this.interceptors == null ? 0 : this.interceptors.size()) {
                case 0:
                    interceptors = Collections.emptyList();
                    break;
                case 1:
                    interceptors = Collections.singletonList(this.interceptors.get(0));
                    break;
                default:
                    interceptors = Collections.unmodifiableList(new ArrayList<Interceptor>(this.interceptors));
            }

            AuthService authService = new AuthService(name, serverConfig, interceptors);
            authServices.put(authService.authServiceId, authService);
            return authService;

        }
    }

}
