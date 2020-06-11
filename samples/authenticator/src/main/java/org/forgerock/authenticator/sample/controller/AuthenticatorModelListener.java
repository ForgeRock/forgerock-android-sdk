/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.controller;

/**
 * A listener that can be set on the AuthenticatorModel in order to be notified about certain events.
 */
public interface AuthenticatorModelListener {

    /**
     * Fired when a data is added or removed.
     */
    void dataChanged();

}
