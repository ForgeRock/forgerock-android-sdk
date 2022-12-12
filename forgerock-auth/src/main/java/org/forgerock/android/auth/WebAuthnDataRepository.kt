/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64.*
import org.forgerock.android.auth.Logger.Companion.error
import org.forgerock.android.auth.Logger.Companion.warn
import org.forgerock.android.auth.webauthn.PublicKeyCredentialSource
import org.forgerock.android.auth.webauthn.PublicKeyCredentialSource.Companion.fromJson
import org.json.JSONArray
import org.json.JSONObject

/**
 * A Repository that store [PublicKeyCredentialSource]
 */

private val TAG = WebAuthnDataRepository::class.java.simpleName
internal const val ALLOW_CREDENTIALS = "ALLOW_CREDENTIALS"
private const val ORG_FORGEROCK_V_1_WEBAUTHN_KEYS = "org.forgerock.v1.WEBAUTHN_KEYS"
private const val ORG_FORGEROCK_V_1_WEBAUTHN = "org.forgerock.v1.WEBAUTHN"
private const val ORG_FORGEROCK_V_2_WEBAUTHN = "org.forgerock.v2.WEBAUTHN"

open class WebAuthnDataRepository internal constructor(val context: Context,
                                                       sharedPreferences: SharedPreferences?) {
    private lateinit var dataRepository: SharedPreferences

    init {
        try {
            dataRepository = sharedPreferences
                ?: SecuredSharedPreferences(context,
                    ORG_FORGEROCK_V_1_WEBAUTHN,
                    ORG_FORGEROCK_V_1_WEBAUTHN_KEYS)
            dataRepository = this.migrate(dataRepository)
        } catch (error: Error) {
            //Something wrong cannot support Usernameless
            error(TAG, error, "UsernameLess cannot be supported.")
        }
    }

    internal open fun getNewSharedPreferences(): SharedPreferences {
        return EncryptedPreferences.getInstance(context, ORG_FORGEROCK_V_2_WEBAUTHN)
    }

    /**
     * Migrate data from [SecuredSharedPreferences] to [EncryptedSharePreference]
     */
    private fun migrate(sharedPreferences: SharedPreferences): SharedPreferences {
        val encryptedPreferences = getNewSharedPreferences()
        //We store all credentials in one attribute before, but now we change the model with
        //rpid as the key, and the string set contains all credentials that has the same rpid
        //rpid -> [credential1, credential2, ...]


        sharedPreferences.getString(ALLOW_CREDENTIALS, null)?.apply {
            val map = mutableMapOf<String, MutableSet<String>>()
            val array = JSONArray(this)

            array.iterator<JSONObject>().forEach {
                fromJson(it).let { credentialSource ->
                    map[credentialSource.rpid] = map[credentialSource.rpid] ?: mutableSetOf()
                    map[credentialSource.rpid]?.add(credentialSource.toJson()
                        .put("created", System.currentTimeMillis()).toString())
                }
            }

            encryptedPreferences.edit().apply {
                map.forEach {
                    this.putStringSet(it.key, it.value)
                }
            }.apply()

            sharedPreferences.edit().remove(ALLOW_CREDENTIALS).apply()
        }
        return encryptedPreferences
    }

    /**
     * Persist the [PublicKeyCredentialSource], it overrides the existing
     * [PublicKeyCredentialSource] with the same [PublicKeyCredentialSource.getUserHandle]
     * * @param source The [PublicKeyCredentialSource] to persist.
     */
    fun persist(source: PublicKeyCredentialSource) {
        if (!this::dataRepository.isInitialized) {
            warn(TAG, "UsernameLess cannot be supported. No credential will be stored")
            return
        }

        dataRepository.getStringSet(source.rpid, emptySet())?.let {
            val result = mutableSetOf<String>()
            result.addAll(it)
            result.add(source.toJson().put("created", System.currentTimeMillis()).toString())
            dataRepository.edit().putStringSet(source.rpid, result).apply()
        }
    }

    /**
     * Retrieve all [PublicKeyCredentialSource] with the same rpId [PublicKeyCredentialSource.getRpid]
     *
     * @param rpId The Relying Party Id
     * @return All [PublicKeyCredentialSource] with the same rpId [PublicKeyCredentialSource.getRpid]
     */
    fun getPublicKeyCredentialSource(rpId: String): List<PublicKeyCredentialSource> {
        if (!this::dataRepository.isInitialized) {
            warn(TAG, "UsernameLess cannot be supported. No credential will be stored")
            return emptyList()
        }
        return dataRepository.getStringSet(rpId, null)?.mapNotNull {
            JSONObject(it)
        }?.sortedWith(compareByDescending {
            it.optLong("created", 0)
        })?.map {
            fromJson(it)
        } ?: emptyList()
    }

    fun deleteByUserIdAndRpId(userId: ByteArray, rpId: String) {
        if (!this::dataRepository.isInitialized) {
            warn(TAG, "UsernameLess cannot be supported. No credential will be stored")
            return
        }

        val storedCredentials = dataRepository.getStringSet(rpId, null)?.filter {
            fromJson(JSONObject(it)).userHandle.contentEquals(userId).not()
        }?.toSet()

        storedCredentials?.let {
            dataRepository.edit().putStringSet(rpId, it).apply()
        }

    }

    class WebAuthnDataRepositoryBuilder internal constructor() {
        private lateinit var context: Context;
        private var sharedPreferences: SharedPreferences? = null
        fun context(context: Context): WebAuthnDataRepositoryBuilder {
            this.context = context
            return this
        }

        fun sharedPreferences(sharedPreferences: SharedPreferences?): WebAuthnDataRepositoryBuilder {
            this.sharedPreferences = sharedPreferences
            return this
        }

        fun build(): WebAuthnDataRepository {
            return WebAuthnDataRepository(context,
                sharedPreferences)
        }
    }

    companion object {

        @JvmStatic
        fun builder(): WebAuthnDataRepositoryBuilder {
            return WebAuthnDataRepositoryBuilder()
        }
    }
}