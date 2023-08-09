/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import androidx.annotation.NonNull;

import org.forgerock.android.auth.exception.ApiException;
import org.forgerock.android.auth.exception.AuthenticationException;
import org.forgerock.android.auth.exception.AuthenticationTimeoutException;
import org.forgerock.android.auth.exception.SuspendedAuthSessionException;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;

import okhttp3.Response;

/**
 * Implementation for handling {@link AuthService} response, and provide feedback to the registered {@link NodeListener}
 */
class AuthServiceResponseHandler implements ResponseHandler {

    private static final String TAG = AuthServiceResponseHandler.class.getSimpleName();
    private static final String TOKEN_ID = "tokenId";
    public static final String SUSPENDED_AUTH_SESSION_EXCEPTION = "org.forgerock.openam.auth.nodes.framework.token.SuspendedAuthSessionException";
    private NodeListener<SSOToken> listener;
    private AuthService authService;

    /**
     * Constructs a new {@link AuthServiceResponseHandler}
     *
     * @param authService        The AuthService
     * @param listener           Listener for {@link AuthService} event.
     */
    AuthServiceResponseHandler(AuthService authService, @NonNull NodeListener<SSOToken> listener) {
        this.authService = authService;
        this.listener = listener;
    }

    /**
     * Handle {@link AuthService} APIs response and trigger registered {@link NodeListener}
     *
     * @param response The response from {@link AuthService}
     */
    void handleResponse(Response response) {
        try {
            if (response.isSuccessful()) {
                //Proceed to next Node in the tree
                JSONObject jsonObject = new JSONObject(response.body().string());
                if (jsonObject.has(Node.AUTH_ID)) {
                    Logger.debug(TAG, "Journey callback(s) received.");
                    if (listener != null) {
                        Node node = listener.onCallbackReceived(authService.getAuthServiceId(), jsonObject);
                        listener.onCallbackReceived(node);
                    }
                } else {
                    //The Auth Tree is consider finished if auth id not from the response
                    authService.done();
                    Logger.debug(TAG, "Journey finished with Success outcome.");
                    if (jsonObject.has(TOKEN_ID)) {
                        Logger.debug(TAG, "SSO Token received.");
                        Listener.onSuccess(listener, new SSOToken(jsonObject.getString(TOKEN_ID)));
                    } else {
                        Listener.onSuccess(listener, null);
                    }
                }
            } else {
                handleError(response, listener);

            }
        } catch (Exception e) {
            handleError(e);
        }
    }

    @Override
    public void handleError(Response response, FRListener<?> listener) {
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
                switch (getError(responseBody)) {
                    case "110":
                        authService.done();
                        handleError(new AuthenticationTimeoutException(response.code(), response.message(), body));
                        return;
                    case SUSPENDED_AUTH_SESSION_EXCEPTION:
                        authService.done();
                        handleError(new SuspendedAuthSessionException(response.code(), response.message(), body));
                        return;
                    default:
                        handleError(new AuthenticationException(response.code(), response.message(), body));
                        return;
                }
            default:
                handleError(new ApiException(response.code(), response.message(), getBody(response)));
        }
    }


    private String getError(JSONObject body) {
        JSONObject detail = body.optJSONObject("detail");
        if (detail != null) {
            return detail.optString("errorCode", "-1");
        }
        String message = body.optString("message","");
        if (message.contains(SUSPENDED_AUTH_SESSION_EXCEPTION)) {
            return SUSPENDED_AUTH_SESSION_EXCEPTION;
        }
        return "-1";

    }

    void handleError(Exception e) {
        Logger.debug(TAG, "Journey finished with failed result %s", e.getMessage());
        Listener.onException(listener, e);
    }

}

