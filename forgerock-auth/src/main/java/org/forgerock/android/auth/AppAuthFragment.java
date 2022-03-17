/*
 * Copyright (c) 2020 -2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static net.openid.appauth.AuthorizationException.EXTRA_EXCEPTION;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;

import org.forgerock.android.auth.exception.BrowserAuthenticationException;

/**
 * Headless Fragment to receive callback result from AppAuth library
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AppAuthFragment extends Fragment {

    static final String TAG = AppAuthFragment.class.getName();
    static final int AUTH_REQUEST_CODE = 100;

    private FRUser.Browser browser;
    private AuthorizationService authorizationService;

    /**
     * Initialize the Fragment to receive AppAuth callback event.
     */
    static void launch(FragmentManager fragmentManager, FRUser.Browser browser) {
        AppAuthFragment existing = (AppAuthFragment) fragmentManager.findFragmentByTag(TAG);
        if (existing != null) {
            existing.browser = null;
            fragmentManager.beginTransaction().remove(existing).commitNow();
        }

        AppAuthFragment fragment = new AppAuthFragment();
        fragment.browser = browser;
        fragmentManager.beginTransaction().add(fragment, AppAuthFragment.TAG).commit();
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
        authorizationService = new AuthorizationService(getContext(),
                appAuthConfigurationBuilder.build());

        //Allow caller to override custom tabs default setting
        CustomTabsIntent.Builder intentBuilder =
                authorizationService.createCustomTabsIntentBuilder(authorizationRequest.toUri());
        configurer.getCustomTabsIntentBuilder().accept(intentBuilder);

        try {
            Intent intent = authorizationService.getAuthorizationRequestIntent(
                    authorizationRequest, intentBuilder.build());
            startActivityForResult(intent, AUTH_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            if (browser.isFailedOnNoBrowserFound()) {
                throw e;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction().remove(this).commitNow();
        }
        if (data != null) {
            String error = data.getStringExtra(EXTRA_EXCEPTION);
            if (error != null) {
                Listener.onException(browser.getListener(),
                        new BrowserAuthenticationException(error));
            } else {
                Listener.onSuccess(browser.getListener(), AuthorizationResponse.fromIntent(data));
            }
        } else {
            //Not expected
            Listener.onException(browser.getListener(), new BrowserAuthenticationException("No response data"));
        }
        browser = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (authorizationService != null) {
            authorizationService.dispose();
        }
    }
}
