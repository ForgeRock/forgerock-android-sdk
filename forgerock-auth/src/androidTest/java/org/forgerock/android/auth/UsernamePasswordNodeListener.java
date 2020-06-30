/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListenerFuture;
import org.forgerock.android.auth.callback.NameCallback;
import org.forgerock.android.auth.callback.PasswordCallback;

import static org.forgerock.android.auth.AndroidBaseTest.PASSWORD;
import static org.forgerock.android.auth.AndroidBaseTest.USERNAME;

public class UsernamePasswordNodeListener extends NodeListenerFuture<FRSession> {

    private Context context;

    public UsernamePasswordNodeListener(Context context) {
        this.context = context;
    }

    @Override
    public void onCallbackReceived(Node node) {
        boolean moveToNext = false;
        if (node.getCallback(NameCallback.class) != null) {
            node.getCallback(NameCallback.class).setName(USERNAME);
            moveToNext = true;
        }

        if (node.getCallback(PasswordCallback.class) != null) {
            node.getCallback(PasswordCallback.class).setPassword(PASSWORD.toCharArray());
            moveToNext = true;
        }
        if (moveToNext) {
            node.next(context, this);
        }
    }

}
