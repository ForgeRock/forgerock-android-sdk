/*
 * Copyright (c) 2019 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.callback.*;
import org.forgerock.android.auth.ui.callback.*;
import org.forgerock.android.auth.ui.page.OneTimePasswordPageFragment;
import org.forgerock.android.auth.ui.page.SecondFactorChoicePageFragment;
import org.forgerock.android.auth.ui.page.UsernamePasswordPageFragment;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory to create {@link Fragment} for {@link org.forgerock.android.auth.NodeListener#onCallbackReceived(Node)}
 */
public class CallbackFragmentFactory {

    private static final String TAG = CallbackFragmentFactory.class.getSimpleName();

    public static final String NODE = "NODE";
    public static final String CALLBACK = "CALLBACK";
    private static final CallbackFragmentFactory INSTANCE = new CallbackFragmentFactory();
    private final Map<String, Class<? extends Fragment>> fragments = new HashMap<>();


    private CallbackFragmentFactory() {
        //Page Callback
        register("UsernamePassword", UsernamePasswordPageFragment.class);
        register("SecondFactorChoice", SecondFactorChoicePageFragment.class);
        register("OneTimePassword", OneTimePasswordPageFragment.class);

        //Callback
        register(ChoiceCallback.class, ChoiceCallbackFragment.class);
        register(PasswordCallback.class, PasswordCallbackFragment.class);
        register(NameCallback.class, NameCallbackFragment.class);
        register(ValidatedUsernameCallback.class, ValidatedUsernameCallbackFragment.class);
        register(ValidatedPasswordCallback.class, ValidatedPasswordCallbackFragment.class);
        register(StringAttributeInputCallback.class, StringAttributeInputCallbackFragment.class);
        register(BooleanAttributeInputCallback.class, BooleanAttributeInputCallbackFragment.class);
        register(NumberAttributeInputCallback.class, NumberAttributeInputCallbackFragment.class);
        register(KbaCreateCallback.class, KbaCreateCallbackFragment.class);
        register(TermsAndConditionsCallback.class, TermsAndConditionsCallbackFragment.class);
        register(PollingWaitCallback.class, PollingWaitCallbackFragment.class);
        register(ConfirmationCallback.class, ConfirmationCallbackFragment.class);
        register(TextOutputCallback.class, TextOutputCallbackFragment.class);
        register(SuspendedTextOutputCallback.class, SuspendedTextOutputCallbackFragment.class);
        register(ReCaptchaCallback.class, ReCaptchaCallbackFragment.class);
        register(ConsentMappingCallback.class, ConsentMappingCallbackFragment.class);
        register(DeviceProfileCallback.class, DeviceProfileCallbackFragment.class);
        register(DeviceBindingCallback.class, DeviceBindingCallbackFragment.class);
        register(DeviceSigningVerifierCallback.class, DeviceSigningVerifierCallbackFragment.class);
        register(WebAuthnRegistrationCallback.class, WebAuthnRegistrationCallbackFragment.class);
        register(WebAuthnAuthenticationCallback.class, WebAuthnAuthenticationCallbackFragment.class);
        register(SelectIdPCallback.class, SelectIdPCallbackFragment.class);
        register(IdPCallback.class, IdPCallbackFragment.class);
    }

    public static CallbackFragmentFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Retrieve the Fragment that represent the {@link Node}
     *
     * @param node The Node returned from {@link org.forgerock.android.auth.NodeListener#onCallbackReceived(Node)}
     * @return The Fragment
     */
    public Fragment getFragment(Node node) {

        String stage = node.getStage();
        if (stage != null) {
            Class<? extends Fragment> clazz = fragments.get(stage);
            if (clazz != null) {
                Fragment fragment = newInstance(clazz);
                Bundle args = new Bundle();
                args.putSerializable(NODE, node);
                fragment.setArguments(args);
                return fragment;
            }
        }
        Fragment fragment = new AdaptiveCallbackFragment();
        Bundle args = new Bundle();
        args.putSerializable(NODE, node);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Retrieve the Fragment that represent the {@link Callback}
     *
     * @param callback The Callback
     * @return The Fragment or null if fragment is not defined.
     */
    public Fragment getFragment(Callback callback) {

        Class<? extends Fragment> clazz = fragments.get(callback.getType());
        if (clazz != null) {
            Fragment fragment = newInstance(clazz);
            Bundle args = new Bundle();
            args.putSerializable(CALLBACK, callback);
            fragment.setArguments(args);
            return fragment;
        }
        return null;
    }

    /**
     * Retrieve the Fragment that represent the {@link Callback}
     *
     * @param node     The Callback's parent Node
     * @param callback The Callback.
     * @return The Fragment or null if fragment is not defined.
     */
    public Fragment getFragment(Node node, Callback callback) {

        Class<? extends Fragment> clazz = fragments.get(callback.getType());
        if (clazz != null) {
            Fragment fragment = newInstance(clazz);
            Bundle args = new Bundle();
            args.putSerializable(NODE, node);
            args.putSerializable(CALLBACK, callback);
            fragment.setArguments(args);
            return fragment;
        }
        return null;
    }


    /**
     * Register Fragment for Callback or Stage for Page Node
     *
     * @param callback Callback Type or Stage from Page Node
     * @param fragment The Fragment to collect data.
     */
    public void register(String callback, Class<? extends Fragment> fragment) {
        fragments.put(callback, fragment);
    }

    public void register(Class<? extends Callback> callback, Class<? extends Fragment> fragment) {
        try {
            fragments.put(CallbackFactory.getInstance().getType(callback), fragment);
        } catch (Exception e) {
            Logger.error(TAG, e, e.getMessage());
        }
    }


    private Fragment newInstance(Class<? extends Fragment> clazz) {
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
