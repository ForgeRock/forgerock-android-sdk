/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
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
class EncryptedFileKeyStore : KeyStoreRepository {

    override fun getInputStream(context: Context, keyAlias: String): InputStream {
        return getEncryptedFile(context, keyAlias).openFileInput();
    }

    override fun getOutputStream(context: Context, keyAlias: String): OutputStream {
        return getEncryptedFile(context, keyAlias, true).openFileOutput();
    }

    private fun getEncryptedFile(context: Context,
                                 keyAlias: String,
                                 createNew: Boolean = false): EncryptedFile {
        val file = File(context.filesDir, keyAlias)
        if (createNew and file.exists()) {
            file.delete();
        }
        return org.forgerock.android.auth.EncryptedFile.getInstance(context, file)
    }


}