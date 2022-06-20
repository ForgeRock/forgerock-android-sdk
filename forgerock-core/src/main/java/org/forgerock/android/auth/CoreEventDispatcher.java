/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import java.util.Observable;

/**
 * Dispatch an event to Observer(s) which listens for the event.
 */
class CoreEventDispatcher extends Observable {

    static final CoreEventDispatcher CLEAR_OKHTTP = new CoreEventDispatcher();

    public CoreEventDispatcher() {
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

