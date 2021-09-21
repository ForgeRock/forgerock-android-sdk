/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.idp;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.InitProvider;
import org.forgerock.android.auth.Listener;

/**
 * {@link IdPHandler} to handle Google login
 * @deprecated Use {@link GoogleIdentityServicesHandler}
 */
@Deprecated
public class GoogleSignInHandler extends Fragment implements IdPHandler {

    public static final int RC_SIGN_IN = 1000;
    public static final String ENABLE_SERVER_SIDE_ACCESS = "ENABLE_SERVER_SIDE_ACCESS";
    public static final String TAG = GoogleSignInHandler.class.getName();
    private FRListener<IdPResult> listener;
    private boolean enableServerSideAccess;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String clientId = null;
        if (getArguments() != null) {
            IdPClient idPClient = ((IdPClient) getArguments().getSerializable(IDP_CLIENT));
            clientId = idPClient.getClientId();
            enableServerSideAccess = getArguments().getBoolean(ENABLE_SERVER_SIDE_ACCESS);
        }
        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail();

        if (enableServerSideAccess) {
            builder.requestServerAuthCode(clientId);
        } else {
            builder.requestIdToken(clientId);
        }

        GoogleSignInOptions gso = builder.build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(getContext(), gso);
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Listener.onSuccess(listener, new IdPResult(account.getIdToken()));
            } catch (ApiException e) {
                Listener.onException(listener, e);
            }
        }
    }

    @Override
    public String getTokenType() {
        if (enableServerSideAccess) {
            return AUTHORIZATION_CODE;
        }
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
        GoogleSignInHandler existing = (GoogleSignInHandler) fragmentManager.findFragmentByTag(TAG);
        if (existing != null) {
            existing.listener = null;
            fragmentManager.beginTransaction().remove(existing).commitNow();
        }

        Bundle args = new Bundle();
        args.putSerializable(IDP_CLIENT, idPClient);
        args.putBoolean(ENABLE_SERVER_SIDE_ACCESS, false);
        setArguments(args);
        this.listener = listener;
        fragmentManager.beginTransaction().add(this, TAG)
                .commit();
    }

}
