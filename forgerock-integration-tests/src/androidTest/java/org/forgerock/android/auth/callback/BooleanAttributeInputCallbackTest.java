/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListenerFuture;
import org.forgerock.android.auth.TreeTest;
import org.forgerock.android.auth.UsernamePasswordNodeListener;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class BooleanAttributeInputCallbackTest extends TreeTest {

    private int hit = 0;

    @Override
    protected String getTreeName() {
        return "BooleanAttributeInputCallbackTest";
    }

    @Override
    protected NodeListenerFuture<FRSession> getNodeListenerFuture() {
        return new UsernamePasswordNodeListener(context) {
            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(BooleanAttributeInputCallback.class) != null) {

                    List<Callback> callbacks = node.getCallbacks();
                    assertThat(callbacks).hasSize(1);
                    BooleanAttributeInputCallback callback = (BooleanAttributeInputCallback) node.getCallback(BooleanAttributeInputCallback.class);
                    assertThat(callback.getName()).isEqualTo("preferences/marketing");
                    assertThat(callback.getPrompt()).isEqualTo("Send me special offers and services");
                    assertThat(callback.isRequired()).isTrue();
                    assertThat(callback.getPolicies().toString()).isEqualTo("{}");
                    assertThat(callback.getFailedPolicies()).isEmpty();
                    assertThat(callback.getValidateOnly()).isFalse();
                    assertThat(callback.getValue()).isFalse();
                    callback.setValue(true);
                    node.next(context, this);
                    hit++;
                    return;
                }
                super.onCallbackReceived(node);

            }
        };
    }

    @Override
    public void testTree() throws ExecutionException, InterruptedException {
        super.testTree();
        assertThat(hit).isEqualTo(1);
    }
}
