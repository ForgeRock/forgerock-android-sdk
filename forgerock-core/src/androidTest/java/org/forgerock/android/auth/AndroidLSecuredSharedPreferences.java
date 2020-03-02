/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

public class AndroidLSecuredSharedPreferences extends SecuredSharedPreferences {

    AndroidLSecuredSharedPreferences(Context context, String fileName, String keyAlias) {
        super(context, fileName, keyAlias);
    }

    @Override
    protected Encryptor getEncryptor(Context context) {
        return new AndroidLEncryptor(context, getKeyAlias(),
                new SharedPreferencesSecretKeyStore(getKeyAlias(), getSharedPreferences()));
    }
}
