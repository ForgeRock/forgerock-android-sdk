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

public class ConfirmationCallbackTest extends TreeTest {

    @Override
    protected String getTreeName() {
        return "ConfirmationCallbackTest";
    }

    private int hit = 0;

    @Override
    protected NodeListenerFuture<FRSession> getNodeListenerFuture() {
        return new UsernamePasswordNodeListener(context) {
            @Override
            public void onCallbackReceived(Node node) {
                super.onCallbackReceived(node);
                if (node.getCallback(ConfirmationCallback.class) != null) {
                    ConfirmationCallback confirmationCallback = node.getCallback(ConfirmationCallback.class);
                    assertThat(confirmationCallback.getPrompt()).isEqualTo("");
                    assertThat(confirmationCallback.getDefaultOption()).isEqualTo(1);
                    assertThat(confirmationCallback.getMessageType()).isEqualTo(0);
                    assertThat(confirmationCallback.getOptionType()).isEqualTo(-1);
                    assertThat(confirmationCallback.getOptions().get(0)).isEqualTo("Yes");
                    assertThat(confirmationCallback.getOptions().get(1)).isEqualTo("No");

                    node.getCallback(ConfirmationCallback.class).setSelectedIndex(0);
                    node.next(context, this);
                    hit++;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback callback = node.getCallback(TextOutputCallback.class);
                    assertThat(callback.getMessage()).isEqualTo("Test");
                    assertThat(callback.getMessageType()).isEqualTo(0);
                    hit++;
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
