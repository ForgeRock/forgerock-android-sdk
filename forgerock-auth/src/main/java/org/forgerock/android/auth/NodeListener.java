/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.callback.Callback;
import org.forgerock.android.auth.callback.CallbackFactory;
import org.forgerock.android.auth.callback.MetadataCallback;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.callback.UnsupportedCallbackException;

import static org.forgerock.android.auth.Node.AUTH_ID;
import static org.forgerock.android.auth.Node.DESCRIPTION;
import static org.forgerock.android.auth.Node.HEADER;
import static org.forgerock.android.auth.Node.STAGE;

/**
 * Interface for an object that listens to changes resulting from a {@link AuthService}.
 */
public interface NodeListener<T> extends FRListener<T> {


    /**
     * Notify the listener that the {@link AuthService} has been started and moved to the first node.
     *
     * @param node The first Node
     */
    void onCallbackReceived(Node node);

    /**
     * Transform the response from AM Intelligent Tree to Node Object, after the transformation
     * {@link #onCallbackReceived(Node)} will be invoked with the returned {@link Node}.
     *
     * @param authServiceId Unique Auth Service Id
     * @param response      The JSON Response from AM Intelligent Tree
     * @return The Node Object
     * @throws Exception Any error during the transformation
     */
    default Node onCallbackReceived(String authServiceId, JSONObject response) throws Exception {

        List<Callback> callbacks = parseCallback(response.getJSONArray("callbacks"));

        return new Node(response.getString(AUTH_ID)
                , response.optString(STAGE, getStage(callbacks))
                , response.optString(HEADER, null)
                , response.optString(DESCRIPTION, null)
                , authServiceId,
                callbacks);
    }

    /**
     * Parse the JSON Array callback response from AM, and transform to {@link Callback} instances.
     *
     * @param jsonArray The JSON Array callback response from AM
     * @return A List of {@link Callback} Object
     * @throws Exception Any error during the transformation
     */
    default List<Callback> parseCallback(JSONArray jsonArray) throws Exception {
        List<Callback> callbacks = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject cb = jsonArray.getJSONObject(i);
            String type = cb.getString("type");
            // Return the Callback Class which represent the Callback from AM
            Class<? extends Callback> clazz = CallbackFactory.getInstance().getCallbacks().get(type);
            if (clazz == null) {
                //When Callback is not registered to the SDK
                throw new UnsupportedCallbackException(null, "Callback Type Not Supported: " + cb.getString("type"));
            }
            callbacks.add(clazz.getConstructor(JSONObject.class, int.class).newInstance(cb, i));
        }
        return callbacks;
    }

    /**
     * Workaround stage property for AM version < 7.0.
     * https://github.com/jaredjensen/forgerock-sdk-blog/blob/master/auth_tree_stage.md
     *
     * @param callbacks Callback from Intelligent Tree
     * @return stage or null if not found.
     */
    default String getStage(List<Callback> callbacks) {
        for (Callback callback : callbacks) {
            if (callback.getClass().equals(MetadataCallback.class)) {
                try {
                    return ((MetadataCallback) callback).getValue().getString("stage");
                } catch (JSONException e) {
                    //ignore and continue to find the next metadata callback.
                }
            }
        }
        return null;
    }
}

