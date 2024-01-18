package org.forgerock.android.auth

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.annotation.MainThread
import org.forgerock.android.auth.callback.CallbackFactory
import org.forgerock.android.auth.callback.PingOneProtectEvaluationCallback
import org.forgerock.android.auth.callback.PingOneProtectInitCallback

/**
 * Content Provider to register Activity Lifecycle Callbacks and keep track of the last active activity.
 */
class PingProvider : ContentProvider() {

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
        CallbackFactory.instance.register(PingOneProtectInitCallback::class.java)
        CallbackFactory.instance.register(PingOneProtectEvaluationCallback::class.java)
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
