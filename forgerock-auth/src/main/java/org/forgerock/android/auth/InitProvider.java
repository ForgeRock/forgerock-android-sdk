/*
 * Copyright (c) 2019 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

import java.lang.ref.WeakReference;

/**
 * Content Provider to register Activity Lifecycle Callbacks and keep track of the last active activity.
 */
public class InitProvider extends ContentProvider {

    private static WeakReference<Activity> currentActivity = new WeakReference<>(null);

    /**
     * Retrieve the current active {@link Activity}
     *
     * @return The current active {@link Activity}
     */
    public static Activity getCurrentActivity() {
        return currentActivity.get();
    }

    /**
     * Retrieve the current active activity as {@link FragmentActivity}
     *
     * @return The current active activity as {@link FragmentActivity}
     */
    public static FragmentActivity getCurrentActivityAsFragmentActivity() {
        return (FragmentActivity) currentActivity.get();
    }

    @VisibleForTesting
    public static void setCurrentActivity(Activity activity) {
        currentActivity = new WeakReference<>(activity);
    }

    public InitProvider() {
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(@NonNull Uri uri) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @MainThread
    @Override
    public boolean onCreate() {
        Application app = (Application) getContext().getApplicationContext();
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                currentActivity = new WeakReference<>(activity);
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                currentActivity = new WeakReference<>(activity);
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                currentActivity = new WeakReference<>(activity);
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });
        return false;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
