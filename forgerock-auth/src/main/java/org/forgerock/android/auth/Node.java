/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import org.forgerock.android.auth.callback.Callback;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public class Node implements Serializable {

    public static final String AUTH_ID = "authId";
    public static final String STAGE = "stage";
    public static final String HEADER = "header";
    public static final String DESCRIPTION = "description";


    private final String authId;
    private final String stage;
    private final String header;
    private final String description;
    private final String authServiceId;
    private final List<Callback> callbacks;

    @VisibleForTesting
    public Node(String authId, String stage, String header, String description, String authServiceId, List<Callback> callbacks) {
        this.authId = authId;
        this.stage = stage;
        this.header = header;
        this.description = description;
        this.authServiceId = authServiceId;
        this.callbacks = callbacks;
    }

    /**
     * Returns {@link JSONObject} mapping of the object
     *
     * @return {@link JSONObject} mapping of the object
     * @throws JSONException Failed to map to {@link JSONObject}
     */
    JSONObject toJsonObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(AUTH_ID, authId);
        if (stage != null) {
            jsonObject.put(STAGE, stage);
        }
        JSONArray array = new JSONArray();
        for (Callback cb : callbacks) {
            array.put(new JSONObject(cb.getContent()));
        }
        jsonObject.put("callbacks", array);
        return jsonObject;
    }

    /**
     * Find the first match Callback with the provided Class.
     * Returns null if not found.
     *
     * @param clazz The Callback Class
     * @param <T>   The Callback Type
     * @return The Callback instance
     */
    public <T> T getCallback(Class<T> clazz) {
        for (Callback callback : callbacks) {
            if (callback.getClass().equals(clazz)) {
                //found the first match.
                return (T) callback;
            }
        }
        return null;
    }

    /**
     * Retrieve all the {@link Callback}.
     *
     * @return All the {@link Callback} associate with this {@link Node}
     */
    public List<Callback> getCallbacks() {
        return callbacks;
    }

    /**
     * Move on to the next node in the tree.
     *
     * @param context  The Application Context
     * @param listener Listener for receiving {@link AuthService} related changes
     *                 <b> {@link NodeListener#onSuccess(Object)} on success login.
     *                 <b> {@link NodeListener#onCallbackReceived(Node)} step to the next node, {@link Node} is returned.
     *                 <b> throws {@link IllegalStateException} when the tree is invalid, e.g the authentication tree has been completed.
     *                 <b> throws {@link org.forgerock.android.auth.exception.AuthenticationException} when server returns {@link java.net.HttpURLConnection#HTTP_UNAUTHORIZED}
     *                 <b> throws {@link org.forgerock.android.auth.exception.ApiException} When server return errors.
     *                 <b> throws {@link javax.security.auth.callback.UnsupportedCallbackException}
     *                 When {@link org.forgerock.android.auth.callback.Callback} returned from Server is not supported by the SDK.
     *                 <b> throws {@link org.forgerock.android.auth.exception.SuspendedAuthSessionException} When Suspended ID timeout
     *                 <b> throws {@link org.forgerock.android.auth.exception.AuthenticationTimeoutException} When Authentication tree timeout
     *                 <b> throws {@link org.json.JSONException} when failed to parse server response as JSON String.
     *                 <b> throws {@link IOException } When there is any network error.
     *                 <b> throws {@link java.net.MalformedURLException} When failed to parse the URL for API request.
     *                 <b> throws {@link NoSuchMethodException} or {@link SecurityException} When failed to initialize the Callback class.
     */
    public void next(Context context, NodeListener<?> listener) {
        AuthService.goToNext(context, this, listener);
    }

    /**
     * Sets the {@link Callback} object for the {@link AuthService} node.
     *
     * @param callback Callback Object with the new state.
     */
    public void setCallback(Callback callback) {
        if (callbacks.size() == 1) {
            callbacks.set(0, callback);
            return;
        }
        //Handle Page Callback with Index
        for (int i = 0; i < callbacks.size(); i++) {
            if (callback.get_id() == callbacks.get(i).get_id()) {
                callbacks.set(i, callback);
                return;
            }
        }
    }

    /**
     * Retrieve the AuthId.
     *
     * @return The AuthId attribute associate with this {@link Node}
     */
    public String getAuthId() {
        return this.authId;
    }

    /**
     * Retrieve the Stage.
     *
     * @return The Stage attribute associate with this {@link Node}
     */
    public String getStage() {
        return this.stage;
    }

    /**
     * Retrieve the Header.
     *
     * @return The Header attribute associate with this {@link Node}
     */
    public String getHeader() {
        return this.header;
    }

    /**
     * Retrieve the Description.
     *
     * @return The Description attribute associate with this {@link Node}
     */
    public String getDescription() {
        return this.description;
    }

    String getAuthServiceId() {
        return this.authServiceId;
    }
}
