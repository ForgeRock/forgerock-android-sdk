package org.forgerock.android.authenticator.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * List of Comparables which reorders itself whenever an element is added to the list.
 * @param <T> The particular class of Comparable that is being stored.
 */
public class SortedList<T extends Comparable> extends ArrayList<T> {

    @Override
    public boolean add(T object) {
        boolean result = super.add(object);
        Collections.sort(this);
        return result;
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        boolean result = super.addAll(collection);
        Collections.sort(this);
        return result;
    }
}
