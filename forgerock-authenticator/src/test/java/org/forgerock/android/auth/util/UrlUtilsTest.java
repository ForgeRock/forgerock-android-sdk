/*
 * Copyright (c) 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class UrlUtilsTest {

    @Test
    public void testGetQueryString() throws URISyntaxException {
        String url = "https://example.com/path?param1=value1&param2=value2";
        String expectedQuery = "param1=value1&param2=value2";
        String actualQuery = UrlUtils.getQueryString(url);
        assertEquals(expectedQuery, actualQuery);
    }

    @Test
    public void testGetQueryStringNoQuery() throws URISyntaxException {
        String url = "https://example.com/path";
        String actualQuery = UrlUtils.getQueryString(url);
        assertNull(actualQuery);
    }

    @Test
    public void testGetQueryStringEmptyUrl() throws URISyntaxException {
        String url = "";
        String result = UrlUtils.getQueryString(url);
        assertNull(result);
    }

    @Test
    public void testGetQueryStringInvalidUrl() throws URISyntaxException {
        String url = "invalid-url";
        String result = UrlUtils.getQueryString(url);
        assertNull(result);
    }

    @Test
    public void testParseQueryParams() throws UnsupportedEncodingException {
        String query = "param1=value1&param2=value2&param3=value%203";
        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put("param1", "value1");
        expectedParams.put("param2", "value2");
        expectedParams.put("param3", "value 3");
        Map<String, String> actualParams = UrlUtils.parseQueryParams(query);
        assertEquals(expectedParams, actualParams);
    }

    @Test
    public void testParseQueryParamsEmptyQuery() throws UnsupportedEncodingException {
        String query = "";
        Map<String, String> expectedParams = new HashMap<>();
        Map<String, String> actualParams = UrlUtils.parseQueryParams(query);
        assertEquals(expectedParams, actualParams);
    }

    @Test
    public void testUpdateQueryParams() throws URISyntaxException, UnsupportedEncodingException {
        String url = "https://example.com/path?param1=value1&param2=value2";
        Map<String, String> newParams = new HashMap<>();
        newParams.put("param1", "value1");
        newParams.put("param2", "new_value2");
        newParams.put("param3", "value3");
        String expectedUrl = UrlUtils.updateQueryParams(url, newParams);
        String query = UrlUtils.getQueryString(expectedUrl);
        Map<String, String> expectedParams = UrlUtils.parseQueryParams(query);
        assertEquals("value1", expectedParams.get("param1"));
        assertEquals("new_value2", expectedParams.get("param2"));
        assertEquals("value3", expectedParams.get("param3"));
    }

    @Test
    public void testUpdateQueryParamsNoQuery() throws URISyntaxException, UnsupportedEncodingException {
        String url = "https://example.com/path";
        Map<String, String> newParams = new HashMap<>();
        newParams.put("param1", "value1");
        String expectedUrl = "https://example.com/path?param1=value1";
        String actualUrl = UrlUtils.updateQueryParams(url, newParams);
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    public void testUpdateQueryParamsEmptyUrl() {
        String url = "";
        Map<String, String> newParams = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> UrlUtils.updateQueryParams(url, newParams));
    }

    @Test
    public void testUpdateQueryParamsInvalidUrl() {
        String url = "invalid-url";
        Map<String, String> newParams = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> UrlUtils.updateQueryParams(url, newParams));
    }

}
