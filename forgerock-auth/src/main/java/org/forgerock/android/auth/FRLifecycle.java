/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Class for handling SDK Lifecycle event.
 */
public class FRLifecycle {

    private final static ArrayList<FRLifecycleListener>
            lifecycleListeners = new ArrayList<>();

    /**
     * Register a {@link FRLifecycleListener}
     *
     * @param lifecycleListener The {@link FRLifecycleListener}
     */
    public static void registerFRLifeCycleListener(FRLifecycleListener lifecycleListener) {
        if (lifecycleListener == null)
            throw new NullPointerException();
        synchronized (lifecycleListeners) {
            if (!lifecycleListeners.contains(lifecycleListener)) {
                lifecycleListeners.add(lifecycleListener);
            }
        }
    }

    /**
     * Unregister a {@link FRLifecycleListener}
     *
     * @param lifecycleListener The {@link FRLifecycleListener}
     */
    public static void unregisterFRLifeCycleListener(FRLifecycleListener lifecycleListener) {
        synchronized (lifecycleListeners) {
            lifecycleListeners.remove(lifecycleListener);
        }
    }

    private static Object[] toArray() {
        Object[] listeners = null;
        synchronized (lifecycleListeners) {
            if (lifecycleListeners.size() > 0) {
                listeners = lifecycleListeners.toArray();
            }
        }
        return listeners;
    }

    static void dispatchSSOTokenUpdated(SSOToken ssoToken) {
        Object[] listeners = toArray();
        if (listeners != null) {
            for (Object callback : listeners) {
                ((FRLifecycleListener) callback).onSSOTokenUpdated(ssoToken);
            }
        }
    }

    static void dispatchLogout() {
        Object[] listeners = toArray();
        if (listeners != null) {
            for (Object callback : listeners) {
                ((FRLifecycleListener) callback).onLogout();
            }
        }
    }

    static void dispatchCookiesUpdated(Collection<String> cookies) {
        Object[] listeners = toArray();
        if (listeners != null) {
            for (Object callback : listeners) {
                ((FRLifecycleListener) callback).onCookiesUpdated(cookies);
            }
        }
    }
}
