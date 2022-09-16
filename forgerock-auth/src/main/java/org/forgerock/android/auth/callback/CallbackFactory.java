/*
 * Copyright (c) 2019 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import org.forgerock.android.auth.Logger;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

/**
 * Factory to manage supported {@link Callback}
 */
public class CallbackFactory {

    private static final String TAG = CallbackFactory.class.getSimpleName();
    private static final CallbackFactory INSTANCE = new CallbackFactory();

    @Getter
    private Map<String, Class<? extends Callback>> callbacks = new HashMap<>();

    private CallbackFactory() {
        register(ChoiceCallback.class);
        register(NameCallback.class);
        register(PasswordCallback.class);
        register(StringAttributeInputCallback.class);
        register(NumberAttributeInputCallback.class);
        register(BooleanAttributeInputCallback.class);
        register(ValidatedPasswordCallback.class);
        register(ValidatedUsernameCallback.class);
        register(KbaCreateCallback.class);
        register(TermsAndConditionsCallback.class);
        register(PollingWaitCallback.class);
        register(ConfirmationCallback.class);
        register(TextOutputCallback.class);
        register(SuspendedTextOutputCallback.class);
        register(ReCaptchaCallback.class);
        register(ConsentMappingCallback.class);
        register(HiddenValueCallback.class);
        register(DeviceProfileCallback.class);
        register(MetadataCallback.class);
        register(WebAuthnRegistrationCallback.class);
        register(WebAuthnAuthenticationCallback.class);
        register(SelectIdPCallback.class);
        register(IdPCallback.class);
        register(DeviceBindingCallback.class);
        register(DeviceSigningVerifierCallback.class);
    }

    /**
     * Returns a cached instance {@link CallbackFactory}
     *
     * @return instance of {@link CallbackFactory}
     */
    public static CallbackFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Register new Callback Class
     *
     * @param callback The callback Class
     */
    public void register(Class<? extends Callback> callback) {
        try {
            callbacks.put(getType(callback), callback);
        } catch (Exception e) {
            Logger.error(TAG, e, e.getMessage());
        }
    }

    public String getType(Class<? extends Callback> callback) throws InstantiationException, IllegalAccessException {
        return callback.newInstance().getType();
    }

}
