/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import androidx.security.crypto.EncryptedFile
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * An implementation of [KeyStoreRepository] which use [EncryptedFile] to store the KeyStore
 */
class EncryptedFileKeyStore(val identifier: String,
                            private val aliasName: String = "org.forgerock.v1.KEY_STORE_MASTER_KEY_ALIAS") : KeyStoreRepository {

    override fun getInputStream(context: Context): InputStream {
        return getEncryptedFile(context).openFileInput();
    }

    override fun getOutputStream(context: Context): OutputStream {
        return getEncryptedFile(context, true).openFileOutput();
    }

    override fun delete(context: Context) {
        val file = File(context.filesDir, identifier)
        file.delete()
    }

    override fun exist(context: Context): Boolean {
        val file = File(context.filesDir, identifier)
        return file.exists()
    }

    private fun getEncryptedFile(context: Context, createNew: Boolean = false): EncryptedFile {
        val file = File(context.filesDir, identifier)
        if (createNew and file.exists()) {
            file.delete();
        }
        return org.forgerock.android.auth.EncryptedFile.getInstance(context, file, aliasName)
    }


}