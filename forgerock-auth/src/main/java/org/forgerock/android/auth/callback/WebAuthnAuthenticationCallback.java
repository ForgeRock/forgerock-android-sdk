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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.InitProvider;
import org.forgerock.android.auth.Listener;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.exception.WebAuthnResponseException;
import org.forgerock.android.auth.webauthn.WebAuthn;
import org.forgerock.android.auth.webauthn.WebAuthnAuthentication;
import org.forgerock.android.auth.webauthn.WebAuthnKeySelector;
import org.forgerock.android.auth.webauthn.WebAuthnListener;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;
import lombok.NoArgsConstructor;

import static org.forgerock.android.auth.webauthn.WebAuthn.PUB_KEY_CRED_PARAMS;
import static org.forgerock.android.auth.webauthn.WebAuthn.WEBAUTHN_AUTHENTICATION;
import static org.forgerock.android.auth.webauthn.WebAuthn._ACTION;
import static org.forgerock.android.auth.webauthn.WebAuthn._PUB_KEY_CRED_PARAMS;
import static org.forgerock.android.auth.webauthn.WebAuthn._TYPE;

/**
 * A callback that handle WebAuthnAuthentication Node.
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
 *                         "_action": "webauthn_authentication",
 *                         "challenge": "6gckyPcs5bDIstu08mgtl+1Yr2g6N/KRJoCNkGXM2wY=",
 *                         "allowCredentials": "allowCredentials: [{ \"type\": \"public-key\", \"id\": new Int8Array([1, -91, -21, 122, -16, 124, 2, 23, 116, 47, 7, 38, 115, -67, 30, 80, -91, 56, -50, 106, 85, 55, -126, -119, -110, 124, 117, 61, 29, -103, 115, -37, 6, 91, -45, 58, -34, 67, 21, 14, -72, 81, -119, 15, 70, 13, 48, -114, -57, 8, -67, 2, -20, 18, -62, 106, -76, 66, 6, 13, 23, -48, -58, -78, 105]).buffer }]",
 *                         "_allowCredentials": [
 *                             {
 *                                 "type": "public-key",
 *                                 "id": [
 *                                     1,
 *                                     -91,
 *                                     -21,
 *                                     122,
 *                                     -16,
 *                                     124,
 *                                     2,
 *                                     23,
 *                                     116,
 *                                     47,
 *                                     7,
 *                                     38,
 *                                     115,
 *                                     -67,
 *                                     30,
 *                                     80,
 *                                     -91,
 *                                     56,
 *                                     -50,
 *                                     106,
 *                                     85,
 *                                     55,
 *                                     -126,
 *                                     -119,
 *                                     -110,
 *                                     124,
 *                                     117,
 *                                     61,
 *                                     29,
 *                                     -103,
 *                                     115,
 *                                     -37,
 *                                     6,
 *                                     91,
 *                                     -45,
 *                                     58,
 *                                     -34,
 *                                     67,
 *                                     21,
 *                                     14,
 *                                     -72,
 *                                     81,
 *                                     -119,
 *                                     15,
 *                                     70,
 *                                     13,
 *                                     48,
 *                                     -114,
 *                                     -57,
 *                                     8,
 *                                     -67,
 *                                     2,
 *                                     -20,
 *                                     18,
 *                                     -62,
 *                                     106,
 *                                     -76,
 *                                     66,
 *                                     6,
 *                                     13,
 *                                     23,
 *                                     -48,
 *                                     -58,
 *                                     -78,
 *                                     105
 *                                 ]
 *                             }
 *                         ],
 *                         "timeout": "60000",
 *                         "userVerification": "preferred",
 *                         "relyingPartyId": "rpId: \"humorous-cuddly-carrot.glitch.me\",",
 *                         "_relyingPartyId": "humorous-cuddly-carrot.glitch.me",
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
public class WebAuthnAuthenticationCallback extends MetadataCallback implements WebAuthnCallback {

    @Keep
    public WebAuthnAuthenticationCallback(JSONObject jsonObject, int index) {
        super(jsonObject, index);
    }

    /**
     * Check if this callback is {@link WebAuthnAuthenticationCallback} Type
     *
     * @param value The callback raw data json.
     * @return True if this is a {@link WebAuthnAuthenticationCallback} Type, else false
     */
    public static boolean instanceOf(JSONObject value) {
        //_action is provided AM version >= AM 7.1
        if (value.has(_ACTION)) {
            try {
                if (value.getString(_ACTION).equals(WEBAUTHN_AUTHENTICATION)) {
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
                    (!value.has(PUB_KEY_CRED_PARAMS) && !value.has(_PUB_KEY_CRED_PARAMS)));
        } catch (JSONException e) {
            return false;
        }
    }

    @Override
    public String getType() {
        return "WebAuthnAuthenticationCallback";
    }

    /**
     * Perform WebAuthn Authentication
     *
     * @param fragment The current Fragment that handle this callback.
     * @param node     The Node returned from AM.
     * @param selector The selector to select which credential key to use. Apply to Username-less only.
     * @param listener Listener
     */
    public void authenticate(@NonNull Fragment fragment, @NonNull Node node,
                             @Nullable WebAuthnKeySelector selector,
                             FRListener<Void> listener) {
        authenticate(fragment.getContext(),
                fragment.getFragmentManager(), node, selector, listener);
    }

    /**
     * Perform WebAuthn Authentication with the current Activity, the Activity has to be of type
     * {@link FragmentActivity}
     *
     * @param node             The Node returned from AM
     * @param selector         The selector to select which credential key to use. Apply to Username-less only.
     * @param listener         Listener
     */
    public void authenticate(@NonNull Node node,
                             @Nullable WebAuthnKeySelector selector,
                             FRListener<Void> listener) {
        FragmentActivity fragmentActivity = InitProvider.getCurrentActivityAsFragmentActivity();
        authenticate(fragmentActivity.getApplicationContext(),
                fragmentActivity.getSupportFragmentManager(), node, selector, listener);
    }

    protected WebAuthnAuthentication getWebAuthnAuthentication() throws JSONException {
        return new WebAuthnAuthentication(getValue());
    }

    private void authenticate(@NonNull Context context, @NonNull FragmentManager fragmentManager,
                              @NonNull Node node, WebAuthnKeySelector selector,
                              FRListener<Void> listener) {
        try {
            if (selector == null) {
                selector = WebAuthnKeySelector.DEFAULT;
            }
            getWebAuthnAuthentication().authenticate(context, fragmentManager,
                    selector, new WebAuthnListener() {
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
