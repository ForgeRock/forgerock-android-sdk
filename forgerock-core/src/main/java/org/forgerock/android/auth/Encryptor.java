/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * The interface class {@link Encryptor} provides methods
 * to encrypt and decrypt data.
 */
public interface Encryptor {

    String ANDROID_KEYSTORE = "AndroidKeyStore";

    /**
     * Encrypts the data.
     *
     * @param clearText bytes to encrypt
     * @return encrypted data
     */
    byte[] encrypt(byte[] clearText);

    /**
     * Decrypts the data.
     *
     * @param encryptedData bytes to decrypt
     * @return decrypted data
     */
    byte[] decrypt(byte[] encryptedData);

    /**
     * Reset the Encryption Provider, remove all created keys
     */
    void reset() throws GeneralSecurityException, IOException;

}
