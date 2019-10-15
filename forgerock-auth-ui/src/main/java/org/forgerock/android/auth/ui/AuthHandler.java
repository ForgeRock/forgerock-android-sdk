/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui;

import org.forgerock.android.auth.Node;

/**
 * Handler to handle action during user authentication with the Auth Service
 */
public interface AuthHandler {

    /**
     * Move to next node
     * @param node The current Node
     */
    void next(Node node);

    /**
     * Cancel the current authentication, will throw {@link android.os.OperationCanceledException}
     * when user explicitly cancel the login.
     *
     * @param e Exception when cancelling the authentication process.
     */
    void cancel(Exception e);
}
