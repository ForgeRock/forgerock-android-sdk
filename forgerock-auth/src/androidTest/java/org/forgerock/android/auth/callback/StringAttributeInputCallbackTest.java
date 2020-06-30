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

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class StringAttributeInputCallbackTest extends TreeTest {

    private int hit = 0;

    @Override
    protected String getTreeName() {
        return "StringAttributeInputCallbackTest";
    }

    @Override
    protected NodeListenerFuture<FRSession> getNodeListenerFuture() {
        return new UsernamePasswordNodeListener(context) {
            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(StringAttributeInputCallback.class) != null) {

                    List<Callback> callbacks = node.getCallbacks();
                    assertThat(callbacks).hasSize(3);
                    StringAttributeInputCallback mail = (StringAttributeInputCallback) callbacks.get(0);
                    assertThat(mail.getName()).isEqualTo("mail");
                    assertThat(mail.getPrompt()).isEqualTo("Email Address");
                    assertThat(mail.isRequired()).isTrue();
                    assertThat(mail.getPolicies()).isNotNull(); //We don't do anything with policy
                    assertThat(mail.getFailedPolicies()).hasSize(0);
                    assertThat(mail.getValidateOnly()).isFalse();

                    StringAttributeInputCallback givenName = (StringAttributeInputCallback) callbacks.get(1);
                    assertThat(givenName.getName()).isEqualTo("givenName");
                    assertThat(givenName.getPrompt()).isEqualTo("First Name");
                    assertThat(givenName.isRequired()).isTrue();
                    assertThat(givenName.getPolicies()).isNotNull(); //We don't do anything with policy
                    assertThat(givenName.getFailedPolicies()).hasSize(0);
                    assertThat(givenName.getValidateOnly()).isFalse();

                    StringAttributeInputCallback sn = (StringAttributeInputCallback) callbacks.get(2);
                    assertThat(sn.getName()).isEqualTo("sn");
                    assertThat(sn.getPrompt()).isEqualTo("Last Name");
                    assertThat(sn.isRequired()).isTrue();
                    assertThat(sn.getPolicies()).isNotNull(); //We don't do anything with policy
                    assertThat(sn.getFailedPolicies()).hasSize(0);
                    assertThat(sn.getValidateOnly()).isFalse();

                    mail.setValue("test@mail.com");
                    givenName.setValue(USERNAME);
                    sn.setValue(USERNAME);
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
