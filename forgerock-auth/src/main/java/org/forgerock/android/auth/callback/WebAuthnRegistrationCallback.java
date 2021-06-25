/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import android.annotation.TargetApi;
import android.content.Context;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.InitProvider;
import org.forgerock.android.auth.Listener;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.exception.WebAuthnResponseException;
import org.forgerock.android.auth.webauthn.WebAuthn;
import org.forgerock.android.auth.webauthn.WebAuthnListener;
import org.forgerock.android.auth.webauthn.WebAuthnRegistration;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;
import lombok.NoArgsConstructor;

import static org.forgerock.android.auth.webauthn.WebAuthn.PUB_KEY_CRED_PARAMS;
import static org.forgerock.android.auth.webauthn.WebAuthn.WEBAUTHN_REGISTRATION;
import static org.forgerock.android.auth.webauthn.WebAuthn._ACTION;
import static org.forgerock.android.auth.webauthn.WebAuthn._PUB_KEY_CRED_PARAMS;
import static org.forgerock.android.auth.webauthn.WebAuthn._TYPE;

/**
 * A callback that allows some extra metadata to be sent in the response.
 * <p>
 * When serialized as JSON in an authenticate response, the {@link #getValue()} object will be the value of a single
 * {@literal data} output value, so for a value of {@code { "foo": "bar" }}, this would be output as:
 * <pre>
 * {@code
 * {
 *     "authId": "...",
 *     "callbacks": [
 *         // ...
 *         {
 *             "type": "MetadataCallback",
 *             "output": [
 *                 {
 *                     "name": "data",
 *                     "value": {
 *                         "_action": "webauthn_registration",
 *                         "challenge": "vlJwjd3nd76zjNfav2km/FKOgcIRa4BNHdVyasnPSb4=",
 *                         "attestationPreference": "none",
 *                         "userName": "e24f0d7c-a9d5-4a3f-a002-6f808210a8a3",
 *                         "userId": "ZTI0ZjBkN2MtYTlkNS00YTNmLWEwMDItNmY4MDgyMTBhOGEz",
 *                         "relyingPartyName": "ForgeRock",
 *                         "authenticatorSelection": "{\"userVerification\":\"preferred\",\"authenticatorAttachment\":\"platform\"}",
 *                         "_authenticatorSelection": {
 *                             "userVerification": "preferred",
 *                             "authenticatorAttachment": "platform"
 *                         },
 *                         "pubKeyCredParams": "[ { \"type\": \"public-key\", \"alg\": -7 }, { \"type\": \"public-key\", \"alg\": -257 } ]",
 *                         "_pubKeyCredParams": [
 *                             {
 *                                 "type": "public-key",
 *                                 "alg": -7
 *                             },
 *                             {
 *                                 "type": "public-key",
 *                                 "alg": -257
 *                             }
 *                         ],
 *                         "timeout": "60000",
 *                         "excludeCredentials": "",
 *                         "_excludeCredentials": [],
 *                         "displayName": "e24f0d7c-a9d5-4a3f-a002-6f808210a8a3",
 *                         "relyingPartyId": "id: \"openam.example.com\",",
 *                         "_relyingPartyId": "openam.example.com",
 *                         "_type": "WebAuthn"
 *                     }
 *                 }
 *             ]
 *         }
 *     ]
 * }
 * </pre>
 */
@NoArgsConstructor
@Getter
@TargetApi(24)
public class WebAuthnRegistrationCallback extends MetadataCallback implements WebAuthnCallback {

    @Keep
    public WebAuthnRegistrationCallback(JSONObject jsonObject, int index) {
        super(jsonObject, index);
    }

    /**
     * Check if this callback is {@link WebAuthnRegistrationCallback} Type
     *
     * @param value The callback raw data json.
     * @return True if this is a {@link WebAuthnRegistrationCallback} Type, else false
     */
    public static boolean instanceOf(JSONObject value) {
        //_action is provided AM version >= AM 7.1
        if (value.has(_ACTION)) {
            try {
                if (value.getString(_ACTION).equals(WEBAUTHN_REGISTRATION)) {
                    return true;
                }
            } catch (JSONException e) {
                //Should not happened
                return false;
            }
        }
        try {
            return (value.has(_TYPE) &&
                    value.getString(_TYPE).equals(WebAuthn.WEB_AUTHN) &&
                    (value.has(PUB_KEY_CRED_PARAMS) || value.has(_PUB_KEY_CRED_PARAMS)));
        } catch (JSONException e) {
            return false;
        }
    }

    @Override
    public String getType() {
        return "WebAuthnRegistrationCallback";
    }

    /**
     * Perform WebAuthn Registration.
     *
     * @param fragment The current {@link Fragment} that handle this callback
     * @param node     The Node returned from AM
     * @param listener Listener to listen for WebAuthn Registration Event
     */
    public void register(@NonNull Fragment fragment, Node node,
                         FRListener<Void> listener) {
        register(fragment.getContext(), fragment.getFragmentManager(), node, listener);
    }

    /**
     * Perform WebAuthn Registration with the current Activity, the Activity has to be of type
     * {@link FragmentActivity}
     *
     * @param node     The Node returned from AM
     * @param listener Listener to listen for WebAuthn Registration Event
     */
    public void register(Node node,
                         FRListener<Void> listener) {
        FragmentActivity fragmentActivity = InitProvider.getCurrentActivityAsFragmentActivity();
        register(fragmentActivity.getApplicationContext(), fragmentActivity.getSupportFragmentManager(), node, listener);
    }

    protected WebAuthnRegistration getWebAuthnRegistration() throws JSONException {
        return new WebAuthnRegistration(getValue());
    }

    private void register(@NonNull Context context, @NonNull FragmentManager fragmentManager,
                          @NonNull Node node, FRListener<Void> listener) {
        try {
            getWebAuthnRegistration().register(context,
                    fragmentManager,
                    new WebAuthnListener() {
                        @Override
                        public void onSuccess(String result) {
                            setHiddenCallbackValue(node, result);
                            Listener.onSuccess(listener, null);
                        }

                        @Override
                        public void onException(WebAuthnResponseException e) {
                            setHiddenCallbackValue(node, e.toServerError());
                            Listener.onException(listener, e);
                        }

                        @Override
                        public void onUnsupported(WebAuthnResponseException e) {
                            setHiddenCallbackValue(node, "unsupported");
                            Listener.onException(listener, e);
                        }

                        @Override
                        public void onException(Exception e) {
                            setHiddenCallbackValue(node, "ERROR::UnknownError:" + e.getMessage());
                            Listener.onException(listener, e);
                        }
                    });
        } catch (UnsupportedOperationException e) {
            setHiddenCallbackValue(node, "unsupported");
            Listener.onException(listener, e);
        } catch (Exception e) {
            Listener.onException(listener, e);
        }
    }
}
