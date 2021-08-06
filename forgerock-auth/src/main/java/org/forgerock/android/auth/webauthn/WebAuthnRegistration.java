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
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.fido.Fido;
import com.google.android.gms.fido.fido2.Fido2ApiClient;
import com.google.android.gms.fido.fido2.api.common.Attachment;
import com.google.android.gms.fido.fido2.api.common.AttestationConveyancePreference;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse;
import com.google.android.gms.fido.fido2.api.common.AuthenticatorSelectionCriteria;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialParameters;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRpEntity;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity;
import com.google.android.gms.tasks.Task;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Listener;
import org.forgerock.android.auth.WebAuthnDataRepository;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Handle WebAuthn Registration
 */
public class WebAuthnRegistration extends WebAuthn {

    protected final String relyingPartyName;
    protected final AttestationConveyancePreference attestationPreference;
    protected final String displayName;
    protected final String relyingPartyId;
    protected final String userName;
    protected final AuthenticatorSelectionCriteria authenticatorSelection;
    protected final boolean requireResidentKey;
    protected final String userId;
    protected final Double timeout;
    protected final List<PublicKeyCredentialDescriptor> excludeCredentials;
    protected final List<PublicKeyCredentialParameters> pubKeyCredParams;
    protected final byte[] challenge;

