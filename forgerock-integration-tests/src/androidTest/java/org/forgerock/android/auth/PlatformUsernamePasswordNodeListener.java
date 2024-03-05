/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import org.forgerock.android.auth.callback.ValidatedPasswordCallback;
import org.forgerock.android.auth.callback.ValidatedUsernameCallback;

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
        if (node.getCallback(ValidatedUsernameCallback.class) != null) {
            node.getCallback(ValidatedUsernameCallback.class).setUsername(USERNAME);
            moveToNext = true;
        }

        if (node.getCallback(ValidatedPasswordCallback.class) != null) {
            node.getCallback(ValidatedPasswordCallback.class).setPassword(PASSWORD.toCharArray());
            moveToNext = true;
        }
        if (moveToNext) {
            node.next(context, this);
        }
    }

}
