/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.webauthn;

import android.app.PendingIntent;

import androidx.annotation.NonNull;

import com.google.android.gms.fido.fido2.Fido2ApiClient;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.forgerock.android.auth.callback.WebAuthnAuthenticationCallback;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Extend the {@link WebAuthnAuthenticationCallback} and provide mocking result
 * from {@link Fido2ApiClient}
 */
public class MockWebAuthnAuthenticationCallback extends WebAuthnAuthenticationCallback {

    public MockWebAuthnAuthenticationCallback() {
    }

    public MockWebAuthnAuthenticationCallback(JSONObject jsonObject, int index) {
        super(jsonObject, index);
    }

    @Override
    protected WebAuthnAuthentication getWebAuthnAuthentication() throws JSONException {
        return new MockWebAuthnAuthentication(getValue());
    }

    private class MockWebAuthnAuthentication extends WebAuthnAuthentication {

        public MockWebAuthnAuthentication(JSONObject input) throws JSONException, UnsupportedOperationException {
            super(input);
        }

        @Override
        protected Task<PendingIntent> getSignPendingIntent(Fido2ApiClient fido2ApiClient, PublicKeyCredentialRequestOptions options) {
            return new DummyTask() {
                /**
                 * Mock the PendingIntent that retrieved from fido2ApiClient, instead of waiting for success, execute the success listener
                 * immediately
                 */
                @NonNull
                @Override
                public Task<PendingIntent> addOnSuccessListener(@NonNull OnSuccessListener<? super PendingIntent> onSuccessListener) {
                    onSuccessListener.onSuccess(null);
                    return this;
                }

                ;
            };
        }
    }
}
