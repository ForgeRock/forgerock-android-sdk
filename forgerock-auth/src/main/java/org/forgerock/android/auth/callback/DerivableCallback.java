/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import androidx.annotation.Nullable;

/**
 * Interface for {@link Callback} that can be derived.
 */
public interface DerivableCallback {

    /**
     * Retrieve the derived callback class, return null if no derive callback found.
     */
    @Nullable
    Class<? extends Callback> getDerivedCallback();

}
