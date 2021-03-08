/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public abstract class DataRepositoryTest {

    public static final String TEST_KEY = "TestKey";
    public static final String TEST_VALUE = "TestValue";
    public Context context = ApplicationProvider.getApplicationContext();

    protected DataRepository repository;

    public abstract DataRepository getRepository() throws Exception;

    @Before
    public void setUpAccountDataRepository() throws Exception {
        repository = getRepository();
    }

    @After
    public void tearDown() {
        repository.deleteAll();
    }

    @Test
    public void testSaveAndGetString() {
        repository.save(TEST_KEY, TEST_VALUE);
        assertThat(repository.getString(TEST_KEY)).isEqualTo(TEST_VALUE);
    }

    @Test
    public void testDelete() {
        repository.save(TEST_KEY, TEST_VALUE);
        repository.delete(TEST_KEY);
        assertThat(repository.getString(TEST_KEY)).isNull();
    }

    @Test
    public void testDeleteAll() throws Exception {
        repository.save(TEST_KEY, TEST_VALUE);
        repository.deleteAll();
        assertThat(repository.getString(TEST_KEY)).isNull();

    }
}