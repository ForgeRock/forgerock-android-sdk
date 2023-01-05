/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import org.junit.Assert.*
import org.junit.Test


class ExtensionTest {
    @Test
    fun testLongDateToString() {
        val longTimeStamp: Long = 1672947404167 // 20230105 13:36:44
        val actualResult = longTimeStamp.convertToTime().split(" ")
        assertEquals(actualResult[0], "20230105")
        assertTrue(actualResult[1].isNotEmpty())
    }

    @Test
    fun testLongDateToStringWithDifferentPattern() {
        val longTimeStamp: Long = 1672947404167
        val actualResult = longTimeStamp.convertToTime("yyyyMMdd")
        val expectedResult = "20230105"
        assertEquals(actualResult, expectedResult)
    }

    @Test
    fun testPoorManTernary() {
        val list = listOf(1, 2, 3)
        val case1 = (list.contains(1) then true) ?: false
        assertTrue(case1)
        val case2 = (list.contains(5) then true) ?: false
        assertFalse(case2)

    }

}