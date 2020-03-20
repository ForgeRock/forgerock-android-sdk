/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import java.io.Serializable;

/**
 * Base Callback interface
 */
public interface Callback extends Serializable {

    /**
     * Return the unique id for this callback.
     * The id only available with Callback that using PageCallback
     *
     * @return The callback id.
     */
    int get_id();

    /**
     * Return the raw content of the Callback.
     */
    String getContent();

    /**
     * Return the type of the Callback, the type name should align with the Callback type returned
     * from AM
     */
    String getType();

}
