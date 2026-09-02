/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.inOrder
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class SingleSignOnInterceptorTest {

    private lateinit var sessionManager: SessionManager
    private lateinit var tokenManager: TokenManager
    private lateinit var singleSignOnManager: SingleSignOnManager
    private lateinit var chain: Interceptor.Chain
    private lateinit var interceptor: SingleSignOnInterceptor

    @Before
    fun setUp() {
        sessionManager = mock()
        tokenManager = mock()
        singleSignOnManager = mock()
        chain = mock()
        interceptor = SingleSignOnInterceptor(sessionManager)
    }

    @Test
    fun testNullTokenProceedsWithoutTouchingSession() {
        //No session token received (noSession=true), we don't destroy the existing session.
        interceptor.intercept(chain, null)

        verify(chain).proceed(null as SSOToken?)
        verify(sessionManager, never()).getSingleSignOnManager()
        verify(sessionManager, never()).getTokenManager()
    }

    @Test
    fun testNoStoredTokenPersistsNewTokenWithoutRevoking() {
        //Centralized login: no existing session token, the existing session does not apply,
        //the received token must be persisted without revoking the OAuth2.0 tokens.
        val token = SSOToken("newToken")
        whenever(sessionManager.singleSignOnManager).thenReturn(singleSignOnManager)
        whenever(singleSignOnManager.token).thenReturn(null)

        interceptor.intercept(chain, token)

        verify(singleSignOnManager).persist(token)
        verifyNoInteractions(tokenManager)
        verify(chain).proceed(token)
    }

    @Test
    fun testSameTokenProceedsWithoutPersistOrRevoke() {
        //The received token matches the stored token, nothing to do.
        val token = SSOToken("sameToken")
        whenever(sessionManager.singleSignOnManager).thenReturn(singleSignOnManager)
        whenever(singleSignOnManager.token).thenReturn(SSOToken("sameToken"))

        interceptor.intercept(chain, token)

        verify(singleSignOnManager, never()).persist(any<SSOToken>())
        verifyNoInteractions(tokenManager)
        verify(chain).proceed(token)
    }

    @Test
    fun testTokenMismatchRevokesThenPersistsThenProceeds() {
        //A real prior session token existed and changed, the Access Token must be revoked.
        val storedToken = SSOToken("storedToken")
        val token = SSOToken("newToken")
        whenever(sessionManager.singleSignOnManager).thenReturn(singleSignOnManager)
        whenever(singleSignOnManager.token).thenReturn(storedToken)
        whenever(sessionManager.tokenManager).thenReturn(tokenManager)

        interceptor.intercept(chain, token)

        val order = inOrder(tokenManager, singleSignOnManager, chain)
        order.verify(tokenManager).revokeAndEndSession(null as FRListener<Void>?)
        order.verify(singleSignOnManager).persist(token)
        order.verify(chain).proceed(token)
    }
}
