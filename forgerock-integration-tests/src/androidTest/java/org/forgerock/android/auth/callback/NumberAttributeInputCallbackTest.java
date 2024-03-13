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

import java.util.concurrent.ExecutionException;

public class NumberAttributeInputCallbackTest extends TreeTest {

    private int hit = 0;

    @Override
    protected String getTreeName() {
        return "NumberAttributeInputCallbackTest";
    }

    @Override
    protected NodeListenerFuture<FRSession> getNodeListenerFuture() {
        return new UsernamePasswordNodeListener(context) {
            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(NumberAttributeInputCallback.class) != null) {
                    NumberAttributeInputCallback callback = node.getCallback(NumberAttributeInputCallback.class);
                    assertThat(callback.getName().contains("age")).isTrue();
                    assertThat(callback.getPrompt()).isEqualTo("How old are you?");
                    assertThat(callback.isRequired()).isTrue();
//                    try {
//                        assertThat(callback.getPolicies().getString("name")).isEqualTo("age");
//                    } catch (JSONException e) {
//                        Assertions.fail(e.getMessage());
//                    }
                    assertThat(callback.getFailedPolicies()).isEmpty();
                    assertThat(callback.getValidateOnly()).isFalse();
                    assertThat(callback.getValue()).isNull();
                    callback.setValue(30.0);
                    node.next(context, this );
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
