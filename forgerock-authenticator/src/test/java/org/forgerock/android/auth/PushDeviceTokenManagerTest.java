/*
 * Copyright (c) 2025 Ping Identity. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.forgerock.android.auth.FRABaseTest.MECHANISM_UID;
import static org.forgerock.android.auth.FRABaseTest.mockPushMechanism;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import android.content.Context;

import org.forgerock.android.auth.exception.PushMechanismException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Calendar;
import java.util.TimeZone;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(RobolectricTestRunner.class)
public class PushDeviceTokenManagerTest {

    private PushDeviceTokenManager pushDeviceTokenManager;

    private MockWebServer server;

    @Mock
    private Context mockContext;

    @Mock
    private StorageClient mockStorageClient;

    @Mock
    private PushMechanism mockPushMechanism;

    @Mock
    private FRAListener<Void> mockListener;

    @Captor
    private ArgumentCaptor<PushDeviceToken> pushDeviceTokenArgumentCaptor;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        server = new MockWebServer();
        server.start();

        pushDeviceTokenManager = new PushDeviceTokenManager(mockContext, mockStorageClient, null);
        PushResponder.getInstance(mockStorageClient);
    }

    @After
    public void shutdown() throws IOException {
        server.shutdown();
    }

    @Test
    public void testGetPushDeviceTokenSuccess() {
        // Given
        PushDeviceToken expectedToken = new PushDeviceToken("token123", Calendar.getInstance(TimeZone.getTimeZone("UTC")));
        when(mockStorageClient.getPushDeviceToken()).thenReturn(expectedToken);

        // When
        PushDeviceToken actualToken = pushDeviceTokenManager.getPushDeviceToken();

        // Then
        assertEquals(expectedToken, actualToken);
        verify(mockStorageClient).getPushDeviceToken();
    }

    @Test
    public void testGetPushDeviceTokenNotFound() {
        // Given
        when(mockStorageClient.getPushDeviceToken()).thenReturn(null);

        // When
        PushDeviceToken actualToken = pushDeviceTokenManager.getPushDeviceToken();

        // Then
        assertNull(actualToken);
        verify(mockStorageClient).getPushDeviceToken();
    }

    @Test
    public void testGetDeviceTokenIdSuccess() {
        // Given
        String expectedTokenId = "token123";
        pushDeviceTokenManager = new PushDeviceTokenManager(mockContext, mockStorageClient, expectedTokenId);

        // When
        String actualTokenId = pushDeviceTokenManager.getDeviceTokenId();

        // Then
        assertEquals(expectedTokenId, actualTokenId);
    }

    @Test
    public void testGetDeviceTokenIdNotFound() {
        // Given
        pushDeviceTokenManager = new PushDeviceTokenManager(mockContext, mockStorageClient, null);

        // When
        String actualTokenId = pushDeviceTokenManager.getDeviceTokenId();

        // Then
        assertNull(actualTokenId);
    }

    @Test
    public void testSetDeviceTokenWhenTokenIsDifferent() {
        // Given
        String newDeviceToken = "newDeviceToken";

        // When
        pushDeviceTokenManager.setDeviceToken(newDeviceToken);

        // Then
        verify(mockStorageClient).setPushDeviceToken(pushDeviceTokenArgumentCaptor.capture());
        assertEquals(newDeviceToken, pushDeviceTokenArgumentCaptor.getValue().getTokenId());
    }

    @Test
    public void testSetDeviceTokenWhenTokenIsSame() {
        // Given
        String existingToken = "existingToken";
        pushDeviceTokenManager = new PushDeviceTokenManager(mockContext, mockStorageClient, existingToken);

        // When
        pushDeviceTokenManager.setDeviceToken(existingToken);

        // Then
        verify(mockStorageClient, never()).setPushDeviceToken(any());
    }

    @Test
    public void testSetDeviceTokenWhenTokenIsNull() {
        // Given
        String newDeviceToken = "newDeviceToken";
        pushDeviceTokenManager = new PushDeviceTokenManager(mockContext, mockStorageClient, null);

        // When
        pushDeviceTokenManager.setDeviceToken(newDeviceToken);

        // Then
        verify(mockStorageClient).setPushDeviceToken(pushDeviceTokenArgumentCaptor.capture());
        assertEquals(newDeviceToken, pushDeviceTokenArgumentCaptor.getValue().getTokenId());
    }

    @Test
    public void testUpdateDeviceTokenSuccess() {
        // Given
        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK));
        String newDeviceToken = "newDeviceToken";
        HttpUrl baseUrl = server.url("/");
        PushMechanism mockPushMechanism = mockPushMechanism(MECHANISM_UID, baseUrl.toString());
        FRAListenerFuture pushListenerFuture = new FRAListenerFuture<Integer>();

        // When
        pushDeviceTokenManager.updateDeviceToken(newDeviceToken, mockPushMechanism, pushListenerFuture);

        // Then
        verify(mockStorageClient).setPushDeviceToken(pushDeviceTokenArgumentCaptor.capture());
        assertEquals(newDeviceToken, pushDeviceTokenArgumentCaptor.getValue().getTokenId());
        try {
            pushListenerFuture.get();
        } catch (Exception ignored) {
            // ignored
        }
    }

    @Test
    public void testUpdateDeviceTokenWhenIsTheSame() {
        // Given
        String existingToken = "existingToken";
        pushDeviceTokenManager = new PushDeviceTokenManager(mockContext, mockStorageClient, existingToken);

        // When
        pushDeviceTokenManager.updateDeviceToken(existingToken, mockPushMechanism, mockListener);

        // Then
        verify(mockStorageClient, never()).setPushDeviceToken(any());
    }

    @Test
    public void testUpdateDeviceTokenFailure() {
        // Given
        server.enqueue(new MockResponse().setResponseCode(HTTP_NOT_FOUND));
        String newDeviceToken = "newDeviceToken";
        HttpUrl baseUrl = server.url("/");
        PushMechanism mockPushMechanism = mockPushMechanism(MECHANISM_UID, baseUrl.toString());
        FRAListenerFuture pushListenerFuture = new FRAListenerFuture<Integer>();

        // When
        pushDeviceTokenManager.updateDeviceToken(newDeviceToken, mockPushMechanism, pushListenerFuture);

        // Then
        verify(mockStorageClient).setPushDeviceToken(pushDeviceTokenArgumentCaptor.capture());
        assertEquals(newDeviceToken, pushDeviceTokenArgumentCaptor.getValue().getTokenId());
        try {
            pushListenerFuture.get();
            Assert.fail("Should throw Exception");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof PushMechanismException);
        }
    }

    @Test
    public void testShouldUpdateTokenWhenTokenIsNew() {
        // Given
        String newDeviceToken = "newDeviceToken";

        // When
        boolean result = pushDeviceTokenManager.shouldUpdateToken(newDeviceToken);

        // Then
        assertTrue(result);
    }

    @Test
    public void testShouldUpdateTokenWhenTokenIsSame() {
        // Given
        String existingToken = "existingToken";
        pushDeviceTokenManager = new PushDeviceTokenManager(mockContext, mockStorageClient, existingToken);

        // When
        boolean result = pushDeviceTokenManager.shouldUpdateToken(existingToken);

        // Then
        assertFalse(result);
    }

    @Test
    public void testShouldUpdateTokenWhenCurrentTokenIsNull() {
        // Given
        String newDeviceToken = "newDeviceToken";
        pushDeviceTokenManager = new PushDeviceTokenManager(mockContext, mockStorageClient, null);

        // When
        boolean result = pushDeviceTokenManager.shouldUpdateToken(newDeviceToken);

        // Then
        assertTrue(result);
    }

    @Test
    public void testShouldUpdateTokenWhenPreviousTokenIsStoredButSame() {
        // Given
        String existingToken = "existingToken";
        pushDeviceTokenManager = new PushDeviceTokenManager(mockContext, mockStorageClient, null);
        PushDeviceToken pushDeviceToken = new PushDeviceToken(existingToken, Calendar.getInstance(TimeZone.getTimeZone("UTC")));
        when(mockStorageClient.getPushDeviceToken()).thenReturn(pushDeviceToken);

        // When
        boolean result = pushDeviceTokenManager.shouldUpdateToken(existingToken);

        // Then
        assertFalse(result);
    }

    @Test
    public void testShouldUpdateTokenWhenPreviousTokenIsStoredButDifferent() {
        // Given
        String newDeviceToken = "newDeviceToken";
        String existingToken = "existingToken";
        pushDeviceTokenManager = new PushDeviceTokenManager(mockContext, mockStorageClient, null);
        PushDeviceToken pushDeviceToken = new PushDeviceToken(existingToken, Calendar.getInstance(TimeZone.getTimeZone("UTC")));
        when(mockStorageClient.getPushDeviceToken()).thenReturn(pushDeviceToken);

        // When
        boolean result = pushDeviceTokenManager.shouldUpdateToken(newDeviceToken);

        // Then
        assertTrue(result);
    }
}
