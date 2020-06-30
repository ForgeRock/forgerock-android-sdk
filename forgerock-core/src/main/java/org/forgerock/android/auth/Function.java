/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

/**
 * A function that accepts one argument and produces a result.
 * Similar to {@link java.util.function.Function} without Min SDK restriction
 */
@FunctionalInterface
public interface Function<T, R> {
    R apply(T var);
}