/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.List;
import java.util.UUID;

import lombok.AccessLevel;
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
    private Long oauthCacheMillis;
    private Long oauthThreshold;
    private Long cookieCacheMillis;

    //Server
    @Getter(value = AccessLevel.NONE)
    private String identifier;

    private String url;
    private String realm;
    private int timeout;
    private List<String> pins;
    @Setter
    private List<BuildStep<OkHttpClient.Builder>> buildSteps;
    private CookieJar cookieJar;
    private String cookieName;

    @Getter(value = AccessLevel.NONE)
    private String authenticateEndpoint;
    @Getter(value = AccessLevel.NONE)
    private String authorizeEndpoint;
    @Getter(value = AccessLevel.NONE)
    private String tokenEndpoint;
    @Getter(value = AccessLevel.NONE)
    private String revokeEndpoint;
    @Getter(value = AccessLevel.NONE)
    private String userinfoEndpoint;
    @Getter(value = AccessLevel.NONE)
    private String sessionEndpoint;
    @Getter(value = AccessLevel.NONE)
    private String endSessionEndpoint;

    //service
    private String authServiceName;
    private String registrationServiceName;

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

    public String getUrl() {
        return this.url;
    }

     Config() {}

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

    /**
     * Load all the SDK configuration from the strings.xml
     *
     * @param appContext  The Application Context
     */
    public synchronized void init(Context appContext) {
        FROptions option = ConfigHelper.load(appContext, null);
        init(appContext, option);
    }

    /**
     * Load all the SDK configuration from the FROptions
     *
     * @param context  The Application Context
     * @param frOptions  FrOptions is nullable it loads the config from strings.xml, if not use the passed value.
     */
    public synchronized void init(Context context, @Nullable FROptions frOptions) {
        this.context = context.getApplicationContext();
        FROptions options = (frOptions == null) ? ConfigHelper.load(context, null) : frOptions;
        clientId = options.getOauth().getOauthClientId();
        redirectUri = options.getOauth().getOauthRedirectUri();
        scope = options.getOauth().getOauthScope();
        oauthCacheMillis = options.getOauth().getOauthCacheSeconds() * 1000;
        oauthThreshold = options.getOauth().getOauthThresholdSeconds();
        cookieJar = null; // We cannot initialize default cookie jar here
        url = options.getServer().getUrl();
        realm = options.getServer().getRealm();
        timeout = options.getServer().getTimeout();
        cookieName = options.getServer().getCookieName();
        cookieCacheMillis = options.getServer().getCookieCacheSeconds() * 1000;
        registrationServiceName = options.getService().getRegistrationServiceName();
        authServiceName = options.getService().getAuthServiceName();
        pins = options.getSslPinning().getPins();
        buildSteps = options.getSslPinning().getBuildSteps();
        authenticateEndpoint = options.getUrlPath().getAuthenticateEndpoint();
        authorizeEndpoint = options.getUrlPath().getAuthorizeEndpoint();
        tokenEndpoint = options.getUrlPath().getTokenEndpoint();
        revokeEndpoint = options.getUrlPath().getRevokeEndpoint();
        userinfoEndpoint = options.getUrlPath().getUserinfoEndpoint();
        sessionEndpoint = options.getUrlPath().getSessionEndpoint();
        if (StringUtils.isEmpty(sessionEndpoint)) {
            sessionEndpoint = context.getString(R.string.forgerock_logout_endpoint);
        }
        endSessionEndpoint = options.getUrlPath().getEndSessionEndpoint();
        identifier = UUID.randomUUID().toString();
        FRLogger customLogger = options.getLogger().getCustomLogger();
        if(customLogger != null) {
            Logger.setCustomLogger(customLogger);
        }
        Logger.Level logLevel = options.getLogger().getLogLevel();
        if(logLevel != null) {
            Logger.set(logLevel);
        }
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
                .sessionEndpoint(sessionEndpoint)
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
                .cacheIntervalMillis(oauthCacheMillis)
                .threshold(oauthThreshold)
                .build();
    }

    SingleSignOnManager getSingleSignOnManager() {
        return DefaultSingleSignOnManager.builder()
                .sharedPreferences(ssoSharedPreferences)
                .serverConfig(getServerConfig())
                .context(context)
                .ssoBroadcastModel(getSSOBroadcastModel())
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
                    .singleSignOnManager(getSingleSignOnManager())
                    .context(context).cacheIntervalMillis(cookieCacheMillis).build();
        }
        return cookieJar;
    }

    @VisibleForTesting
    void setSSOBroadcastModel(SSOBroadcastModel ssoModel) {
        this.ssoBroadcastModel = ssoModel;
    }


    SSOBroadcastModel getSSOBroadcastModel() {
        if (ssoBroadcastModel == null) {
            return ssoBroadcastModel = new SSOBroadcastModel(context);
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
