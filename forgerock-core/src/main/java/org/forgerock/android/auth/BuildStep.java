/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

/**
 * A Build Step to build the Builder.
 *
 * @param <T> Class with Builder pattern.
 */
public interface BuildStep<T> {
    void build(T builder);
}