    /**
     * Constructor to create WebAuthnRegistration
     *
     * @param input The json from WebAuthn Registration Node
     * @throws JSONException Failed to parse the Json
     */
    public WebAuthnRegistration(JSONObject input) throws JSONException, UnsupportedOperationException {
        try {
            this.challenge = Base64.decode(input.getString(CHALLENGE), Base64.NO_WRAP);
            this.attestationPreference = AttestationConveyancePreference.fromString(input.optString(ATTESTATION_PREFERENCE, "none"));
            this.userName = input.optString(USER_NAME);
            this.userId = input.optString(USER_ID);
            this.relyingPartyName = input.optString(RELYING_PARTY_NAME);
            this.authenticatorSelection = getAuthenticatorSelectionCriteria(input);
            this.requireResidentKey = isRequireResidentKey(input);
            this.pubKeyCredParams = getPublicKeyCredentialParameters(input);
            this.timeout = Double.parseDouble(input.optString(TIMEOUT, TIMEOUT_DEFAULT)) / 1000;
            this.excludeCredentials = getExcludeCredentials(input);
            this.displayName = input.optString(DISPLAY_NAME);
            this.relyingPartyId = getRelyingPartyId(input);

        } catch (AttestationConveyancePreference.UnsupportedAttestationConveyancePreferenceException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    /**
     * Parse and retrieve all the Public key credentials
     *
     * @param value The json from WebAuthn Registration Node
     * @return The parsed PublicKeyCredentialParameters
     * @throws JSONException Failed to parse the Json
     */
    protected List<PublicKeyCredentialParameters> getPublicKeyCredentialParameters(JSONObject value) throws JSONException {
        List<PublicKeyCredentialParameters> result = new ArrayList<>();

        JSONArray pubKeyCredParams;
        if (value.has(_PUB_KEY_CRED_PARAMS)) {
            pubKeyCredParams = value.getJSONArray(_PUB_KEY_CRED_PARAMS);
        } else {
            pubKeyCredParams = new JSONArray(value.getString(PUB_KEY_CRED_PARAMS));
        }

        for (int i = 0; i < pubKeyCredParams.length(); i++) {
            JSONObject o = pubKeyCredParams.getJSONObject(i);
            result.add(new PublicKeyCredentialParameters(o.getString(TYPE),
                    o.getInt(ALG)));

        }
        return result;
    }

    /**
     * Parse and retrieve requiredResidentKey attribute
     *
     * @param value The json from WebAuthn Registration Node
     * @return The parsed requiredResidentKey.
     * @throws JSONException Failed to parse the Json
     */
    protected boolean isRequireResidentKey(JSONObject value) throws JSONException {
        JSONObject authenticatorSelection;
        if (value.has(_AUTHENTICATOR_SELECTION)) {
            authenticatorSelection = value.getJSONObject(_AUTHENTICATOR_SELECTION);
        } else {
            authenticatorSelection = new JSONObject(value.getString(AUTHENTICATOR_SELECTION));
        }

        if (authenticatorSelection.has(REQUIRE_RESIDENT_KEY)) {
            return authenticatorSelection.getBoolean(REQUIRE_RESIDENT_KEY);
        }
        return false;
    }

    /**
     * Parse and retrieve AuthenticatorSelectionCriteria attribute
     *
     * @param value The json from WebAuthn Registration Node
     * @return The parsed AuthenticatorSelectionCriteria.
     * @throws JSONException Failed to parse the Json
     */
    protected AuthenticatorSelectionCriteria getAuthenticatorSelectionCriteria(JSONObject value) throws JSONException {
        JSONObject authenticatorSelection;
        if (value.has(_AUTHENTICATOR_SELECTION)) {
            authenticatorSelection = value.getJSONObject(_AUTHENTICATOR_SELECTION);
        } else {
            authenticatorSelection = new JSONObject(value.getString(AUTHENTICATOR_SELECTION));
        }

        if (authenticatorSelection.has(AUTHENTICATOR_ATTACHMENT)) {
            try {
                Attachment attachment = Attachment.fromString(authenticatorSelection.getString(AUTHENTICATOR_ATTACHMENT));
                if (attachment == Attachment.CROSS_PLATFORM) {
                    throw new UnsupportedOperationException("Cross Platform attachment is not supported");
                }
                return new AuthenticatorSelectionCriteria.Builder()
                        .setAttachment(attachment)
                        .build();
            } catch (Attachment.UnsupportedAttachmentException e) {
                throw new UnsupportedOperationException(e);
            }
        } else {
            return new AuthenticatorSelectionCriteria.Builder()
                    .setAttachment(Attachment.PLATFORM)
                    .build();
        }
    }

    /**
     * Parse and retrieve PublicKeyCredentialDescriptor attribute
     *
     * @param value The json from WebAuthn Registration Node
     * @return The parsed PublicKeyCredentialDescriptor.
     * @throws JSONException Failed to parse the Json
     */
    protected List<PublicKeyCredentialDescriptor> getExcludeCredentials(JSONObject value) throws JSONException {

        JSONArray excludeCredentials;
        if (value.has(_EXCLUDE_CREDENTIALS)) {
            excludeCredentials = value.getJSONArray(_EXCLUDE_CREDENTIALS);
        } else {
            String excludeCredentialString = ("[" + value.optString(EXCLUDE_CREDENTIALS, "") + "]")
                    .replaceAll("(new Int8Array\\(|\\).buffer )", "");
            excludeCredentials = new JSONArray(excludeCredentialString);
        }

        return getCredentials(excludeCredentials);

    }

    @VisibleForTesting
    protected Task<PendingIntent> getRegisterPendingIntent(Fido2ApiClient fido2ApiClient,
                                                           PublicKeyCredentialCreationOptions options) {
        return fido2ApiClient.getRegisterPendingIntent(options);
    }


    /**
     * Perform WebAuthn Registration
     *
     * @param context         The Application Context
     * @param fragmentManager The FragmentManager to manage the lifecycle of Fido API Callback
     * @param listener        The Listener for the result event.
     */
    public void register(@NonNull Context context,
                         @NonNull FragmentManager fragmentManager,
                         @NonNull WebAuthnListener listener) {

        Fido2ApiClient fido2ApiClient = Fido.getFido2ApiClient(context);
        PublicKeyCredentialCreationOptions options = null;
        options = new PublicKeyCredentialCreationOptions.Builder()
                .setRp(new PublicKeyCredentialRpEntity(relyingPartyId, relyingPartyName, null))
                .setAttestationConveyancePreference(attestationPreference)
                .setUser(new PublicKeyCredentialUserEntity(userId.getBytes(), userName, null, displayName))
                .setChallenge(challenge)
                .setTimeoutSeconds(timeout)
                .setAuthenticatorSelection(authenticatorSelection)
                .setExcludeList(excludeCredentials)
                .setParameters(pubKeyCredParams)
                .build();

        Task<PendingIntent> fido2PendingIntent = getRegisterPendingIntent(fido2ApiClient, options);
        fido2PendingIntent.addOnSuccessListener(pendingIntent -> {
            WebAuthnHeadlessRegistrationFragment webAuthnHeadlessRegistrationFragment = WebAuthnHeadlessRegistrationFragment.init(fragmentManager, pendingIntent);
            webAuthnHeadlessRegistrationFragment.setListener(new FRListener<AuthenticatorAttestationResponse>() {
                @Override
                public void onSuccess(AuthenticatorAttestationResponse result) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(new String(result.getClientDataJSON()));
                    sb.append("::");
                    sb.append(format(result.getAttestationObject()));
                    sb.append("::");
                    sb.append(Base64.encodeToString(result.getKeyHandle(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING));

                    //Extension to support username-less
                    if (requireResidentKey) {
                        PublicKeyCredentialSource source = PublicKeyCredentialSource.builder()
                                .id(result.getKeyHandle())
                                .rpid(relyingPartyId)
                                .userHandle(Base64.decode(userId, Base64.URL_SAFE | Base64.NO_WRAP))
                                .otherUI(displayName).build();
                        persist(context, source);
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

    /**
     * Persist the {@link PublicKeyCredentialSource}
     *
     * @param context The Application context
     * @param source  The {@link PublicKeyCredentialSource} to persist
     */
    protected void persist(Context context, PublicKeyCredentialSource source) {
        WebAuthnDataRepository.builder().context(context).build().persist(source);
    }

}
