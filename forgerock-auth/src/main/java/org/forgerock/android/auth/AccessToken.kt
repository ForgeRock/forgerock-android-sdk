/*
 * Copyright (c) 2019 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import java.util.Date
import java.util.StringTokenizer

/**
 * Models an OAuth2 access token.
 *
 * @property value The value of the access token.
 * @property expiresIn The duration (in seconds) for which the access token is valid.
 * @property expiration The date and time when the access token expires.
 * @property refreshToken The refresh token which can be used to obtain new access tokens.
 * @property idToken The ID token associated with the access token.
 * @property tokenType The type of the token (usually "Bearer").
 * @property scope The scope of the access token which defines the resources that the access token can access.
 * @property sessionToken The session token associated with the access token.
 */
@Serializable
data class AccessToken(
    override val value: String = "",
    val tokenType: String? = null,
    @Serializable(with = ScopeSerializer::class)
    val scope: Scope? = null,
    val expiresIn: Long = 0,
    val refreshToken: String? = null,
    val idToken: String? = null,
    @Serializable(with = DateSerializer::class)
    var expiration: Date? = null,
    @Serializable(with = SSOTokenSerializer::class)
    val sessionToken: SSOToken? = null
) : Token {
    @Transient
    var isPersisted: Boolean = false

    init {
        expiration = expiration ?: Date(System.currentTimeMillis() + (expiresIn * 1000L))
    }

    /**
     * Checks if the access token is expired.
     *
     * @return true if the expiration is before the current time, false otherwise.
     */
    val isExpired: Boolean
        get() = isExpired(0)

    /**
     * Checks if the access token is expired.
     *
     * @param threshold Threshold in Seconds
     * @return true if the expiration is before the current time, false otherwise.
     */
    fun isExpired(threshold: Long): Boolean {
        val now = Date(System.currentTimeMillis() + (threshold * 1000L))
        return expiration?.before(now) ?: false
    }

    /**
     * Represents the authorization scope of the access token.
     */
    @Serializable
    class Scope : HashSet<String>() {

        companion object {
            @JvmStatic
            fun parse(s: String?): Scope? {
                if (s == null) return null

                val scope = Scope()

                if (s.trim { it <= ' ' }.isEmpty()) return scope

                val st = StringTokenizer(s, " ")

                while (st.hasMoreTokens()) {
                    scope.add(st.nextToken())
                }

                return scope
            }
        }
    }

    fun toJson(): String {
        return json.encodeToString(serializer(), this)
    }


    /**
     * Builder class for AccessToken.
     */
    class AccessTokenBuilder internal constructor() {
        private var value: String = ""
        private var expiresIn: Long = 0
        private var refreshToken: String? = null
        private var idToken: String? = null
        private var tokenType: String? = null
        private var scope: Scope? = null
        private var sessionToken: SSOToken? = null

        fun value(value: String): AccessTokenBuilder {
            this.value = value
            return this
        }

        fun expiresIn(expiresIn: Long): AccessTokenBuilder {
            this.expiresIn = expiresIn
            return this
        }

        fun refreshToken(refreshToken: String?): AccessTokenBuilder {
            this.refreshToken = refreshToken
            return this
        }

        fun idToken(idToken: String?): AccessTokenBuilder {
            this.idToken = idToken
            return this
        }

        fun tokenType(tokenType: String?): AccessTokenBuilder {
            this.tokenType = tokenType
            return this
        }

        fun scope(scope: Scope?): AccessTokenBuilder {
            this.scope = scope
            return this
        }

        fun sessionToken(sessionToken: SSOToken?): AccessTokenBuilder {
            this.sessionToken = sessionToken
            return this
        }

        /**
         * Builds an AccessToken object.
         *
         * @return An AccessToken object.
         */
        fun build(): AccessToken {
            return AccessToken(
                value = this.value,
                expiresIn = this.expiresIn,
                refreshToken = this.refreshToken,
                idToken = this.idToken,
                tokenType = this.tokenType,
                scope = this.scope,
                sessionToken = this.sessionToken)
        }

        override fun toString(): String {
            return "AccessToken.AccessTokenBuilder(value=" + this.value + ", expiresIn=" + this.expiresIn + ", refreshToken=" + this.refreshToken + ", idToken=" + this.idToken + ", tokenType=" + this.tokenType + ", scope=" + this.scope + ", sessionToken=" + this.sessionToken + ")"
        }
    }

    companion object {

        @JvmStatic
        fun builder(): AccessTokenBuilder {
            return AccessTokenBuilder()
        }
    }

}

@OptIn(InternalSerializationApi::class)
object ScopeSerializer : KSerializer<AccessToken.Scope> {

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor("Scope2", StructureKind.LIST)

    override fun serialize(encoder: Encoder, value: AccessToken.Scope) {
        encoder.encodeStructure(descriptor) {
            value.toList().forEachIndexed { i, v -> encodeStringElement(descriptor, i, v) }
        }
    }

    override fun deserialize(decoder: Decoder): AccessToken.Scope {
        return decoder.decodeStructure(descriptor) {
            val scope = AccessToken.Scope()
            while (true) {
                val index = decodeElementIndex(descriptor)
                if (index == -1) break
                scope.add(decodeStringElement(descriptor, index))
            }
            scope
        }
    }
}

@OptIn(InternalSerializationApi::class)
object SSOTokenSerializer : KSerializer<SSOToken> {

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("SSOToken", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SSOToken) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): SSOToken {
        val value = decoder.decodeString()
        return SSOToken(value)
    }
}

@OptIn(InternalSerializationApi::class)
object DateSerializer : KSerializer<Date> {

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor("Date", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeLong(value.time)
    }

    override fun deserialize(decoder: Decoder): Date {
        val value = decoder.decodeLong()
        return Date(value)
    }
}