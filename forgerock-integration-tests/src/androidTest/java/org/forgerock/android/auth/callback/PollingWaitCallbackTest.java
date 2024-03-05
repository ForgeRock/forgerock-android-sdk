/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import org.forgerock.android.auth.TreeTest;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListenerFuture;
import org.forgerock.android.auth.UsernamePasswordNodeListener;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class PollingWaitCallbackTest extends TreeTest {

    private int hit = 0;

    @Override
    protected String getTreeName() {
        return "PollingWaitCallbackTest";
    }

    @Override
    protected NodeListenerFuture<FRSession> getNodeListenerFuture() {
        return new UsernamePasswordNodeListener(context) {
            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(PollingWaitCallback.class) != null) {
                    PollingWaitCallback callback = node.getCallback(PollingWaitCallback.class);
                    assertThat(callback.getWaitTime()).isEqualTo("10000");
                    assertThat(callback.getMessage()).isEqualTo("Please Wait");
                    hit++;
                }
                if (node.getCallback(ConfirmationCallback.class) != null) {
                    ConfirmationCallback callback = node.getCallback(ConfirmationCallback.class);
                    assertThat(callback.getPrompt()).isEmpty();
                    assertThat(callback.getMessageType()).isEqualTo(0);
                    assertThat(callback.getOptions().contains("Exit")).isTrue();
                    assertThat(callback.getOptionType()).isEqualTo(-1);
                    assertThat(callback.getDefaultOption()).isEqualTo(0);
                    callback.setSelectedIndex(0);
                    node.next(context, this );
                    hit++;
                }

                super.onCallbackReceived(node);

            }
        };
    }

    @Override
    public void testTree() throws ExecutionException, InterruptedException {
        super.testTree();
        assertThat(hit).isEqualTo(2);
    }
}
