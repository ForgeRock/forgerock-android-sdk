/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

public class AccountDataRepositoryTest extends DataRepositoryTest {
    @Override
    public DataRepository getRepository() throws Exception {
        return AccountDataRepository.builder().accountName("Dummy")
                .context(context).encryptor(new MockEncryptor())
                .build();
    }
}
