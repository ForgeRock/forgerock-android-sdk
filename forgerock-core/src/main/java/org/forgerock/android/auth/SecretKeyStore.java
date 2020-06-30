/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

/**
 * Interface for storing the Encrypted {@link javax.crypto.SecretKey}
 */
interface SecretKeyStore {

    /**
     * Persist the encrypted Secret Key
     *
     * @param encryptedSecretKey The encrypted Secret Key
     */
    void persist(String encryptedSecretKey);

    /**
     * Retrieve the encrypted Secret Key
     * @return The Encrypted Secret Key
     */
    String getEncryptedSecretKey();

    /**
     * Remove the encrypted Secret Key.
     */
    void remove();

}
