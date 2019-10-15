/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

public class AndroidLSingleSignOnManager extends DefaultSingleSignOnManager {

    public AndroidLSingleSignOnManager(Context context, Encryptor encryptor) {
        super(context, null,  encryptor, null);
    }

    @Override
    protected Encryptor getEncryptor(Context context) {
        return new AndroidLEncryptor(context, ORG_FORGEROCK_V_1_SSO_KEYS, this);
    }
}
