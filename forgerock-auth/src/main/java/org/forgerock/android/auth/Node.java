/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.forgerock.android.auth.callback.Callback;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public class Node implements Serializable {

    private final String authId;
    private final String stage;
    private final String authServiceId;
    private final List<Callback> callbacks;

    /**
     * Returns {@link JSONObject} mapping of the object
     *
     * @return {@link JSONObject} mapping of the object
     * @throws JSONException Failed to map to {@link JSONObject}
     */
    JSONObject toJsonObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("authId", authId);
        if (stage != null) {
            jsonObject.put("stage", stage);
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

    public List<Callback> getCallbacks() {
        return callbacks;
    }

    /**
     * Move on to the next node in the tree.
     *
     * @param context  The Application Context
     * @param listener Listener for receiving {@link AuthService} related changes
     */
    public void next(Context context, NodeListener listener) {
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
            if (callback.getId() == callbacks.get(i).getId()) {
                callbacks.set(i, callback);
                return;
            }
        }
    }

}
