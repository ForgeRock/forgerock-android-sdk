/*
 * Copyright (c) 2019 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.VisibleForTesting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import okhttp3.CookieJar;
import okhttp3.OkHttpClient;

/**
 * Provide SDK Configuration, most components in the SDK has its default setting, this class allow developer to
 * override the default configuration.
 */
@Getter
public class Config {

    private static Config mInstance = new Config();
    private Context context;

    //OAuth2
    private String clientId;
    private String redirectUri;
    private String scope;
    private String oAuthUrl;

    //Server
    private String identifier;
    private String url;
    private String realm;
    private int timeout;
    private List<String> pins;
    @Setter
    private List<BuildStep<OkHttpClient.Builder>> buildSteps;
    private CookieJar cookieJar;
    private String cookieName;

    private String authenticateEndpoint;
    private String authorizeEndpoint;
    private String tokenEndpoint;
    private String revokeEndpoint;
    private String userinfoEndpoint;
    private String logoutEndpoint;
    private String endSessionEndpoint;

    //SSO Token Manager
    private Encryptor encryptor;

    private boolean initialized = false;


    private SharedPreferences ssoSharedPreferences;

    //Token Manager
    private SharedPreferences sharedPreferences;

    //KeyStoreManager
    private KeyStoreManager keyStoreManager;

    //BroadcastModel
    private SSOBroadcastModel ssoBroadcastModel;

    @VisibleForTesting
    public void setUrl(String url) {
        this.url = url;
    }

    private Config() {
    }

    @VisibleForTesting
    public void setEncryptor(Encryptor encryptor) {
        this.encryptor = encryptor;
    }

    //For testing to avoid using Android KeyStore Encryption
    @VisibleForTesting
    public void setSsoSharedPreferences(SharedPreferences ssoSharedPreferences) {
        this.ssoSharedPreferences = ssoSharedPreferences;
    }

    //For testing to avoid using Android KeyStore Encryption
    @VisibleForTesting
    public void setSharedPreferences(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public synchronized void init(Context context) {
        if (!initialized) {
            this.context = context.getApplicationContext();
            clientId = context.getString(R.string.forgerock_oauth_client_id);
            redirectUri = context.getString(R.string.forgerock_oauth_redirect_uri);
            scope = context.getString(R.string.forgerock_oauth_scope);
            oAuthUrl = context.getString(R.string.forgerock_oauth_url);
            url = context.getString(R.string.forgerock_url);
            realm = context.getString(R.string.forgerock_realm);
            timeout = context.getResources().getInteger(R.integer.forgerock_timeout);
            cookieJar = null; // We cannot initialize default cookie jar here
            cookieName = context.getString(R.string.forgerock_cookie_name);
            pins = Arrays.asList(context.getResources().getStringArray(R.array.forgerock_ssl_pinning_public_key_hashes));
            buildSteps = Collections.emptyList();
            authenticateEndpoint = context.getString(R.string.forgerock_authenticate_endpoint);
            authorizeEndpoint = context.getString(R.string.forgerock_authorize_endpoint);
            tokenEndpoint = context.getString(R.string.forgerock_token_endpoint);
            revokeEndpoint = context.getString(R.string.forgerock_revoke_endpoint);
            userinfoEndpoint = context.getString(R.string.forgerock_userinfo_endpoint);
            logoutEndpoint = context.getString(R.string.forgerock_logout_endpoint);
            endSessionEndpoint = context.getString(R.string.forgerock_endsession_endpoint);
            identifier = UUID.randomUUID().toString();
        }
        initialized = true;
    }

    public static Config getInstance() {
        return mInstance;
    }

    ServerConfig getServerConfig() {
        return ServerConfig.builder()
                .context(context)
                .identifier(identifier)
                .url(url)
                .realm(realm)
                .timeout(timeout)
                .cookieJarSupplier(this::getCookieJar)
                .cookieName(cookieName)
                .pins(pins)
                .buildSteps(buildSteps)
                .authenticateEndpoint(authenticateEndpoint)
                .authorizeEndpoint(authorizeEndpoint)
                .tokenEndpoint(tokenEndpoint)
                .revokeEndpoint(revokeEndpoint)
                .userInfoEndpoint(userinfoEndpoint)
                .logoutEndpoint(logoutEndpoint)
                .endSessionEndpoint(endSessionEndpoint)
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

    @VisibleForTesting
    TokenManager getTokenManager() {
        return DefaultTokenManager.builder()
                .context(context)
                .sharedPreferences(sharedPreferences)
                .oAuth2Client(getOAuth2Client())
                .build();
    }

    SingleSignOnManager getSingleSignOnManager() {
        return DefaultSingleSignOnManager.builder()
                .sharedPreferences(ssoSharedPreferences)
                .serverConfig(getServerConfig())
                .context(context)
                .encryptor(encryptor)
                .build();
    }

    /**
     * Retrieve the Session Manager that manage the user session.
     *
     * @return The SessionManager
     */
    public SessionManager getSessionManager() {
        return SessionManager.builder()
                .tokenManager(getTokenManager())
                .singleSignOnManager(getSingleSignOnManager())
                .build();

    }

    private CookieJar getCookieJar() {
        if (cookieJar == null) {
            cookieJar = SecureCookieJar.builder()
                    .context(context).build();
        }
        return cookieJar;
    }

    @VisibleForTesting
    void setSSOBroadcastModel(SSOBroadcastModel ssoModel) {
        this.ssoBroadcastModel = ssoModel;
    }


    SSOBroadcastModel getSSOBroadcastModel() {
        if (ssoBroadcastModel == null) {
            return ssoBroadcastModel = new SSOBroadcastModel();
        } else {
            return ssoBroadcastModel;
        }
    }

    @VisibleForTesting
    public void setKeyStoreManager(KeyStoreManager keyStoreManager) {
        this.keyStoreManager = keyStoreManager;
    }


    public KeyStoreManager getKeyStoreManager() {
        if (keyStoreManager == null) {
            return KeyStoreManager.builder().context(context).build();
        } else {
            return keyStoreManager;
        }
    }

    @VisibleForTesting
    void setCookieJar(CookieJar cookieJar) {
        this.cookieJar = cookieJar;
    }

    @VisibleForTesting
    public static void reset() {
        mInstance = new Config();
    }

}
