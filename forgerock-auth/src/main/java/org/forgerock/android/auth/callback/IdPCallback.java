/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Listener;
import org.forgerock.android.auth.idp.AppleSignInHandler;
import org.forgerock.android.auth.idp.FacebookSignInHandler;
import org.forgerock.android.auth.idp.GoogleIdentityServicesHandler;
import org.forgerock.android.auth.idp.IdPClient;
import org.forgerock.android.auth.idp.IdPHandler;
import org.forgerock.android.auth.idp.IdPResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Callback to handle Identity Provider SignIn
 */
@Getter
public class IdPCallback extends AbstractCallback implements IdPClient, AdditionalParameterCallback {

    private String provider;
    private String clientId;
    private String redirectUri;
    private List<String> scopes;
    private String nonce;
    private List<String> acrValues;
    private String request;
    private String requestUri;
    private final Map<String, String> additionalParameters = new HashMap<>();

    @Keep
    public IdPCallback() {
    }

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
     * @param fragment   The Active Fragment
     * @param idPHandler Optional {@link IdPHandler} to perform sign in, if not provided,
     *                   SDK automatically selects the default implementation
     * @param listener   Listener to listen for the result.
     */
    public void signIn(Fragment fragment, @Nullable IdPHandler idPHandler, FRListener<Void> listener) {
        if (idPHandler == null) {
            idPHandler = getIdPHandler();
            if (idPHandler == null) {
                Listener.onException(listener, new UnsupportedOperationException("Unsupported provider: " + provider));
                return;
            }
        }
        idPHandler.signIn(fragment, this, getResultListener(idPHandler, listener));
    }

    /**
     * Perform the Identity Provider sign in with the current active
     * {@link androidx.fragment.app.FragmentActivity}
     *
     * @param idPHandler Optional {@link IdPHandler} to perform sign in, if not provided,
     *                   SDK automatically selects the default implementation
     * @param listener   Listener to listen for the result.
     */
    public void signIn(@Nullable IdPHandler idPHandler, FRListener<Void> listener) {
        if (idPHandler == null) {
            idPHandler = getIdPHandler();
            if (idPHandler == null) {
                Listener.onException(listener, new UnsupportedOperationException("Unsupported provider: " + provider));
                return;
            }
        }
        idPHandler.signIn(this, getResultListener(idPHandler, listener));
    }


    private FRListener<IdPResult> getResultListener(IdPHandler idPHandler, FRListener<Void> listener) {
        return new FRListener<IdPResult>() {
            @Override
            public void onSuccess(IdPResult result) {
                if (result.getAdditionalParameters() != null) {
                    additionalParameters.putAll(result.getAdditionalParameters());
                }
                setTokenType(idPHandler.getTokenType());
                setToken(result.getToken());
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
            idPHandler = new GoogleIdentityServicesHandler();
        } else if (provider.toLowerCase().contains("facebook")) {
            idPHandler = new FacebookSignInHandler();
        } else if (provider.toLowerCase().contains("apple")) {
            idPHandler = new AppleSignInHandler();
        }
        return idPHandler;
    }


    @Override
    public Map<String, String> getAdditionalParameters() {
        return additionalParameters;
    }
}

