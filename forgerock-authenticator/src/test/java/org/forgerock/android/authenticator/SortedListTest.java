/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

import org.forgerock.android.authenticator.util.SortedList;
import org.junit.Test;

import java.util.Arrays;

import static junit.framework.Assert.assertEquals;

public class SortedListTest {

    public final String[] elements = {"SECOND", "THIRD", "FIRST"};

    @Test
    public void testShouldSortElementsAddedOneByOne() {
        SortedList<String> sortedList = new SortedList<>();

        for (String element : elements) {
            sortedList.add(element);
        }

        assertEquals(sortedList.get(0), "FIRST");
        assertEquals(sortedList.get(1), "SECOND");
        assertEquals(sortedList.get(2), "THIRD");
    }

    @Test
    public void testShouldSortElementsAddedTogether() {
        SortedList<String> sortedList = new SortedList<>();

        sortedList.addAll(Arrays.asList(elements));

        assertEquals(sortedList.get(0), "FIRST");
        assertEquals(sortedList.get(1), "SECOND");
        assertEquals(sortedList.get(2), "THIRD");
    }

}

