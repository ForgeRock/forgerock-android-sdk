/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.ActivityNotFoundException;
import android.os.Bundle;

import androidx.annotation.Nullable;

import org.forgerock.android.auth.idp.AppleSignInHandler;

public class AppleSignInHandlerMock extends AppleSignInHandler {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
        } catch (ActivityNotFoundException e) {
            //ignore
        }
    }
}
