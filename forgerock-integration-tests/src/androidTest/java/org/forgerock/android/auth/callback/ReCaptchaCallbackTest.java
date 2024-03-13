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
import org.forgerock.android.auth.exception.AuthenticationException;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ReCaptchaCallbackTest extends TreeTest {

    private int hit = 0;

    @Override
    protected String getTreeName() {
        return "RecaptchaCallbackTest";
    }

    @Override
    protected NodeListenerFuture<FRSession> getNodeListenerFuture() {
        return new UsernamePasswordNodeListener(context) {
            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(ReCaptchaCallback.class) != null) {
                    ReCaptchaCallback callback = node.getCallback(ReCaptchaCallback.class);
                    assertThat(callback.getReCaptchaSiteKey()).isEqualTo("siteKey");
                    callback.setValue("dummy");
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
        try {
            super.testTree();
            fail("Should Failed");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(AuthenticationException.class);
        }
        assertThat(hit).isEqualTo(1);
    }
}
