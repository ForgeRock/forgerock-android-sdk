/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Singular;

/**
 * Model of an FRAuth.
 * <p>
 * Dispatches requests to {@link AuthService} and {@link OAuth2Client}, performs user authentication and manage
 * user session.
 *
 * <p>
 * To create a new FRAuth object use the static {@link FRAuth#builder()}.
 */
public class FRAuth {

    private AuthService authService;
    private SessionManager sessionManager;

    private static boolean started;

    private static FROptions cachedOptions;

    /**
     * Start the SDK
     *
     * @param context  The Application Context
     * @param options  The FROptions is a nullable field which takes either a null or config. If the caller passes null it fetches the default values from strings.xml .
     */
    public static synchronized void start(Context context, @Nullable FROptions options) {
        if(!started || !FROptions.equals(cachedOptions, options)) {
            started = true;
            FROptions currentOptions = ConfigHelper.load(context, options);
            //Validate (AM URL, Realm, CookieName) is not Empty. If its empty will throw IllegalArgumentException.
            currentOptions.validateConfig();
            if (ConfigHelper.isConfigDifferentFromPersistedValue(context, currentOptions)) {
               SessionManager sessionManager = ConfigHelper.getPersistedConfig(context, cachedOptions).getSessionManager();
               sessionManager.close();
            }
            Config.getInstance().init(context, currentOptions);
            ConfigHelper.persist(context, currentOptions);
            cachedOptions = options;
        }
    }

    public static void start(Context context) {
       start(context, null);
    }

    @Builder
    private FRAuth(@NonNull Context context,
                   String serviceName,
                   PolicyAdvice advice,
                   Uri resumeURI,
                   ServerConfig serverConfig,
                   SessionManager sessionManager,
                   @Singular List<Interceptor<?>> interceptors) {

        this.sessionManager = sessionManager == null ? Config.getInstance().getSessionManager() : sessionManager;

        AuthService.AuthServiceBuilder builder = AuthService.builder()
                .name(serviceName)
                .advice(advice)
                .resumeURI(resumeURI)
                .serverConfig(serverConfig == null ? Config.getInstance().getServerConfig() : serverConfig)
                .interceptor(new SingleSignOnInterceptor(
                        this.sessionManager));

        for (Interceptor<?> interceptor : interceptors) {
            builder.interceptor(interceptor);
        }

        authService = builder.build();
    }

    static FRAuthBuilder builder() {
        return new FRAuthBuilder();
    }

    /**
     * Trigger the Authentication Tree flow process.
     *
     * @param context  The Application Context
     * @param listener Listener for receiving {@link FRAuth} related changes
     */
    void next(final Context context, final NodeListener<?> listener) {
        authService.next(context, listener);
    }

    static class FRAuthBuilder {

        public FRAuth build() {
            List<Interceptor<?>> interceptors;
            switch (this.interceptors == null ? 0 : this.interceptors.size()) {
                case 0:
                    interceptors = java.util.Collections.emptyList();
                    break;
                case 1:
                    interceptors = java.util.Collections.singletonList(this.interceptors.get(0));
                    break;
                default:
                    interceptors = java.util.Collections.unmodifiableList(new ArrayList<>(this.interceptors));
            }

            return new FRAuth(context, serviceName, advice, resumeURI, serverConfig, sessionManager, interceptors);
        }
    }
}