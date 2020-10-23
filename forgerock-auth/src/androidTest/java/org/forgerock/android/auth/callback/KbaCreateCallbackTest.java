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
import org.forgerock.android.auth.PlatformUsernamePasswordNodeListener;
import org.forgerock.android.auth.TreeTest;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class KbaCreateCallbackTest extends TreeTest {

    private int hit = 0;

    @Override
    protected String getTreeName() {
        return "KbaCreateCallbackTest";
    }

    @Override
    protected NodeListenerFuture<FRSession> getNodeListenerFuture() {
        return new PlatformUsernamePasswordNodeListener(context) {
            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(KbaCreateCallback.class) != null) {
                    List<Callback> callbacks = node.getCallbacks();
                    assertThat(callbacks).hasSize(2);
                    KbaCreateCallback firstQuestion = (KbaCreateCallback) callbacks.get(0);
                    firstQuestion.setSelectedQuestion(firstQuestion.getPredefinedQuestions().get(0));
                    firstQuestion.setSelectedAnswer("Test");
                    assertThat(firstQuestion.getPrompt()).isEqualTo("Security questions");

                    KbaCreateCallback secondQuestion = (KbaCreateCallback) callbacks.get(1);
                    secondQuestion.setSelectedQuestion(secondQuestion.getPredefinedQuestions().get(1));
                    secondQuestion.setSelectedAnswer("Test");
                    assertThat(secondQuestion.getPrompt()).isEqualTo("Security questions");

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
