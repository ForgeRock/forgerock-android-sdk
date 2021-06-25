/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import lombok.Builder;

/**
 * A Repository that store data in {@link SharedPreferences}
 */
@TargetApi(23)
class SharedPreferenceDataRepository implements DataRepository {

    private final SharedPreferences sharedPreferences;

    @Builder
    public SharedPreferenceDataRepository(@NonNull Context context, @NonNull SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    @Override
    public void save(String key, String value) {
        sharedPreferences.edit().putString(key, value).commit();
    }

    @Override
    public String getString(String key) {
        return sharedPreferences.getString(key, null);
    }

    @Override
    public void delete(String key) {
        sharedPreferences.edit().remove(key).commit();
    }

    @Override
    public void deleteAll() {
        sharedPreferences.edit().clear().commit();
    }

}
