/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

/**
 * Base class for objects which are a part of the Authenticator Model.
 */
abstract class ModelObject<T> {

    /**
     * Returns true if the two objects would conflict if added to a storage system.
     * @param object The object to compare.
     * @return True if key traits of the objects match, false otherwise.
     */
    public abstract boolean matches(T object);

    /**
     * Serializes the {@link T} object into its equivalent Json representation.
     * @return a JSON string representation of {@link T}
     */
    public abstract String toJson();

}
