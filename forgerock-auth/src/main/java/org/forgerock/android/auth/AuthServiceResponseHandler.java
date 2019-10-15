/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import lombok.NonNull;
import okhttp3.Response;
import org.forgerock.android.auth.callback.Callback;
import org.forgerock.android.auth.exception.ApiException;
import org.forgerock.android.auth.exception.AuthenticationException;
import org.forgerock.android.auth.exception.AuthenticationTimeoutException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.util.*;

/**
 * Implementation for handling {@link AuthService} response, and provide feedback to the registered {@link NodeListener}
 */
class AuthServiceResponseHandler implements ResponseHandler {

    private static final String AUTH_ID = "authId";
    private static final String STAGE = "stage";
    private static final String TOKEN_ID = "tokenId";
    private NodeListener<SSOToken> listener;
    private AuthService authService;
    private Map<String, Class<? extends Callback>> supportedCallbacks;

    /**
     * Constructs a new {@link AuthServiceResponseHandler}
     *
     * @param authService The AuthService
     * @param listener           Listener for {@link AuthService} event.
     * @param supportedCallbacks Supported {@link Callback} Types.
     */
    AuthServiceResponseHandler(AuthService authService, @NonNull NodeListener<SSOToken> listener, Map<String, Class<? extends Callback>> supportedCallbacks) {
        this.authService = authService;
        this.listener = listener;
        this.supportedCallbacks = supportedCallbacks;
    }

    /**
     * Handle {@link AuthService} APIs response and trigger registered {@link NodeListener}
     *
     * @param response   The response from {@link AuthService}
     */
    void handleResponse(Response response) {
        try {
            if (response.isSuccessful()) {
                //Proceed to next Node in the tree
                JSONObject jsonObject = new JSONObject(Objects.requireNonNull(response.body()).string());
                if (jsonObject.has(AUTH_ID)) {
                    if (listener != null) {
                        listener.onCallbackReceived(parseState(jsonObject));
                    }
                    return;
                }
                //The Auth Tree is consider finished after SSO Token is received
                if (jsonObject.has(TOKEN_ID)) {
                    authService.done();
                    Listener.onSuccess(listener, new SSOToken(jsonObject.getString(TOKEN_ID)));
                    return;
                }
                handleError(new UnsupportedOperationException("Unknown response content"));
            } else {
                handleError(response, listener);
            }
        } catch (Exception e) {
            handleError(e);
        }
    }

    @Override
    public void handleError(Response response, FRListener listener) {
        switch (response.code()) {
            case HttpURLConnection.HTTP_UNAUTHORIZED:
                String body = getBody(response);
                JSONObject responseBody = null;
                try {
                    responseBody = new JSONObject(body);
                } catch (JSONException e) {
                    //should not happened
                    handleError(new AuthenticationException(response.code(), response.message(), body));
                    return;
                }
                switch (getErrorCode(responseBody)) {
                    case "110":
                        authService.done();
                        handleError(new AuthenticationTimeoutException(response.code(), response.message(), body));
                        return;
                    default:
                        handleError(new AuthenticationException(response.code(), response.message(), body));
                        return;
                }
            default:
                handleError(new ApiException(response.code(), response.message(), getBody(response)));
        }
    }



    private String getErrorCode(JSONObject body) {
        JSONObject detail = body.optJSONObject("detail");
        if (detail != null) {
            return detail.optString("errorCode", "-1");
        }
        return "-1";
    }

    void handleError(Exception e) {
        Listener.onException(listener, e);
    }

    private Node parseState(JSONObject jsonObject) throws JSONException, UnsupportedCallbackException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        return new Node(jsonObject.getString(AUTH_ID)
                , jsonObject.optString(STAGE, null)
                , authService.getAuthServiceId(),
                parseCallback(jsonObject.getJSONArray("callbacks")));
    }

    private List<Callback> parseCallback(JSONArray jsonArray) throws JSONException, UnsupportedCallbackException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        List<Callback> callbacks = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject cb = jsonArray.getJSONObject(i);
            Class<? extends Callback> clazz = supportedCallbacks.get(cb.getString("type"));
            if (clazz == null) {
                throw new UnsupportedCallbackException(null, "Callback Type Not Supported: " + cb.getString("type"));
            }
            callbacks.add(newInstance(clazz, cb, i));
        }
        return callbacks;
    }

    private Callback newInstance(Class<? extends Callback> callbackClass, JSONObject jsonObject, int index) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return callbackClass.getConstructor(JSONObject.class, int.class).newInstance(jsonObject, index);

    }
}

