/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.android.auth.AndroidBaseTest;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.NodeListenerFuture;
import org.forgerock.android.auth.UsernamePasswordNodeListener;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class TextOutputCallbackTest extends AndroidBaseTest {
    protected final static String TREE = "TextOutputCallbackTest";

    @After
    public void logoutSession() {
        if (FRSession.getCurrentSession() != null) {
            FRSession.getCurrentSession().logout();
        }
    }

    @Test
    public void testTextOutputCallback() throws ExecutionException, InterruptedException {
        final int[] textOutputCallbacksReceived = {0};
        NodeListenerFuture<FRSession> nodeListenerFuture = new UsernamePasswordNodeListener(context) {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(TextOutputCallback.class) != null) {
                    textOutputCallbacksReceived[0]++;
                    // The TextOutputCallbackProducer script sends 4 TextOutput callbacks (of all types)
                    List<Callback> callbacks = node.getCallbacks();
                    assertThat(callbacks.size() == 4);

                    for (int i = 0; i < callbacks.size(); i++) {
                        TextOutputCallback callback = (TextOutputCallback) callbacks.get(i);
                        assert( callback.getMessage().equals("TextOutput Type 0 (INFO)") ||
                                callback.getMessage().equals("TextOutput Type 1 (WARNING)") ||
                                callback.getMessage().equals("TextOutput Type 2 (ERROR)") ||
                                callback.getMessage().equals("TextOutput Type 4 (SCRIPT)"));

                        assert( callback.getMessageType() == 0 ||
                                callback.getMessageType() == 1 ||
                                callback.getMessageType() == 2 ||
                                callback.getMessageType() == 4);
                    }
                    node.next(context, nodeListener);
                    return;
                }
                super.onCallbackReceived(node);
            }
        };

        FRSession.authenticate(context, TREE, nodeListenerFuture);
        Assert.assertNotNull(nodeListenerFuture.get());
        assertThat(textOutputCallbacksReceived[0]).isEqualTo(1);

        // Ensure that the journey finishes with success
        // Note that the SDK should NOT send TextOutput of type 4 to AM  (SDKS-3227)
        // If it does the journey will fail (see the `TextOutputCallbackProducer` script...)
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());
    }
}