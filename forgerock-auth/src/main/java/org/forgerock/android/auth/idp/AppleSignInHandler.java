/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.idp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.InitProvider;
import org.forgerock.android.auth.Listener;
import org.forgerock.android.auth.StringUtils;
import org.forgerock.android.auth.exception.BrowserAuthenticationException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static net.openid.appauth.AuthorizationException.EXTRA_EXCEPTION;

/**
 * {@link IdPHandler} to handle Apple login
 */
public class AppleSignInHandler extends Fragment implements IdPHandler {

    public static final int RC_SIGN_IN = 1000;
    public static final String TAG = AppleSignInHandler.class.getName();
    public static final String FORM_POST_ENTRY = "form_post_entry";
    public static final String AUTHORIZE_ENDPOINT = "https://appleid.apple.com/auth/authorize";
    public static final String TOKEN_ENDPOINT = "https://appleid.apple.com/auth/token";
    public static final String FORM_POST = "form_post";
    public static final String CODE = "code";
    private FRListener<IdPResult> listener;
    private IdPClient idPClient;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            idPClient = (IdPClient) getArguments().getSerializable(IDP_CLIENT);
        }
        AuthorizationServiceConfiguration configuration = new AuthorizationServiceConfiguration(
                Uri.parse(AUTHORIZE_ENDPOINT),
                Uri.parse(TOKEN_ENDPOINT));


        AuthorizationRequest.Builder authRequestBuilder = new AuthorizationRequest.Builder(configuration,
                idPClient.getClientId(),
                CODE,
                Uri.parse(idPClient.getRedirectUri()))
                .setScopes(idPClient.getScopes())
                .setState(null)
                .setResponseMode(FORM_POST);

        authRequestBuilder.setNonce(idPClient.getNonce());

        AuthorizationRequest authorizationRequest = authRequestBuilder
                .build();
        AppAuthConfiguration.Builder appAuthConfigurationBuilder = new AppAuthConfiguration.Builder();
        AuthorizationService authorizationService = new AuthorizationService(getContext(),
                appAuthConfigurationBuilder.build());

        CustomTabsIntent.Builder intentBuilder = getIntentBuilder(authorizationService, authorizationRequest.toUri());

        Intent intent = authorizationService.getAuthorizationRequestIntent(
                authorizationRequest, intentBuilder.build());
        startActivityForResult(intent, RC_SIGN_IN);
    }

    /**
     * Retrieve the Custom tab builder.
     *
     * @param service The authorization service that handle request to an OAuth2 authorization service
     * @param uri The uri to launch
     * @return The Custom T
     */
    public CustomTabsIntent.Builder getIntentBuilder(@NonNull AuthorizationService service, @NonNull Uri uri) {
        return service.createCustomTabsIntentBuilder(uri);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            if (data != null) {
                String error = data.getStringExtra(EXTRA_EXCEPTION);
                if (error != null) {
                    Listener.onException(listener,
                            new BrowserAuthenticationException(error));
                } else {
                    //Double redirect happened, the uri should be the uri that configured
                    //with Redirect after form post URL, a parameter form_post_entry is encrypted
                    //with the authorization code.
                    Uri uri = data.getData();
                    Map<String, String> additionalParameters = new HashMap<>();
                    if (uri != null && uri.getQueryParameterNames() != null) {
                        for (String key : uri.getQueryParameterNames()) {
                            additionalParameters.put(key, uri.getQueryParameter(key));
                        }
                    }
                    Listener.onSuccess(listener, new IdPResult(FORM_POST_ENTRY, additionalParameters));
                }
            } else {
                //Not expected
                Listener.onException(listener, new BrowserAuthenticationException("No response data"));
            }
        }

    }

    @Override
    public String getTokenType() {
        return AUTHORIZATION_CODE;
    }

    @Override
    public void signIn(IdPClient idPClient, FRListener<IdPResult> listener) {
        FragmentManager fragmentManager = InitProvider.getCurrentActivityAsFragmentActivity().getSupportFragmentManager();
        signIn(fragmentManager, idPClient, listener);
    }

    @Override
    public void signIn(Fragment fragment, IdPClient idPClient, FRListener<IdPResult> listener) {
        signIn(fragment.getFragmentManager(), idPClient, listener);
    }

    private void signIn(FragmentManager fragmentManager, IdPClient idPClient, FRListener<IdPResult> listener) {

        if (StringUtils.isEmpty(idPClient.getNonce())) {
            //For Apple signin, since PKCE is not supported, we use nonce to prevent Authentication Code Injection
            //https://tools.ietf.org/html/draft-ietf-oauth-security-topics-18#section-4.5)
            listener.onException(new IllegalArgumentException("Enable Native Nonce is required."));
            return;
        }

        AppleSignInHandler existing = (AppleSignInHandler) fragmentManager.findFragmentByTag(TAG);
        if (existing != null) {
            existing.listener = null;
            fragmentManager.beginTransaction().remove(existing).commitNow();
        }

        Bundle args = new Bundle();
        args.putSerializable(IDP_CLIENT, idPClient);
        setArguments(args);
        this.listener = listener;
        fragmentManager.beginTransaction().add(this, TAG)
                .commit();
    }

}
