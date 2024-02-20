/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
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
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class TextInputCallbackTest extends AndroidBaseTest {
    protected final static String TREE = "TextInputCallbackTest";

    @Test
    public void testTextInputCallback() throws ExecutionException, InterruptedException {
        final int[] textInputCallbackReceived = {0};
        final int[] success = {0};
        NodeListenerFuture<FRSession> nodeListenerFuture = new UsernamePasswordNodeListener(context) {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(NameCallback.class) != null) {
                    NameCallback nameCallback = node.getCallback(NameCallback.class);
                    nameCallback.setValue(USERNAME);
                    node.next(context, this );
                    return;
                }
                if (node.getCallback(TextInputCallback.class) != null) {
                    TextInputCallback callback = node.getCallback(TextInputCallback.class);
                    assertThat(callback.getPrompt()).isEqualTo("What is your username?");
                    assertThat(callback.getDefaultText()).isEqualTo("ForgerRocker");
                    textInputCallbackReceived[0]++;
                    callback.setValue(USERNAME);
                    node.next(context, nodeListener);
                    return;
                }
                // This step here is to ensure that the SDK correctly sets the value in the TextInputCallback...
                // The values entered in the NameCallback and  TextInputCallback above should match for "success"
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback callback = node.getCallback(TextOutputCallback.class);
                    assertThat(callback.getMessage()).isEqualTo("Success");
                    success[0]++;
                    node.next(context, nodeListener);
                    return;
                }
                super.onCallbackReceived(node);
            }
        };

        FRSession.authenticate(context, TREE, nodeListenerFuture);
        Assert.assertNotNull(nodeListenerFuture.get());
        assertThat(textInputCallbackReceived[0]).isEqualTo(1);
        assertThat(success[0]).isEqualTo(1);

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());
    }
}
