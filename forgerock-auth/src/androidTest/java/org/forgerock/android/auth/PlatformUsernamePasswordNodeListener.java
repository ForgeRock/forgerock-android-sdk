/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import org.forgerock.android.auth.callback.NameCallback;
import org.forgerock.android.auth.callback.PasswordCallback;
import org.forgerock.android.auth.callback.ValidatedCreatePasswordCallback;
import org.forgerock.android.auth.callback.ValidatedCreateUsernameCallback;

import static org.forgerock.android.auth.AndroidBaseTest.PASSWORD;
import static org.forgerock.android.auth.AndroidBaseTest.USERNAME;

public class PlatformUsernamePasswordNodeListener extends NodeListenerFuture<FRSession> {

    private Context context;

    public PlatformUsernamePasswordNodeListener(Context context) {
        this.context = context;
    }

    @Override
    public void onCallbackReceived(Node node) {
        boolean moveToNext = false;
        if (node.getCallback(ValidatedCreateUsernameCallback.class) != null) {
            node.getCallback(ValidatedCreateUsernameCallback.class).setUsername(USERNAME);
            moveToNext = true;
        }

        if (node.getCallback(ValidatedCreatePasswordCallback.class) != null) {
            node.getCallback(ValidatedCreatePasswordCallback.class).setPassword(PASSWORD.toCharArray());
            moveToNext = true;
        }
        if (moveToNext) {
            node.next(context, this);
        }
    }

}
