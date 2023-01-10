/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.webauthn

import android.os.Parcelable
import android.util.Base64
import com.google.android.gms.fido.common.Transport
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

private const val ID = "id"

private const val TYPE = "type"

private const val RPID = "rpid"

private const val USER_HANDLE = "userHandle"

private const val OTHER_UI = "otherUI"

private const val CREATED = "created"

/**
 * Representation of Public Key Credential Source that received after WebAuthn registration.
 */
@Parcelize
class PublicKeyCredentialSource(val id: ByteArray,
                                val type: String = PublicKeyCredentialType.PUBLIC_KEY.toString(),
                                val rpid: String,
                                val userHandle: ByteArray,
                                val otherUI: String,
                                val created: Long) : Parcelable {

    /**
     * Convert [PublicKeyCredentialSource] to [JSONObject]
     *
     * @return The result JSONObject
     */
    fun toJson() = JSONObject().apply {
        this.put(ID, Base64.encodeToString(id, Base64.DEFAULT))
        this.put(TYPE, type)
        this.put(RPID, rpid)
        this.put(USER_HANDLE, Base64.encodeToString(userHandle, Base64.DEFAULT))
        this.put(OTHER_UI, otherUI)
        this.put(CREATED, created)
    }

    /**
     * Convert [PublicKeyCredentialSource] to [PublicKeyCredentialDescriptor]
     *
     * @return The result [PublicKeyCredentialDescriptor]
     */
    fun toDescriptor() =
        PublicKeyCredentialDescriptor(PublicKeyCredentialType.fromString(type).toString(),
            id, listOf(Transport.INTERNAL))

    class PublicKeyCredentialSourceBuilder internal constructor() {
        private lateinit var id: ByteArray
        private var type: String = PublicKeyCredentialType.PUBLIC_KEY.toString()
        private lateinit var rpid: String
        private lateinit var userHandle: ByteArray
        private lateinit var otherUI: String
        private var created: Long? = null

        fun id(id: ByteArray): PublicKeyCredentialSourceBuilder {
            this.id = id
            return this
        }

        fun type(type: String): PublicKeyCredentialSourceBuilder {
            this.type = type
            return this
        }

        fun rpid(rpid: String): PublicKeyCredentialSourceBuilder {
            this.rpid = rpid
            return this
        }

        fun userHandle(userHandle: ByteArray): PublicKeyCredentialSourceBuilder {
            this.userHandle = userHandle
            return this
        }

        fun otherUI(otherUI: String): PublicKeyCredentialSourceBuilder {
            this.otherUI = otherUI
            return this
        }

        fun created(created: Long): PublicKeyCredentialSourceBuilder {
            this.created = created
            return this
        }

        fun build(): PublicKeyCredentialSource {
            return PublicKeyCredentialSource(id, type, rpid, userHandle, otherUI, created = created ?: System.currentTimeMillis())
        }
    }

    companion object {
        fun builder() = PublicKeyCredentialSourceBuilder()

        /**
         * Convert [JSONObject] to [PublicKeyCredentialSource]
         *
         * @param jsonObject The json object to be converted
         * @return The result PublicKeyCredentialSource
         */
        @JvmStatic
        fun fromJson(jsonObject: JSONObject) =
            builder()
                .id(Base64.decode(jsonObject.getString(ID), Base64.DEFAULT))
                .type(jsonObject.getString(TYPE))
                .rpid(jsonObject.getString(RPID))
                .userHandle(Base64.decode(jsonObject.getString(USER_HANDLE), Base64.DEFAULT))
                .otherUI(jsonObject.getString(OTHER_UI))
                .created(jsonObject.optLong(CREATED,0 ))
                .build()

    }
}