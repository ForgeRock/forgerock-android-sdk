/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import androidx.annotation.Keep;
import androidx.fragment.app.Fragment;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Listener;
import org.forgerock.android.auth.idp.FacebookSignInHandler;
import org.forgerock.android.auth.idp.GoogleSignInHandler;
import org.forgerock.android.auth.idp.IdPClient;
import org.forgerock.android.auth.idp.IdPHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Callback to handle Identity Provider SignIn
 */
@NoArgsConstructor
@Getter
public class IdPCallback extends AbstractCallback implements IdPClient {

    private String provider;
    private String clientId;
    private String redirectUri;
    private List<String> scopes;
    private String nonce;
    private List<String> acrValues;
    private String request;
    private String requestUri;

    @Keep
    public IdPCallback(JSONObject jsonObject, int index) {
        super(jsonObject, index);
    }

    @Override
    protected void setAttribute(String name, Object value) {
        switch (name) {
            case "provider":
                this.provider = (String) value;
                break;
            case "clientId":
                this.clientId = (String) value;
                break;
            case "redirectUri":
                this.redirectUri = (String) value;
                break;
            case "scopes":
                JSONArray array = (JSONArray) value;
                scopes = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    try {
                        scopes.add(array.getString(i));
                    } catch (JSONException e) {
                        //ignore
                    }
                }
                break;
            case "nonce":
                this.nonce = (String) value;
                break;
            case "acrValues":
                JSONArray values = (JSONArray) value;
                acrValues = new ArrayList<>();
                for (int i = 0; i < values.length(); i++) {
                    try {
                        acrValues.add(values.getString(i));
                    } catch (JSONException e) {
                        //ignore
                    }
                }
                break;
            case "request":
                this.request = (String) value;
                break;
            case "requestUri":
                this.requestUri = (String) value;
                break;

            default:
                //ignore
        }
    }

    /**
     * Set the the authentication token value.
     *
     * @param value The authentication token value.
     */
    public void setToken(String value) {
        super.setValue(value, 0);
    }

    /**
     * Set the Token Type (access_token, id_token, authorization_code)
     *
     * @param value The Token Type
     */
    public void setTokenType(String value) {
        super.setValue(value, 1);
    }

    @Override
    public String getType() {
        return "IdPCallback";
    }

    /**
     * Perform the Identity Provider sign in with the current active Fragment
     *
     * @param fragment The Active Fragment
     * @param listener Listener to listen for the result.
     */
    public void signIn(Fragment fragment, FRListener<Void> listener) {
        IdPHandler idPHandler = getIdPHandler();
        idPHandler.signIn(fragment, this, getResultListener(idPHandler, listener));
    }

    /**
     * Perform the Identity Provider sign in with the current active
     * {@link androidx.fragment.app.FragmentActivity}
     *
     * @param listener Listener to listen for the result.
     */
    public void signIn(FRListener<Void> listener) {
        IdPHandler idPHandler = getIdPHandler();
        idPHandler.signIn(this, getResultListener(idPHandler, listener));
    }


    private FRListener<String> getResultListener(IdPHandler idPHandler, FRListener<Void> listener) {
        return new FRListener<String>() {
            @Override
            public void onSuccess(String result) {
                setToken(result);
                setTokenType(idPHandler.getTokenType());
                Listener.onSuccess(listener, null);
            }

            @Override
            public void onException(Exception e) {
                Listener.onException(listener, e);
            }
        };
    }

    /**
     * Get the {@link IdPHandler} that handle the Identity Provider SignIn
     *
     * @return The handler to handle the sign-in.
     */
    protected IdPHandler getIdPHandler() {
        IdPHandler idPHandler = null;
        if (provider.toLowerCase().contains("google")) {
            idPHandler = new GoogleSignInHandler();
        } else if (provider.toLowerCase().contains("facebook")) {
            idPHandler = new FacebookSignInHandler();
        }
        return idPHandler;
    }


}

