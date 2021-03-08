/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.app.PendingIntent;

import androidx.annotation.NonNull;

import com.google.android.gms.fido.fido2.Fido2ApiClient;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.forgerock.android.auth.callback.WebAuthnRegistrationCallback;
import org.forgerock.android.auth.webauthn.WebAuthnRegistration;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * For WebAuthnRegistration Testing
 */
public class MockWebAuthnRegistrationCallback extends WebAuthnRegistrationCallback {

    public MockWebAuthnRegistrationCallback() {
    }

    public MockWebAuthnRegistrationCallback(JSONObject jsonObject, int index) {
        super(jsonObject, index);
    }

    @Override
    protected WebAuthnRegistration getWebAuthnRegistration() throws JSONException {
        return new MockWebAuthRegistration(getValue());
    }

    private class MockWebAuthRegistration extends WebAuthnRegistration {

        public MockWebAuthRegistration(JSONObject input) throws JSONException, UnsupportedOperationException {
            super(input);
        }

        @Override
        protected Task<PendingIntent> getRegisterPendingIntent(Fido2ApiClient fido2ApiClient, PublicKeyCredentialCreationOptions options) {
            return new DummyTask() {
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
