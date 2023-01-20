/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import androidx.security.crypto.EncryptedFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * An implementation of [KeyStoreRepository] which use [EncryptedFile] to store the KeyStore
 */
class EncryptedFileKeyStore(val identifier: String,
                            private val aliasName: String = "org.forgerock.v1.DEVICE_REPO_BKS") : KeyStoreRepository {

    override fun getInputStream(context: Context, keyAlias: String): InputStream {
        val file = getFile(context)
        val byteArray = Encryptor.getEncryptor(context, keyAlias).decrypt(file.readBytes())
        return ByteArrayInputStream(byteArray)
    }

    override fun getOutputStream(context: Context, keyAlias: String): OutputStream {
        return CustomByteArrayOutputStream {
            val file = getFile(context, true)
            val byteArray = Encryptor.getEncryptor(context, keyAlias).encrypt(it)
            file.writeBytes(byteArray)
        }
    }

    override fun delete(context: Context) {
        val file = File(context.filesDir, identifier)
        file.delete()
    }

    private fun getFile(context: Context, createNew: Boolean = false): File {
        val file = File(context.filesDir, identifier)
        if (createNew and file.exists()) {
            file.delete();
        }
        return file
    }
}


open class CustomByteArrayOutputStream(val onClose:(ByteArray) -> Unit) : ByteArrayOutputStream() {
    override fun close() {
        onClose(buf)
        super.close()
    }
}