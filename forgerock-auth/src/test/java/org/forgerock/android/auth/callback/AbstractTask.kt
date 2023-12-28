/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback

import android.app.Activity
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import java.lang.Exception
import java.lang.UnsupportedOperationException
import java.util.concurrent.Executor

abstract class AbstractTask<T> : Task<T>() {
    override fun addOnFailureListener(p0: OnFailureListener): Task<T> {
        throw UnsupportedOperationException()
    }

    override fun addOnFailureListener(p0: Activity, p1: OnFailureListener): Task<T> {
        throw UnsupportedOperationException()
    }

    override fun addOnFailureListener(p0: Executor, p1: OnFailureListener): Task<T> {
        throw UnsupportedOperationException()
    }

    override fun getException(): Exception? {
        throw UnsupportedOperationException()
    }

    override fun getResult(): T {
        throw UnsupportedOperationException()
    }

    override fun <X : Throwable?> getResult(p0: Class<X>): T {
        throw UnsupportedOperationException()
    }

    override fun isCanceled(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun isComplete(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun isSuccessful(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addOnSuccessListener(p0: Executor, p1: OnSuccessListener<in T>): Task<T> {
        throw UnsupportedOperationException()
    }

    override fun addOnSuccessListener(p0: Activity, p1: OnSuccessListener<in T>): Task<T> {
        throw UnsupportedOperationException()
    }

    override fun addOnSuccessListener(p0: OnSuccessListener<in T>): Task<T> {
        throw UnsupportedOperationException()
    }
}