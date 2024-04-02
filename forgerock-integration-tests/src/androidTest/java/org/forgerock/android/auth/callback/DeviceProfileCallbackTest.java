/*
 * Copyright (c) 2020 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import org.assertj.core.api.Assertions;
import org.forgerock.android.auth.TreeTest;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.NodeListenerFuture;
import org.forgerock.android.auth.UsernamePasswordNodeListener;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class DeviceProfileCallbackTest extends TreeTest {

    private int hit = 0;

    @Override
    protected String getTreeName() {
        return "DeviceProfileCallbackTest";
    }

    @Override
    protected NodeListenerFuture<FRSession> getNodeListenerFuture() {
        return new UsernamePasswordNodeListener(context) {
            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(DeviceProfileCallback.class) != null) {
                    DeviceProfileCallback callback = node.getCallback(DeviceProfileCallback.class);
                    assertThat(callback.getMessage()).isEqualTo("Collecting profile ...");
                    assertThat(callback.isLocation()).isTrue();
                    assertThat(callback.isMetadata()).isTrue();

                    NodeListener<FRSession> nodeListener = this;

                    callback.execute(context, new FRListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            node.next(context, nodeListener );
                            hit++;
                        }

                        @Override
                        public void onException(Exception e) {
                            Assertions.fail(e.getMessage());
                        }
                    });
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
