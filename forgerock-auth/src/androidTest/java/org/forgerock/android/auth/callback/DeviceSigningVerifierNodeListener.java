/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import static org.forgerock.android.auth.callback.BaseDeviceBindingTest.USERNAME;

import android.content.Context;

import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListenerFuture;

import java.util.List;

public class DeviceSigningVerifierNodeListener extends NodeListenerFuture<FRSession> {

    private Context context;
    private String nodeConfiguration;

    public DeviceSigningVerifierNodeListener(Context context, String nodeConfiguration) {
        this.context = context;
        this.nodeConfiguration = nodeConfiguration;
    }

    @Override
    public void onCallbackReceived(Node node) {
        if (node.getCallback(ChoiceCallback.class) != null) {
            ChoiceCallback choiceCallback = node.getCallback(ChoiceCallback.class);
            List<String> choices = choiceCallback.getChoices();
            // Explicitly set the first choice collector to "collectusername" for non "usernameless" flows
            if (choices.contains("usernameless") && !nodeConfiguration.equals("usernameless")) {
                int choiceIndex = choices.indexOf("collectusername");
                choiceCallback.setSelectedIndex(choiceIndex);
                node.next(context, this);
                return;
            }
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