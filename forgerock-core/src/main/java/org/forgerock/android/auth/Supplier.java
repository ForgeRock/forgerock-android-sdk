/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

/**
 * Represents a supplier of results.
 * Similar to {@link java.util.function.Supplier} without Min SDK restriction
 */
@FunctionalInterface
public interface Supplier<T> {
    T get();
}

