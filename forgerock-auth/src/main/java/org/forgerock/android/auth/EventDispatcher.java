/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import java.util.Observable;

/**
 * Dispatch an event to Observer(s) which listens for the event.
 */
class EventDispatcher extends Observable {

    //Observable for token removed event, either SSO Token or Access Token
    static final EventDispatcher TOKEN_REMOVED = new EventDispatcher();

    public EventDispatcher() {
        setChanged();
    }

    @Override
    protected synchronized void clearChanged() {
    }

    @Override
    public synchronized boolean hasChanged() {
        return true;
    }

}

