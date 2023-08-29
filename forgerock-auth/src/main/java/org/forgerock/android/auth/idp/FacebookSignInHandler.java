/*
 * Copyright (c) 2021 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.idp;

import android.os.Bundle;
import android.os.OperationCanceledException;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.InitProvider;
import org.forgerock.android.auth.Listener;

import java.util.List;

/**
 * {@link IdPHandler} to handle Facebook login
 */
public class FacebookSignInHandler extends Fragment implements IdPHandler {

    public static final String TAG = FacebookSignInHandler.class.getName();
    public FRListener<IdPResult> listener;
    private CallbackManager callbackManager;
    private IdPClient idPClient;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            idPClient = (IdPClient) getArguments().getSerializable(IDP_CLIENT);
        }

        LoginManager.getInstance().logOut();

        callbackManager = CallbackManager.Factory.create();

        // Callback registration
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Listener.onSuccess(listener, new IdPResult(loginResult.getAccessToken().getToken()));
            }

            @Override
            public void onCancel() {
                Listener.onException(listener, new OperationCanceledException());
            }

            @Override
            public void onError(FacebookException exception) {
                Listener.onException(listener, exception);
            }
        });
        LoginManager.getInstance().logInWithReadPermissions(this, callbackManager, getPermissions(idPClient));
    }

    @Override
    public String getTokenType() {
        return ACCESS_TOKEN;
    }

    @Override
    public void signIn(IdPClient idPClient, FRListener<IdPResult> listener) {
        FragmentManager fragmentManager = InitProvider.getCurrentActivityAsFragmentActivity().getSupportFragmentManager();
        signIn(fragmentManager, idPClient, listener);
    }

    @Override
    public void signIn(Fragment fragment, IdPClient idPClient, FRListener<IdPResult> listener) {
        signIn(fragment.getParentFragmentManager(), idPClient, listener);
    }

    private void signIn(FragmentManager fragmentManager, IdPClient idPClient, FRListener<IdPResult> listener) {
        FacebookSignInHandler existing = (FacebookSignInHandler) fragmentManager.findFragmentByTag(TAG);
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

    /**
     * The request permissions
     *
     * @return The Request permissions
     */
    protected List<String> getPermissions(IdPClient idPClient) {
        return idPClient.getScopes();
    }


}
