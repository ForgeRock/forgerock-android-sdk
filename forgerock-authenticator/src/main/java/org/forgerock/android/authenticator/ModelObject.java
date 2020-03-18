/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

/**
 * Base class for objects which are a part of the Account Model.
 */
public abstract class ModelObject<T> implements Comparable<T> {

    /**
     * Returns true if the two objects would conflict if added to a storage system.
     * @param object The object to compare.
     * @return True if key traits of the objects match, false otherwise.
     */
    public abstract boolean matches(T object);
}
