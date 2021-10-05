/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.idp;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.OperationCanceledException;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.InitProvider;
import org.forgerock.android.auth.Listener;
import org.forgerock.android.auth.StringUtils;

public class GoogleIdentityServicesHandler extends Fragment implements IdPHandler {

    public static final String TAG = GoogleIdentityServicesHandler.class.getName();
    public static final int RC_SIGN_IN = 1000;
    public FRListener<IdPResult> listener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IdPClient idPClient = null;
        if (getArguments() != null) {
            idPClient = ((IdPClient) getArguments().getSerializable(IDP_CLIENT));
            if (idPClient == null) {
                listener.onException(new IllegalArgumentException("IDP Client is missing from Argument"));
                return;
            }
        }

        SignInClient oneTapClient = Identity.getSignInClient(getContext());

        BeginSignInRequest.GoogleIdTokenRequestOptions.Builder builder = BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                .setSupported(true)
                // Your server's client ID, not your Android client ID.
                .setServerClientId(idPClient.getClientId())
                .setFilterByAuthorizedAccounts(false);

        if (!StringUtils.isEmpty(idPClient.getNonce())) {
            builder.setNonce(idPClient.getNonce());
        }

        BeginSignInRequest signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(builder.build())
                .build();

        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(result -> {
                    try {
                        startIntentSenderForResult(
                                result.getPendingIntent().getIntentSender(), RC_SIGN_IN,
                                null, 0, 0, 0, null);
                    } catch (IntentSender.SendIntentException e) {
                        Listener.onException(listener, e);
                    }
                })
                .addOnFailureListener(e -> {
                    Listener.onException(listener, e);
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == RC_SIGN_IN) {
                SignInCredential credential = null;
                try {
                    credential = Identity.getSignInClient(getContext()).getSignInCredentialFromIntent(data);
                    String idToken = credential.getGoogleIdToken();
                    listener.onSuccess(new IdPResult(idToken));
                } catch (ApiException e) {
                    Listener.onException(listener, e);
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            try {
                if (data != null) {
                    //something wrong, try to get the error reason
                    Identity.getSignInClient(getContext()).getSignInCredentialFromIntent(data);
                }
                Listener.onException(listener, new OperationCanceledException());
            } catch (ApiException e) {
                Listener.onException(listener, e);
            }
        }

    }

    @Override
    public String getTokenType() {
        return ID_TOKEN;
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
        GoogleIdentityServicesHandler existing = (GoogleIdentityServicesHandler) fragmentManager.findFragmentByTag(TAG);
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
