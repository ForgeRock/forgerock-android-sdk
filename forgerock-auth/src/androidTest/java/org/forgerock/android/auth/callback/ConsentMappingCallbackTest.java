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

public class ConsentMappingCallbackTest extends TreeTest {

    private int hit = 0;

    @Override
    protected String getTreeName() {
        return "ConsentMappingCallbackTest";
    }

    @Override
    protected NodeListenerFuture<FRSession> getNodeListenerFuture() {
        return new UsernamePasswordNodeListener(context) {
            @Override
            public void onCallbackReceived(Node node) {
                super.onCallbackReceived(node);
                if (node.getCallback(ConsentMappingCallback.class) != null) {
                    ConsentMappingCallback callback = node.getCallback(ConsentMappingCallback.class);
                    callback.setAccept(true);
                    assertThat(callback.getAccessLevel()).isEqualTo("Actual Profile");
                    assertThat(callback.getDisplayName()).isEqualTo("Identity Mapping");
                    assertThat(callback.getIcon()).isNotNull();
                    assertThat(callback.isRequired()).isTrue();
                    assertThat(callback.getMessage()).isEqualTo("Test");
                    assertThat(callback.getName()).isEqualTo("managedUser_managedUser");
                    node.next(context, this);
                    hit++;
                }
            }
        };
    }

    @Override
    public void testTree() throws ExecutionException, InterruptedException {
        super.testTree();
        assertThat(hit).isEqualTo(1);
    }
}
