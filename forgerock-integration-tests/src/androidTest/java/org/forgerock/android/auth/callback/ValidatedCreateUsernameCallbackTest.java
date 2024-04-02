/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListenerFuture;
import org.forgerock.android.auth.TreeTest;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidatedCreateUsernameCallbackTest extends TreeTest {

    private int hit = 0;

    @Override
    protected String getTreeName() {
        return "PlatformUsernamePasswordTest";
    }

    @Override
    protected NodeListenerFuture<FRSession> getNodeListenerFuture() {
        return new NodeListenerFuture<FRSession>() {
            @Override
            public void onCallbackReceived(Node node) {
                boolean moveToNext = false;
                if (node.getCallback(NameCallback.class) != null) {
                    node.getCallback(NameCallback.class).setName(USERNAME);
                    moveToNext = true;
                    hit++;
                }

                if (node.getCallback(ValidatedPasswordCallback.class) != null) {
                    node.getCallback(ValidatedPasswordCallback.class).setPassword(PASSWORD.toCharArray());
                    moveToNext = true;
                    hit++;
                }
                if (moveToNext) {
                    node.next(context, this);
                }
            }
        };
    }

    @Override
    public void testTree() throws ExecutionException, InterruptedException {
        super.testTree();
        assertThat(hit).isEqualTo(2);
    }
}
