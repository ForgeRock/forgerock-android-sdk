/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.OperationCanceledException;

import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;

import org.forgerock.android.auth.exception.AuthenticationException;

import java.net.HttpURLConnection;

import static android.app.Activity.RESULT_CANCELED;

public class AppAuthFragment extends Fragment {

    private static final String TAG = AppAuthFragment.class.getName();
    private static final int AUTH_REQUEST_CODE = 100;

    private FRUser.Browser browser;

    public static AppAuthFragment init(FragmentManager fragmentManager, FRUser.Browser browser) {
        AppAuthFragment fragment = new AppAuthFragment();
        fragment.browser = browser;
        fragmentManager.beginTransaction().add(fragment, AppAuthFragment.TAG).commit();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OAuth2Client oAuth2Client = Config.getInstance().getOAuth2Client();
        AppAuthConfigurer configurer = browser.getAppAuthConfigurer();
        //Allow caller to override Authorization Service Configuration setting
        AuthorizationServiceConfiguration configuration = configurer.getAuthorizationServiceConfigurationSupplier().get();
        AuthorizationRequest.Builder authRequestBuilder = new AuthorizationRequest.Builder(configuration,
                oAuth2Client.getClientId(),
                oAuth2Client.getResponseType(),
                Uri.parse(oAuth2Client.getRedirectUri()))
                .setScope(oAuth2Client.getScope());

        //Allow caller to override Authorization Request setting
        configurer.getAuthorizationRequestBuilder().accept(authRequestBuilder);
        AuthorizationRequest authorizationRequest = authRequestBuilder.build();

        //Allow caller to override AppAuth default setting
        AppAuthConfiguration.Builder appAuthConfigurationBuilder = new AppAuthConfiguration.Builder();
        configurer.getAppAuthConfigurationBuilder().accept(appAuthConfigurationBuilder);
        AuthorizationService authorizationService = new AuthorizationService(getContext(), appAuthConfigurationBuilder.build());

        //Allow caller to override custom tabs default setting
        CustomTabsIntent.Builder intentBuilder =
                authorizationService.createCustomTabsIntentBuilder(authorizationRequest.toUri());
        configurer.getCustomTabsIntentBuilder().accept(intentBuilder);

        Intent intent = authorizationService.getAuthorizationRequestIntent(
                authorizationRequest,
                intentBuilder.build());

        startActivityForResult(intent, AUTH_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        getActivity().getSupportFragmentManager().beginTransaction().remove(this).commitNow();
        if (resultCode == RESULT_CANCELED) {
            Listener.onException(browser.getListener(), new OperationCanceledException());
        } else {
            if (requestCode == AUTH_REQUEST_CODE) {
                String error = data.getStringExtra(AuthorizationException.EXTRA_EXCEPTION);
                if (error != null) {
                    Listener.onException(browser.getListener(),
                            new AuthenticationException(HttpURLConnection.HTTP_UNAUTHORIZED, error,
                                    error));
                } else {
                    Listener.onSuccess(browser.getListener(), AuthorizationResponse.fromIntent(data));
                }
            } else {
                Listener.onException(browser.getListener(), new UnsupportedOperationException("Invalid result Code."));
            }
        }
        browser = null;
    }

}
