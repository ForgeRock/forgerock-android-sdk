/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.SharedPreferences;

/**
 * Test class for SharedPreferenceDataRepository
 */
public class SharedPreferencesDataRepositoryTest extends DataRepositoryTest {

    @Override
    public DataRepository getRepository() {
        SharedPreferences sp = new SecuredSharedPreferences(context,
                "DUMMY", "DUMMY_KEY", new MockEncryptor());

        return SharedPreferenceDataRepository.builder().context(context)
                .sharedPreferences(sp)
                .build();
    }

}
