/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

/**
 * Utility to send events to {@link FRListener}
 */
public class Listener {

    private Listener() {
    }

    /**
     * Notify the Listener with success result
     *
     * @param listener The listener to listen for events
     * @param value    The Value send with the event.
     * @param <T>      The Value Type
     */
    public static <T> void onSuccess(final FRListener<T> listener, final T value) {
        if (listener != null) {
            listener.onSuccess(value);
        }
    }

    /**
     * Notify the Listener with failed result
     *
     * @param listener The listener to listen for events
     * @param value    The Exception send with the event.
     */
    public static void onException(final FRListener<?> listener, final Exception value) {
        if (listener != null) {
            listener.onException(value);
        }
    }
}
