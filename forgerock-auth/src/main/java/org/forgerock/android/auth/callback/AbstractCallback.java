/*
 * Copyright (c) 2019 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import androidx.annotation.Keep;

import org.forgerock.android.auth.Callback;
import org.forgerock.android.auth.RootAbstractCallback;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Abstract Callback that provides the raw content of the Callback, and allow sub classes to access
 * Callback's input and output
 */
public abstract class AbstractCallback extends RootAbstractCallback implements Callback {
    public AbstractCallback() {}

    public AbstractCallback(JSONObject raw, int index) {
        super(raw, index);
    }

}
