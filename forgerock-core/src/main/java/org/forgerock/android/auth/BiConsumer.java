/*
 * Copyright (c) 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

@FunctionalInterface
public interface BiConsumer<T, U> {

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     * @param u the input argument
     */
    void accept(T t, U u);

}