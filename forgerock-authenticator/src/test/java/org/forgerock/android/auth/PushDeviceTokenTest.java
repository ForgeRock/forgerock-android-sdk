/*
 * Copyright (c) 2025 Ping Identity. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
public class PushDeviceTokenTest {

    private Calendar calendar;

    @Before
    public void setUp() {
        calendar = new GregorianCalendar(2025, Calendar.JANUARY, 1, 10, 0, 0);
    }

    @Test
    public void testConstructorAndGetters() {
        String tokenId = "testTokenId";
        PushDeviceToken deviceToken = new PushDeviceToken(tokenId, calendar);

        assertEquals(tokenId, deviceToken.getTokenId());
        assertEquals(calendar, deviceToken.getTimeAdded());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithEmptyTokenId() {
        new PushDeviceToken("", calendar);
    }

    @Test
    public void testEquals() {
        PushDeviceToken deviceToken1 = new PushDeviceToken("token1", calendar);
        PushDeviceToken deviceToken2 = new PushDeviceToken("token1", calendar);
        PushDeviceToken deviceToken3 = new PushDeviceToken("token2", calendar);
        Calendar calendar2 = new GregorianCalendar(2025, Calendar.FEBRUARY, 1, 10, 0, 0);
        PushDeviceToken deviceToken4 = new PushDeviceToken("token1", calendar2);

        assertEquals(deviceToken1, deviceToken2);
        assertNotEquals(deviceToken1, deviceToken3);
        assertNotEquals(deviceToken1, deviceToken4);
        assertNotEquals(deviceToken1, null);
        assertNotEquals(deviceToken1, new Object());
    }

    @Test
    public void testHashCode() {
        PushDeviceToken deviceToken1 = new PushDeviceToken("token1", calendar);
        PushDeviceToken deviceToken2 = new PushDeviceToken("token1", calendar);
        PushDeviceToken deviceToken3 = new PushDeviceToken("token2", calendar);

        assertEquals(deviceToken1.hashCode(), deviceToken2.hashCode());
        assertNotEquals(deviceToken1.hashCode(), deviceToken3.hashCode());
    }

    @Test
    public void testToString() {
        PushDeviceToken deviceToken = new PushDeviceToken("token1", calendar);
        String expectedString = "PushDeviceToken{tokenId='token1', lastUpdate=" + calendar + '}';
        assertEquals(expectedString, deviceToken.toString());
    }

    @Test
    public void testMatches() {
        PushDeviceToken deviceToken1 = new PushDeviceToken("token1", calendar);
        PushDeviceToken deviceToken2 = new PushDeviceToken("token1", calendar);
        PushDeviceToken deviceToken3 = new PushDeviceToken("token2", calendar);
        Calendar calendar2 = new GregorianCalendar(2025, Calendar.FEBRUARY, 1, 10, 0, 0);
        PushDeviceToken deviceToken4 = new PushDeviceToken("token1", calendar2);

        assertTrue(deviceToken1.matches(deviceToken2));
        assertFalse(deviceToken1.matches(deviceToken3));
        assertFalse(deviceToken1.matches(deviceToken4));
        assertFalse(deviceToken1.matches(null));
    }

    @Test
    public void testToJson() throws JSONException {
        PushDeviceToken deviceToken = new PushDeviceToken("token1", calendar);
        String jsonString = deviceToken.toJson();
        assertTrue(jsonString.contains("\"tokenId\":\"token1\""));
        assertTrue(jsonString.contains("\"timeAdded\":"));
    }

    @Test
    public void testSerialize() {
        PushDeviceToken deviceToken = new PushDeviceToken("token1", calendar);
        String serializedString = deviceToken.serialize();
        assertTrue(serializedString.contains("\"tokenId\":\"token1\""));
        assertTrue(serializedString.contains("\"timeAdded\":"));
    }

    @Test
    public void testDeserialize() {
        String jsonString = "{\"tokenId\":\"token1\",\"timeAdded\":1704099600000}";
        PushDeviceToken deviceToken = PushDeviceToken.deserialize(jsonString);
        assertEquals("token1", deviceToken.getTokenId());
        Calendar expectedCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        expectedCalendar.setTimeInMillis(1704099600000L);
        assertEquals(expectedCalendar, deviceToken.getTimeAdded());
    }

    @Test
    public void testDeserializeNull() {
        assertNull(PushDeviceToken.deserialize(null));
    }

    @Test
    public void testDeserializeEmpty() {
        assertNull(PushDeviceToken.deserialize(""));
    }

    @Test
    public void testDeserializeInvalidJson() {
        assertNull(PushDeviceToken.deserialize("invalid json"));
    }

    @Test
    public void testCompareTo() {
        PushDeviceToken deviceToken1 = new PushDeviceToken("token1", calendar);
        PushDeviceToken deviceToken2 = new PushDeviceToken("token1", calendar);
        PushDeviceToken deviceToken3 = new PushDeviceToken("token2", calendar);
        Calendar calendar2 = new GregorianCalendar(2025, Calendar.FEBRUARY, 1, 10, 0, 0);
        PushDeviceToken deviceToken4 = new PushDeviceToken("token1", calendar2);

        assertEquals(0, deviceToken1.compareTo(deviceToken2));
        assertTrue(deviceToken1.compareTo(deviceToken3) < 0);
        assertTrue(deviceToken3.compareTo(deviceToken1) > 0);
        assertTrue(deviceToken1.compareTo(deviceToken4) < 0);
        assertTrue(deviceToken4.compareTo(deviceToken1) > 0);
        assertTrue(deviceToken1.compareTo(null) < 0);
    }
}
