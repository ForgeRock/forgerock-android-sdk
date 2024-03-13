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
import org.forgerock.android.auth.UsernamePasswordNodeListener;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class PageCallbackTest extends TreeTest {

    @Override
    protected String getTreeName() {
        return "PageCallbackTest";
    }

    @Override
    protected NodeListenerFuture<FRSession> getNodeListenerFuture() {
        return new UsernamePasswordNodeListener(context) {
            @Override
            public void onCallbackReceived(Node node) {
                assertThat(node.getStage()).isEqualTo("Test");
                assertThat(node.getHeader()).isEqualTo("Page Header Test");
                assertThat(node.getDescription()).isEqualTo("Page Description Test");
                super.onCallbackReceived(node);
            }
        };
    }

    @Override
    public void testTree() throws ExecutionException, InterruptedException {
        super.testTree();
    }
}
