/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.VisibleForTesting;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

/**
 * Provide SDK Configuration, most components in the SDK has its default setting, this class allow developer to
 * override the default configuration.
 */
@Getter
@Setter
public class Config {

    private static Config mInstance;
    private Context context;

    //OAuth2
    private String clientId;
    private String redirectUri;
    private String scope;
    private String oAuthUrl;

    //Server
    private String url;
    private String realm;
    private int timeout;
    private List<String> pins;

    //SSO Token Manager
    private String accountName;
    private Encryptor encryptor;

    //Token Manager
    private SharedPreferences sharedPreferences;
    private long cacheIntervalMillis = 0L;
    private long threshold;

    //KeyStoreManager
    private KeyStoreManager keyStoreManager;

    private Config(Context context) {
        this.context = context.getApplicationContext();
        clientId = context.getString(R.string.forgerock_oauth_client_id);
        redirectUri = context.getString(R.string.forgerock_oauth_redirect_uri);
        scope = context.getString(R.string.forgerock_oauth_scope);
        oAuthUrl = context.getString(R.string.forgerock_oauth_url);
        threshold = context.getResources().getInteger(R.integer.forgerock_oauth_threshold);
        url = context.getString(R.string.forgerock_url);
        realm = context.getString(R.string.forgerock_realm);
        timeout = context.getResources().getInteger(R.integer.forgerock_timeout);
        accountName = context.getString(R.string.forgerock_account_name);
        pins = Arrays.asList(context.getResources().getStringArray(R.array.forgerock_pins));
    }

    public static Config getInstance(Context context) {
        if (mInstance == null) {
            synchronized (Config.class) {
                if (mInstance == null) {
                    mInstance = new Config(context);
                }
            }
        }
        return mInstance;
    }

    public static Config getInstance() {
        if (mInstance == null) {
            throw new IllegalStateException("Config is not initialized");
        }
        return mInstance;
    }

    ServerConfig getServerConfig() {
        return ServerConfig.builder()
                .context(context)
                .url(url)
                .realm(realm)
                .timeout(timeout)
                .build();
    }

    OAuth2Client getOAuth2Client() {
        return OAuth2Client.builder()
                .clientId(clientId)
                .scope(scope)
                .redirectUri(redirectUri)
                .serverConfig(getServerConfig())
                .build();
    }

    TokenManager getTokenManager() {
        return DefaultTokenManager.builder()
                .context(context)
                .sharedPreferences(sharedPreferences)
                .cacheIntervalMillis(cacheIntervalMillis)
                .oAuth2Client(getOAuth2Client())
                .threshold(threshold)
                .build();
    }

    SingleSignOnManager getSingleSignOnManager() {
        return DefaultSingleSignOnManager.builder()
                .context(context)
                .encryptor(encryptor)
                .build();
    }

    SessionManager getSessionManager() {
        return SessionManager.builder()
                .tokenManager(getTokenManager())
                .singleSignOnManager(getSingleSignOnManager())
                .oAuth2Client(getOAuth2Client())
                .build();

    }

    @VisibleForTesting
    public void setKeyStoreManager(KeyStoreManager keyStoreManager) {
        this.keyStoreManager = keyStoreManager;
    }


    @VisibleForTesting
    KeyStoreManager getKeyStoreManager() {
        if (keyStoreManager == null) {
            return KeyStoreManager.builder().context(context).build();
        } else {
            return keyStoreManager;
        }
    }

    List<String> applyDefaultIfNull(List<String> pins) {
        return applyIfNull(pins, null, (Function<Void, List<String>>) var -> getPins());
    }

    String applyDefaultIfNull(String realm) {
        return applyIfNull(realm, null, (Function<Void, String>) var -> getRealm());
    }

    Integer applyDefaultIfNull(Integer timeout) {
        return applyIfNull(timeout, null, (Function<Void, Integer>) var -> getTimeout());
    }


    Encryptor applyDefaultIfNull(Encryptor encryptor, final Context context, final Function<Context, Encryptor> function) {
        return applyIfNull(encryptor, null, (Function<Void, Encryptor>) var -> {
            Encryptor enc = getEncryptor();
            if (enc == null) {
                return function.apply(context);
            } else {
                return enc;
            }
        });
    }

    SharedPreferences applyDefaultIfNull(SharedPreferences sharedPreferences, final Context context, final Function<Context, SharedPreferences> function) {
        return applyIfNull(sharedPreferences, null, (Function<Void, SharedPreferences>) var -> {
            SharedPreferences s = getSharedPreferences();
            if (s == null) {
                return function.apply(context);
            } else {
                return s;
            }
        });
    }

    Long applyDefaultIfNull(Long cacheIntervalMillis) {
        return applyIfNull(cacheIntervalMillis, null, (Function<Void, Long>) var -> getCacheIntervalMillis());
    }

    Long applyDefaultThresholdIfNull(Long threshold) {
        return applyIfNull(threshold, null, (Function<Void, Long>) var -> getThreshold());
    }

    ServerConfig applyDefaultIfNull(ServerConfig serverConfig) {
        return applyIfNull(serverConfig, null, (Function<Void, ServerConfig>) var -> getServerConfig());
    }

    OAuth2Client applyDefaultIfNull(OAuth2Client oAuth2Client) {
        return applyIfNull(oAuth2Client, null, (Function<Void, OAuth2Client>) var -> getOAuth2Client());
    }

    TokenManager applyDefaultIfNull(TokenManager tokenManager) {
        return applyIfNull(tokenManager, null, (Function<Void, TokenManager>) var -> getTokenManager());
    }

    SingleSignOnManager applyDefaultIfNull(SingleSignOnManager singleSignOnManager) {
        return applyIfNull(singleSignOnManager, null, (Function<Void, SingleSignOnManager>) var -> getSingleSignOnManager());
    }

    SessionManager applyDefaultIfNull(SessionManager sessionManager) {
        return applyIfNull(sessionManager, null, (Function<Void, SessionManager>) var -> getSessionManager());
    }

    KeyStoreManager applyDefaultIfNull(KeyStoreManager keyStoreManager) {
        return applyIfNull(keyStoreManager, null, (Function<Void, KeyStoreManager>) var -> getKeyStoreManager());
    }


    private <T, R> R applyIfNull(R obj, T val, Function<T, R> func) {
        if (obj == null) {
            return func.apply(val);
        } else {
            return obj;
        }
    }

    public static void reset() {
        mInstance = null;
    }

}
