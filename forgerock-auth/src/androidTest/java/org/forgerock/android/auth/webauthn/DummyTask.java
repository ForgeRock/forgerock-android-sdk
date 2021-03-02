/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.webauthn;

import android.app.Activity;
import android.app.PendingIntent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.concurrent.Executor;

/**
 * DummyTask that represent the result of {@link com.google.android.gms.fido.fido2.Fido2ApiClient}
 */
public class DummyTask extends Task<PendingIntent> {

    @Override
    public boolean isComplete() {
        return false;
    }

    @Override
    public boolean isSuccessful() {
        return false;
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Nullable
    @Override
    public PendingIntent getResult() {
        return null;
    }

    @Nullable
    @Override
    public <X extends Throwable> PendingIntent getResult(@NonNull Class<X> aClass) throws X {
        return null;
    }

    @Nullable
    @Override
    public Exception getException() {
        return null;
    }

    @NonNull
    @Override
    public Task<PendingIntent> addOnSuccessListener(@NonNull OnSuccessListener<? super PendingIntent> onSuccessListener) {
        return this;
    }

    @NonNull
    @Override
    public Task<PendingIntent> addOnSuccessListener(@NonNull Executor executor, @NonNull OnSuccessListener<? super PendingIntent> onSuccessListener) {
        return this;
    }

    @NonNull
    @Override
    public Task<PendingIntent> addOnSuccessListener(@NonNull Activity activity, @NonNull OnSuccessListener<? super PendingIntent> onSuccessListener) {
        return this;
    }

    @NonNull
    @Override
    public Task<PendingIntent> addOnFailureListener(@NonNull OnFailureListener onFailureListener) {
        return this;
    }

    @NonNull
    @Override
    public Task<PendingIntent> addOnFailureListener(@NonNull Executor executor, @NonNull OnFailureListener onFailureListener) {
        return this;
    }

    @NonNull
    @Override
    public Task<PendingIntent> addOnFailureListener(@NonNull Activity activity, @NonNull OnFailureListener onFailureListener) {
        return this;
    }
}
