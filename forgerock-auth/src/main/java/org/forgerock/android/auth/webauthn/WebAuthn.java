/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.webauthn;

import com.google.android.gms.fido.fido2.api.common.ErrorCode;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType;

import org.forgerock.android.auth.exception.WebAuthnResponseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.android.gms.fido.common.Transport.INTERNAL;

/**
 * Abstract class to provide common utilities method for {@link WebAuthnAuthentication} and
 * {@link WebAuthnRegistration}
 */
public abstract class WebAuthn {

    public static final String _ACTION = "_action";
    public static final String CHALLENGE = "challenge";
    public static final String TIMEOUT = "timeout";
    public static final String _ALLOW_CREDENTIALS = "_allowCredentials";
    public static final String ALLOW_CREDENTIALS = "allowCredentials";
    public static final String USER_VERIFICATION = "userVerification";
    public static final String ATTESTATION_PREFERENCE = "attestationPreference";
    public static final String USER_NAME = "userName";
    public static final String USER_ID = "userId";
    public static final String RELYING_PARTY_NAME = "relyingPartyName";
    public static final String DISPLAY_NAME = "displayName";
    public static final String _PUB_KEY_CRED_PARAMS = "_pubKeyCredParams";
    public static final String PUB_KEY_CRED_PARAMS = "pubKeyCredParams";
    public static final String TYPE = "type";
    public static final String ALG = "alg";
    public static final String _AUTHENTICATOR_SELECTION = "_authenticatorSelection";
    public static final String AUTHENTICATOR_SELECTION = "authenticatorSelection";
    public static final String REQUIRED = "required";
    public static final String REQUIRE_RESIDENT_KEY = "requireResidentKey";
    public static final String AUTHENTICATOR_ATTACHMENT = "authenticatorAttachment";
    public static final String _EXCLUDE_CREDENTIALS = "_excludeCredentials";
    public static final String EXCLUDE_CREDENTIALS = "excludeCredentials";
    public static final String TIMEOUT_DEFAULT = "60000";
    public static final String WEB_AUTHN = "WebAuthn";
    public static final String WEBAUTHN_REGISTRATION = "webauthn_registration";
    public static final String WEBAUTHN_AUTHENTICATION = "webauthn_authentication";
    public static final String _TYPE = "_type";

    /**
     * Parse the relaying party id.
     *
     * @param value The json value to parse
     * @return The relaying party id.
     * @throws JSONException Failed to parse the json input.
     */
    protected String getRelyingPartyId(JSONObject value) throws JSONException {
        if (value.has("_relyingPartyId")) {
            return value.getString("_relyingPartyId");
        } else {
            return value.getString("relyingPartyId")
                    .replaceAll("(rpId: |\"|,)", "")
                    .replaceAll("(id: |\"|,)", "");
        }
    }

    /**
     * Format the bytes array to string.
     *
     * @param bytes The input bytes array
     * @return The string representation of the bytes array.
     */
    String format(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte aByte : bytes) {
            result.append(aByte).append(",");
        }
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    /**
     * Parse the {@link PublicKeyCredentialDescriptor}
     *
     * @param credentials The json array value to parse.
     * @return The list of {@link PublicKeyCredentialDescriptor}
     * @throws JSONException Failed to parse the json input.
     */
    protected List<PublicKeyCredentialDescriptor> getCredentials(JSONArray credentials) throws JSONException {

        List<PublicKeyCredentialDescriptor> result = new ArrayList<>();

        for (int i = 0; i < credentials.length(); i++) {
            JSONObject excludeCredential = credentials.getJSONObject(i);
            String type = excludeCredential.getString("type");
            JSONArray id = excludeCredential.getJSONArray("id");
            byte[] bytes = new byte[id.length()];
            for (int j = 0; j < id.length(); j++) {
                bytes[j] = (byte) id.getInt(j);
            }
            try {
                PublicKeyCredentialDescriptor descriptor = new PublicKeyCredentialDescriptor(
                        PublicKeyCredentialType.fromString(type).toString(),
                        bytes,
                        Collections.singletonList(INTERNAL));
                result.add(descriptor);
            } catch (PublicKeyCredentialType.UnsupportedPublicKeyCredTypeException e) {
                throw new UnsupportedOperationException(e);
            }
        }
        return result;
    }

    /**
     * Transform the exception and invoke {@link WebAuthnListener#onException(WebAuthnResponseException)} or
     * {@link WebAuthnListener#onUnsupported(WebAuthnResponseException)}
     *
     * @param listener Listener to listen for exception event.
     * @param e        The exception
     */
    protected void onWebAuthnException(WebAuthnListener listener, Exception e) {
        if (listener != null) {
            try {
                throw e;
            } catch (WebAuthnResponseException webAuthnResponseException) {
                if (webAuthnResponseException.getErrorCode() == ErrorCode.NOT_SUPPORTED_ERR) {
                    listener.onUnsupported(webAuthnResponseException);
                } else {
                    listener.onException(webAuthnResponseException);
                }
            } catch (Exception exception) {
                listener.onException(e);
            }
        }
    }

}
