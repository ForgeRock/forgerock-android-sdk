/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.callback.IdPCallback;
import org.forgerock.android.auth.idp.IdPHandler;
import org.json.JSONObject;

public class IdPCallbackMock extends IdPCallback {

    public IdPCallbackMock() {
    }

    public IdPCallbackMock(JSONObject jsonObject, int index) {
        super(jsonObject, index);
    }

    @Override
    protected IdPHandler getIdPHandler() {
        if (getProvider().toLowerCase().contains("apple")) {
            return new AppleSignInHandlerMock();
        }
        throw new IllegalArgumentException();
    }
}
