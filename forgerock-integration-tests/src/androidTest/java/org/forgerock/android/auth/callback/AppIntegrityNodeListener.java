/*
 * Copyright (c) 2022 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import static org.forgerock.android.auth.AndroidBaseTest.USERNAME;

import android.content.Context;

import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListenerFuture;

import java.util.List;

public class AppIntegrityNodeListener extends NodeListenerFuture<FRSession> {

    private Context context;
    private String nodeConfiguration;

    public AppIntegrityNodeListener(Context context, String nodeConfiguration) {
        this.context = context;
        this.nodeConfiguration = nodeConfiguration;
    }

    @Override
    public void onCallbackReceived(Node node) {
        if (node.getCallback(ChoiceCallback.class) != null) {
            ChoiceCallback choiceCallback = node.getCallback(ChoiceCallback.class);
            List<String> choices = choiceCallback.getChoices();
            int choiceIndex = choices.indexOf(nodeConfiguration);
            choiceCallback.setSelectedIndex(choiceIndex);
            node.next(context, this);
        }
        if (node.getCallback(NameCallback.class) != null) {
            node.getCallback(NameCallback.class).setName(USERNAME);
            node.next(context, this);
        }
    }
}
