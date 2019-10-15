/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

/**
 * Interface for an object that listens to changes resulting from Encryption Keys update
 */
interface KeyUpdatedListener {

    /**
     * Notify the listener that the Secret Key or Asymmetric Keys have been updated.
     */
    void onKeyUpdated();
}
