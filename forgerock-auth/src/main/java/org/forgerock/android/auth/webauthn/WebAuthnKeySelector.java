/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.webauthn;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import org.forgerock.android.auth.FRListener;

import java.util.List;

/**
 * A Selector for credential key selection.
 */
public interface WebAuthnKeySelector {

    WebAuthnKeySelector DEFAULT = new WebAuthnKeySelector() {
    };

    /**
     * Select the {@link PublicKeyCredentialSource} that used for usernameless authentication.
     *
     * @param fragmentManager The FragmentManager that used to launch the UI fragment.
     * @param sourceList      The stored PublicKeyCredentialSource
     * @param listener        listener for select event, call {@link FRListener#onSuccess(Object)}
     *                        for the selected PublicKeyCredentialSource
     */
    default void select(@NonNull FragmentManager fragmentManager,
                        @NonNull List<PublicKeyCredentialSource> sourceList,
                        @NonNull FRListener<PublicKeyCredentialSource> listener) {

        WebAuthKeySelectionDialogFragment.newInstance(sourceList, listener).show(fragmentManager, null);
    }

}
