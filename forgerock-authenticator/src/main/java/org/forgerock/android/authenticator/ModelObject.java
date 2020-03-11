package org.forgerock.android.authenticator;

/**
 * Base class for objects which are a part of the Account Model.
 */
public abstract class ModelObject<T> implements Comparable<T> {

    /**
     * Returns true if the two objects would conflict if added to a storage system.
     * @param object The object to compare.
     * @return True if key traits of the objects match, false otherwise.
     */
    public abstract boolean matches(T object);
}
