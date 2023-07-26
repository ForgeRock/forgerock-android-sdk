/*
 * Copyright (c) 2022 - 2023 ForgeRock. All rights reserved.
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

            Logger.debug("---->${dataRepository.all}", "one 0")

         //  dataRepository = migrate(dataRepository)
            dataRepository.getBoolean("caching", false).apply {
                if(this.not()) {
                    dataRepository = migrate(dataRepository)
                    dataRepository = migrateToAlpha(dataRepository)
                } else {
                    dataRepository = migrateToAlpha(null)
                }
            }

        } catch (error: Error) {
            //Something wrong cannot support Usernameless
            error(TAG, error, "UsernameLess cannot be supported.")
        }
    }

    internal open fun getNewSharedPreferences(): SharedPreferences {
        return EncryptedPreferences.getInstance(context, ORG_FORGEROCK_V_2_WEBAUTHN)
    }

    private fun migrateToAlpha(encryptedPreferences: SharedPreferences?): SharedPreferences {
        val map = mutableMapOf<String, MutableSet<String>>()

        encryptedPreferences?.let {

            Logger.debug("---->${encryptedPreferences.all}", "one 4")
            val keys: List<String> = encryptedPreferences.all.entries.map { it.key }

            Logger.debug("---->$keys", "tested0")
            Logger.debug("---->${encryptedPreferences.all}", "tested1")

            keys.forEach { key ->
                encryptedPreferences.getStringSet(key, emptySet())?.let {
                    val result = mutableSetOf<String>()
                    result.addAll(it)
                    map[key] = result
                }
            }

            encryptedPreferences.edit().clear().apply()
            NewEncryptedPreferences.deletePreferencesFile(context, ORG_FORGEROCK_V_2_WEBAUTHN)
        }

        Logger.debug("---->${map}", "testedEnd")

        val newDataRepository = NewEncryptedPreferences.getInstance(context, ORG_FORGEROCK_V_2_WEBAUTHN)
        newDataRepository.edit().apply {
          map.forEach {
              this.putStringSet(it.key, it.value)
          }
        }.apply()

        Logger.debug("---->${newDataRepository.all}", "tested2")

       return newDataRepository
    }
    /**
     * Migrate data from [SecuredSharedPreferences] to [EncryptedSharePreference]
     */
    private fun migrate(sharedPreferences: SharedPreferences): SharedPreferences {
        val encryptedPreferences = getNewSharedPreferences()

        Logger.debug("---->${sharedPreferences.getString(ALLOW_CREDENTIALS, null)}", "one 1")
        sharedPreferences.getString(ALLOW_CREDENTIALS, null)?.apply {

            Logger.debug("---->i am inside", "one 2")

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

        sharedPreferences.edit().putBoolean("caching", true).apply()

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
            fromJson(JSONObject(it))
        }?.sortedWith(compareBy<PublicKeyCredentialSource> { it.otherUI }.thenBy { it.created } )  ?: emptyList()
    }

    /**
     * Delete the provided [PublicKeyCredentialSource]
     * @param publicKeyCredentialSource The [PublicKeyCredentialSource] to be deleted
     */
    fun delete(publicKeyCredentialSource: PublicKeyCredentialSource) {
        if (!this::dataRepository.isInitialized) {
            warn(TAG, "UsernameLess cannot be supported. No credential will be stored")
            return
        }

        val storedCredentials =
            getPublicKeyCredentialSource(publicKeyCredentialSource.rpid).filter {
                it.id.contentEquals(publicKeyCredentialSource.id).not()
            }.map { it.toJson().toString() }.toSet()

        dataRepository.edit()
            .putStringSet(publicKeyCredentialSource.rpid, storedCredentials)
            .apply()
    }

    /**
     * Delete all the [PublicKeyCredentialSource] with provided rpId
     * @param rpId The rpId to be deleted
     */
    fun delete(rpId: String) {
        if (!this::dataRepository.isInitialized) {
            warn(TAG, "UsernameLess cannot be supported. No credential will be stored")
            return
        }
        dataRepository.edit().remove(rpId).apply()
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