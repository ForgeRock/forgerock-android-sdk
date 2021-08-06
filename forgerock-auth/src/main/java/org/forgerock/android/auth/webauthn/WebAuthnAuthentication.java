/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.webauthn;

import android.app.PendingIntent;
import android.content.Context;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.fido.Fido;
import com.google.android.gms.fido.fido2.Fido2ApiClient;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions;
import com.google.android.gms.tasks.Task;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Listener;
import org.forgerock.android.auth.WebAuthnDataRepository;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

/**
 * Handle WebAuthn Authentication
 */
public class WebAuthnAuthentication extends WebAuthn {

    protected final List<PublicKeyCredentialDescriptor> allowCredentials;
    protected final String relayingPartyId;
    protected final Double timeout;
    protected final byte[] challenge;
    //Keep it just for completeness, current user verification is required
    protected final String userVerification;

    /**
     * Constructor to create WebAuthnAuthentication
     *
     * @param input The json from WebAuthn Authentication Node
     * @throws JSONException Failed to parse the Json
     */
    public WebAuthnAuthentication(JSONObject input) throws JSONException {
        this.challenge = Base64.decode(input.getString(CHALLENGE), Base64.NO_WRAP);
        this.relayingPartyId = getRelyingPartyId(input);
        this.timeout = Double.parseDouble(input.optString(TIMEOUT, TIMEOUT_DEFAULT)) / 1000;
        this.allowCredentials = getAllowCredentials(input);
        this.userVerification = input.optString(USER_VERIFICATION, null);
    }

    /**
     * Parse and retrieve all the allow credentials
     *
     * @param value The json from WebAuthn Authentication Node
     * @return The parsed PublicKeyCredentialDescriptor
     * @throws JSONException Failed to parse the Json
     */
    protected List<PublicKeyCredentialDescriptor> getAllowCredentials(JSONObject value) throws JSONException {

        JSONArray allowCredentials = new JSONArray();
        if (value.has(_ALLOW_CREDENTIALS)) {
            allowCredentials = value.getJSONArray(_ALLOW_CREDENTIALS);
        } else if (value.has(ALLOW_CREDENTIALS)) {
            String allowCredentialString = value.getString(ALLOW_CREDENTIALS)
                    .replaceAll("(allowCredentials: |new Int8Array\\(|\\).buffer )", "");
            if (allowCredentialString.trim().length() > 0) {
                allowCredentials = new JSONArray(allowCredentialString);
            }
        }
        return getCredentials(allowCredentials);

    }

    /**
     * Perform WebAuthn Authentication
     *
     * @param context             The Application Context
     * @param fragmentManager     The FragmentManager to manage the lifecycle of Fido API Callback
     * @param webAuthnKeySelector The Selector for user to select which credential to use (UsernameLess)
     * @param listener            The Listener for the result event.
     */
    public void authenticate(@NonNull Context context,
                             @NonNull FragmentManager fragmentManager,
                             @Nullable WebAuthnKeySelector webAuthnKeySelector,
                             @NonNull WebAuthnListener listener) {

        //username less when allowCredentials is empty
        if (allowCredentials.isEmpty()) {
            List<PublicKeyCredentialSource> publicKeyCredentialSources = getPublicKeyCredentialSource(context);
            if (publicKeyCredentialSources.isEmpty()) {
                authenticate(context, fragmentManager, listener, allowCredentials, null);
                return;
            }
            //When there is only one stored credential, automatically trigger with the stored credential.
            if (publicKeyCredentialSources.size() == 1) {
                authenticate(context, fragmentManager,
                        listener,
                        Collections.singletonList(publicKeyCredentialSources.get(0).toDescriptor()),
                        publicKeyCredentialSources.get(0).getUserHandle());
            } else {
                //Launch a dialog and ask for which user for authentication
                FRListener<PublicKeyCredentialSource> selectedListener = new FRListener<PublicKeyCredentialSource>() {
                    @Override
                    public void onSuccess(PublicKeyCredentialSource result) {
                        authenticate(context, fragmentManager,
                                listener,
                                Collections.singletonList(result.toDescriptor()),
                                result.getUserHandle());
                    }

                    @Override
                    public void onException(Exception e) {
                        Listener.onException(listener, e);
                    }
                };
                if (webAuthnKeySelector == null) {
                    //Use the default Fragment Dialog
                    webAuthnKeySelector = WebAuthnKeySelector.DEFAULT;
                }
                webAuthnKeySelector.select(fragmentManager, publicKeyCredentialSources, selectedListener);
            }
        } else {
            authenticate(context, fragmentManager, listener, allowCredentials, null);
        }
    }

    /**
     * Retrieve the {@link PublicKeyCredentialSource}
     *
     * @param context The Application Context
     * @return The stored {@link PublicKeyCredentialSource}
     */
    protected List<PublicKeyCredentialSource> getPublicKeyCredentialSource(Context context) {
        return WebAuthnDataRepository.builder()
                .context(context).build()
                .getPublicKeyCredentialSource(relayingPartyId);
    }

    protected Task<PendingIntent> getSignPendingIntent(Fido2ApiClient fido2ApiClient, PublicKeyCredentialRequestOptions options) {
        return fido2ApiClient.getSignPendingIntent(options);
    }

    protected void authenticate(Context context, FragmentManager fragmentManager,
                                WebAuthnListener listener,
                                List<PublicKeyCredentialDescriptor> allowCredentials,
                                byte[] userHandle) {

        Fido2ApiClient fido2ApiClient = Fido.getFido2ApiClient(context);
        PublicKeyCredentialRequestOptions options = new PublicKeyCredentialRequestOptions.Builder()
                .setRpId(relayingPartyId)
                .setChallenge(challenge)
                .setAllowList(allowCredentials)
                .setTimeoutSeconds(timeout)
                .build();

        Task<PendingIntent> task = getSignPendingIntent(fido2ApiClient, options);
        task.addOnSuccessListener(pendingIntent -> {
            WebAuthnHeadlessAuthenticateFragment fido2HeadlessFragment =
                    WebAuthnHeadlessAuthenticateFragment.init(fragmentManager, pendingIntent);
            fido2HeadlessFragment.setListener(new FRListener<AuthenticatorAssertionResponse>() {
                @Override
                public void onSuccess(AuthenticatorAssertionResponse result) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(new String(result.getClientDataJSON()));
                    sb.append("::");
                    sb.append(format(result.getAuthenticatorData()));
                    sb.append("::");
                    sb.append(format(result.getSignature()));
                    sb.append("::");
                    sb.append(Base64.encodeToString(result.getKeyHandle(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING));
                    if (userHandle != null) {
                        sb.append("::");
                        sb.append(Base64.encodeToString(userHandle, Base64.URL_SAFE | Base64.NO_WRAP));
                    } else {
                        if (result.getUserHandle() != null) {
                            sb.append("::");
                            sb.append(Base64.encodeToString(result.getUserHandle(), Base64.URL_SAFE | Base64.NO_WRAP));
                        }
                    }
                    Listener.onSuccess(listener, sb.toString());
                }

                @Override
                public void onException(Exception e) {
                    onWebAuthnException(listener, e);
                }
            });
        }).addOnFailureListener(e -> onWebAuthnException(listener, e));
    }

}
