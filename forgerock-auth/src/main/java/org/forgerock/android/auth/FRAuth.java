/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;

import static android.content.Context.MODE_PRIVATE;

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

    //Alias to store Previous Configure Host
    public static final String ORG_FORGEROCK_V_1_HOSTS = "org.forgerock.v1.HOSTS";
    private static boolean started;

    public static void start(Context context) {
        if (!started) {
            started = true;
            Config config = Config.getInstance(context);
            //Clean up when server switch
            SharedPreferences sharedPreferences = context.getSharedPreferences(ORG_FORGEROCK_V_1_HOSTS, MODE_PRIVATE);
            String previousHost = sharedPreferences.getString("url", null);
            if (previousHost != null) {
                if (!config.getUrl().equals(previousHost)) {
                    SessionManager.builder().build().close();
                }
            }
            sharedPreferences.edit().putString("url", config.getUrl()).apply();
        }
    }

    /**
     * @deprecated As of release 1.1, replaced by {@link FRSession#authenticate(Context, String, NodeListener)} ()}
     */
    @Deprecated
    @Builder
    public FRAuth(@NonNull Context context,
                   String serviceName,
                   ServerConfig serverConfig,
                   SessionManager sessionManager,
                   @Singular List<Interceptor> interceptors) {

        Config config = Config.getInstance(context);

        this.sessionManager = config.applyDefaultIfNull(sessionManager);

        AuthService.AuthServiceBuilder builder = AuthService.builder()
                .name(serviceName)
                .serverConfig(config.applyDefaultIfNull(serverConfig))
                .interceptor(new SingleSignOnInterceptor(this.sessionManager));

        for (Interceptor interceptor : interceptors) {
            builder.interceptor(interceptor);
        }

        authService = builder.build();
    }

    static FRAuthBuilder builder() {
        return new FRAuthBuilder();
    }

    /**
     * Discard the existing user session and Move on to the next node in the auth process.
     *
     * @param context  The Application Context
     * @param listener Listener for receiving {@link FRAuth} related changes
     * @deprecated  As of release 1.1, replaced by {@link FRSession#authenticate(Context, String, NodeListener)} ()}
     */
    @Deprecated
    public void next(final Context context, final NodeListener listener) {
        authService.next(context, listener);
    }

    static class FRAuthBuilder {

        public FRAuth build() {
            List<Interceptor> interceptors;
            switch (this.interceptors == null ? 0 : this.interceptors.size()) {
                case 0:
                    interceptors = java.util.Collections.emptyList();
                    break;
                case 1:
                    interceptors = java.util.Collections.singletonList(this.interceptors.get(0));
                    break;
                default:
                    interceptors = java.util.Collections.unmodifiableList(new ArrayList<Interceptor>(this.interceptors));
            }

            return new FRAuth(context, serviceName, serverConfig, sessionManager, interceptors);
        }
    }
}
