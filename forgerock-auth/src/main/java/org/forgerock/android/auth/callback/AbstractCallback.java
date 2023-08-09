/*
 * Copyright (c) 2019 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import androidx.annotation.Keep;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Abstract Callback that provides the raw content of the Callback, and allow sub classes to access
 * Callback's input and output
 */
public abstract class AbstractCallback implements Callback {

    //The content is as JSON representation, JSONObject is not Serializable
    protected static final String VALUE = "value";

    protected String content;
    protected int _id;

    public AbstractCallback() {}

    protected JSONObject getContentAsJson() throws JSONException {
        return new JSONObject(content);
    }

    public AbstractCallback(JSONObject raw, int index) {
        setContent(raw);
        _id = raw.optInt("_id", index);

        JSONArray output = raw.optJSONArray("output");
        if (output != null) {
            for (int i = 0; i < output.length(); i++) {
                try {
                    JSONObject elm = output.getJSONObject(i);
                    if (!elm.isNull(VALUE)) {
                        setAttribute(getName(elm), elm.get(VALUE));
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    protected abstract void setAttribute(String name, Object value);

    protected String getName(JSONObject jsonObject) {
        return jsonObject.optString("name");
    }

    /**
     * Sets the value of the Callback
     *
     * @param jsonObject The Json Object to represent the Callback
     */
    protected void setContent(JSONObject jsonObject) {
        content = jsonObject.toString();

    }

    /**
     * Set the value for the input.
     *
     * @param value The input value
     * @param index The index of the element.
     */
    protected void setValue(Object value, int index) {
        try {
            JSONObject json = getContentAsJson();
            JSONObject input = getInput(json, index);
            input.put(VALUE, value);
            setContent(json);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the first value for input
     *
     * @param value The input value
     */
    protected void setValue(Object value) {
        setValue(value, 0);
    }

    /**
     * Get the first value for input
     */
    public Object getInputValue() {
        return getInputValue(0);
    }


    /**
     * Get the value for input
     * @param index The index of the element.
     */
    public Object getInputValue(int index) {
        JSONObject input = null;
        try {
            input = getInput(getContentAsJson(), index);
            return input.get(VALUE);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private JSONObject getInput(JSONObject content, int index) throws JSONException {
        return content
                .getJSONArray("input")
                .getJSONObject(index);

    }

    /**
     * Get the callback content
     */
    public String getContent() {
        return this.content;
    }

    /**
     * Get the _id received from server
     */
    public int get_id() {
        return this._id;
    }
}
