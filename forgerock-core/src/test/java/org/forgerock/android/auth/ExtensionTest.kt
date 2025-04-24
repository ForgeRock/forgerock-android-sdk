/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
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

    @Test
    fun testIsAbsoluteUrl() {
        val url = "https://www.example.com"
        assertTrue(url.isAbsoluteUrl())
    }

    @Test
    fun testIsNotAbsoluteUrlWithoutScheme() {
        val url = "www.example.com"
        assertFalse(url.isAbsoluteUrl())
    }

    @Test
    fun testOnlyScheme() {
        val url = "https://" // Invalid URL
        assertFalse(url.isAbsoluteUrl())
    }

    @Test
    fun testOnlySchemeAndPath() {
        val url = "https://?value=test" // Invalid URL
        assertFalse(url.isAbsoluteUrl())
    }

    @Test
    fun testIsNotAbsoluteUrlWithInvalidUrl() {
        val url = "/as/revoke"
        assertFalse(url.isAbsoluteUrl())
    }

}