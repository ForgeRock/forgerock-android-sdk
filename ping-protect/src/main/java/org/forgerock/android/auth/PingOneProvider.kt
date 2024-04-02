/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import androidx.annotation.MainThread
import org.forgerock.android.auth.callback.CallbackFactory

/**
 * Content Provider to register Activity Lifecycle Callbacks .
 */
internal class PingOneProvider : ContentProvider() {

    internal var factory: CallbackFactory = CallbackFactory.getInstance()
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun getType(uri: Uri): String? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    @MainThread
    override fun onCreate(): Boolean {
        factory.register(PingOneProtectInitializeCallback::class.java)
        factory.register(PingOneProtectEvaluationCallback::class.java)
        return false
    }

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        throw UnsupportedOperationException("Not yet implemented")
    }

}
