/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.webauthn;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.fido.Fido;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Listener;
import org.forgerock.android.auth.exception.WebAuthnException;
import org.forgerock.android.auth.exception.WebAuthnResponseException;

/**
 * A Fragment that start intent for WebAuthn {@link PendingIntent} and receive result using
 * {@link #onActivityResult(int, int, Intent)} from the {@link PendingIntent}
 */
public class WebAuthnHeadlessAuthenticateFragment extends Fragment {

    public static final String TAG = WebAuthnHeadlessAuthenticateFragment.class.getName();
    public static final int REQUEST_FIDO2_SIGNIN = 2000;
    private FRListener<AuthenticatorAssertionResponse> listener;

    /**
     * Initialize the Fragment.
     *
     * @param fragmentManager The current FragmentManager
     * @param pendingIntent   The pending Intent for launching the Biometric Authentication
     * @return The Fragment to handle the Biometric Authentication
     */
    public static WebAuthnHeadlessAuthenticateFragment init(FragmentManager fragmentManager, PendingIntent pendingIntent) {

        WebAuthnHeadlessAuthenticateFragment existing = (WebAuthnHeadlessAuthenticateFragment) fragmentManager.findFragmentByTag(TAG);
        if (existing != null) {
            existing.listener = null;
            fragmentManager.beginTransaction().remove(existing).commitNow();
        }

        WebAuthnHeadlessAuthenticateFragment fragment = newInstance(pendingIntent);
        fragmentManager.beginTransaction().add(fragment, WebAuthnHeadlessAuthenticateFragment.TAG).commit();
        return fragment;
    }

    private static WebAuthnHeadlessAuthenticateFragment newInstance(PendingIntent pendingIntent) {
        WebAuthnHeadlessAuthenticateFragment fragment = new WebAuthnHeadlessAuthenticateFragment();
        Bundle args = new Bundle();
        args.putParcelable("PENDING_INTENT", pendingIntent);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        if (getArguments() != null) {
            PendingIntent pendingIntent = getArguments().getParcelable("PENDING_INTENT");
            if (pendingIntent != null) {
                try {
                    startIntentSenderForResult(pendingIntent.getIntentSender(),
                            REQUEST_FIDO2_SIGNIN,
                            null, 0, 0, 0, null);
                } catch (IntentSender.SendIntentException e) {
                    Listener.onException(listener, new WebAuthnException(e));
                }
            }
        }
    }

    /**
     * Set the listener to listen for result event.
     *
     * @param listener The listener
     */
    public void setListener(FRListener<AuthenticatorAssertionResponse> listener) {
        this.listener = listener;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_FIDO2_SIGNIN) {
            if (data == null) {
                Listener.onException(listener, new WebAuthnException("No Response Data"));
                return;
            }
            if (data.getByteArrayExtra(Fido.FIDO2_KEY_ERROR_EXTRA) != null) {
                AuthenticatorErrorResponse response = AuthenticatorErrorResponse.deserializeFromBytes(data.getByteArrayExtra(Fido.FIDO2_KEY_ERROR_EXTRA));
                Listener.onException(listener, new WebAuthnResponseException(response));
            } else {
                AuthenticatorAssertionResponse response =
                        AuthenticatorAssertionResponse.deserializeFromBytes(
                                data.getByteArrayExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA));
                Listener.onSuccess(listener, response);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
            Listener.onException(listener, new WebAuthnException("Unrecognized request code: " + requestCode));
        }
    }

}
