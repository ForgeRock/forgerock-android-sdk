/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

/**
 * Interface for data repository to persist and delete data from repository
 */
public interface DataRepository {

    /**
     * Persist data to the repository.
     *
     * @param key   The key for the value
     * @param value The data value
     */
    void save(String key, String value);

    /**
     * Retrieve the value from the repository
     *
     * @param key The name of the value to retrieve.
     * @return The value if exist or null if does not exist.
     */
    String getString(String key);

    /**
     * Remove data from the repository.
     *
     * @param key The name of the value to remove.
     */
    void delete(String key);

    /**
     * Remove all data from the repository
     */
    void deleteAll();

}
