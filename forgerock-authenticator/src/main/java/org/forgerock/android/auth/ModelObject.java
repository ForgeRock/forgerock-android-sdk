/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import java.util.Calendar;

/**
 * Base class for objects which are a part of the Authenticator Model.
 */
abstract class ModelObject<T> implements Comparable<T> {

    /**
     * Returns true if the two objects would conflict if added to a storage system.
     * @param object The object to compare.
     * @return True if key traits of the objects match, false otherwise.
     */
    public abstract boolean matches(T object);

    /**
     * Creates a JSON string representation of {@link T} object. Sensitive information are not
     * exposed.
     * @return a JSON string object
     */
    public abstract String toJson();

    /**
     * Serializes the {@link T} object into its equivalent Json representation.
     * @return a JSON string representation of {@link T}
     */
    abstract String serialize();

    /**
     * Return date from milliseconds.
     * @param milliSeconds Date in milliseconds
     * @return Calendar representing date
     */
    static Calendar getDate(long milliSeconds)
    {
        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return calendar;
    }

}
