/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import android.content.Context;

import androidx.annotation.Keep;

import com.google.android.gms.safetynet.SafetyNet;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Listener;
import org.json.JSONObject;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Callback designed for usage with the ReCaptchaNode.
 */
@NoArgsConstructor
@Getter
public class ReCaptchaCallback extends AbstractCallback {

    /**
     * Retrieves the specified site key.
     *
     * @return the site key.
     */
    private String reCaptchaSiteKey;

    /**
     * Constructor that creates a {@link ReCaptchaCallback}.
     */
    @Keep
    public ReCaptchaCallback(JSONObject raw, int index) {
        super(raw, index);
    }

    @Override
    protected void setAttribute(String name, Object value) {
        if ("recaptchaSiteKey".equals(name)) {
            this.reCaptchaSiteKey = (String) value;
        }
    }

    /**
     * Proceed to trigger the ReCAPTCHA
     *
     * @param context  The Application Context
     * @param listener Listener to lister for ReCAPTCHA result event.
     */
    public void proceed(Context context, FRListener<Void> listener) {
        SafetyNet.getClient(context).verifyWithRecaptcha(reCaptchaSiteKey)
                .addOnSuccessListener(
                        response -> {
                            String userResponseToken = response.getTokenResult();
                            if (!userResponseToken.isEmpty()) {
                                setValue(userResponseToken);
                            }
                            Listener.onSuccess(listener, null);
                        })
                .addOnFailureListener(e -> Listener.onException(listener, e));
    }

    @Override
    public String getType() {
        return "ReCaptchaCallback";
    }


}
