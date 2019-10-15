/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

/**
 * Interface for an object that listens to changes resulting from a {@link AuthService}.
 */
public interface NodeListener<T> extends FRListener<T> {

    /**
     * Notify the listener that the {@link AuthService} has been started and moved to the first node.
     *
     * @param node The first Node
     */
    void onCallbackReceived(Node node);

}
