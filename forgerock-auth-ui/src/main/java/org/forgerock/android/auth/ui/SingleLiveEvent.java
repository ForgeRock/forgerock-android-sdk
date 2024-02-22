/*
 * Copyright (c) 2019 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui;


/**
 * Event only trigger once after configuration change.
 */
public class SingleLiveEvent<T> {

    private T value;

    public SingleLiveEvent(T value) {
        this.value = value;
    }

    public T getValue() {
        T result = value;
        value = null;
        return result;
    }
}
