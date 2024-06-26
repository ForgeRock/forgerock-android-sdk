/*
 * Copyright (c) 2019 - 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable
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
class AccessToken(override val value: String,
                  val expiresIn: Long,
                  var expiration: Date?,
                  val refreshToken: String?,
                  val idToken: String?,
                  val tokenType: String?,
                  val scope: Scope?,
                  val sessionToken: SSOToken?) : Token(
    value), Serializable {
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
    class Scope : HashSet<String> {
        constructor(stringSet: Set<String>) : super(stringSet)

        constructor() : super()

        /**
         * Converts the scope to a JSON array.
         *
         * @return A JSON array representing the scope.
         */
        fun toJsonArray(): JSONArray {
            val result = JSONArray()
            for (s in this) {
                result.put(s)
            }
            return result
        }

        companion object {
            /**
             * Creates a Scope object from a JSON array.
             *
             * @param array The JSON array representing the scope.
             * @return A Scope object.
             * @throws JSONException If the JSON array is malformed.
             */
            @Throws(JSONException::class)
            fun fromJsonArray(array: JSONArray?): Scope? {
                if (array == null) {
                    return null
                }
                val s = Scope()
                for (i in 0 until array.length()) {
                    s.add(array.getString(i))
                }
                return s
            }

            /**
             * Parses a scope from the specified string representation.
             *
             * @param s The scope string.
             * @return The scope.
             */
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

    /**
     * Converts the access token to a JSON string.
     *
     * @return A JSON string representing the access token.
     * @throws RuntimeException If the JSON object is malformed.
     */
    fun toJson(): String {
        val result = JSONObject()
        try {
            result.put("value", value)
            result.put("expiresIn", expiresIn)
            result.put("refreshToken", refreshToken)
            result.put("idToken", idToken)
            result.put("tokenType", tokenType)
            result.put("scope", scope?.toJsonArray())
            expiration?.let {
                result.put("expiration", it.time)
            }
            result.put("sessionToken", sessionToken?.value)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
        return result.toString()
    }

    /**
     * Builder class for AccessToken.
     */
    class AccessTokenBuilder internal constructor() {
        private var value: String = ""
        private var expiresIn: Long = 0
        private var expiration: Date? = null
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

        fun expiration(expiration: Date?): AccessTokenBuilder {
            this.expiration = expiration
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
            return AccessToken(this.value,
                this.expiresIn,
                this.expiration,
                this.refreshToken,
                this.idToken,
                this.tokenType,
                this.scope,
                this.sessionToken)
        }

        override fun toString(): String {
            return "AccessToken.AccessTokenBuilder(value=" + this.value + ", expiresIn=" + this.expiresIn + ", expiration=" + this.expiration + ", refreshToken=" + this.refreshToken + ", idToken=" + this.idToken + ", tokenType=" + this.tokenType + ", scope=" + this.scope + ", sessionToken=" + this.sessionToken + ")"
        }
    }

    companion object {
        /**
         * Creates a new AccessTokenBuilder.
         *
         * @return An AccessTokenBuilder object.
         */
        @JvmStatic
        fun builder(): AccessTokenBuilder {
            return AccessTokenBuilder()
        }

        /**
         * Creates an AccessToken object from a JSON string.
         *
         * @param str The JSON string representing the access token.
         * @return An AccessToken object.
         */
        @JvmStatic
        fun fromJson(str: String): AccessToken? {
            try {
                val result = JSONObject(str)
                return builder()
                    .value(result.getString("value"))
                    .expiresIn(result.optLong("expiresIn", -1))
                    .refreshToken(if (result.has("refreshToken")) result.getString("refreshToken") else null)
                    .idToken(if (result.has("idToken")) result.getString("idToken") else null)
                    .tokenType(if (result.has("tokenType")) result.getString("tokenType") else null)
                    .scope(Scope.fromJsonArray(result.optJSONArray("scope")))
                    .expiration(expiration(result.optLong("expiration", -1)))
                    .sessionToken(if (result.has("sessionToken")) SSOToken(result.optString("sessionToken")) else null)
                    .build()
            } catch (e: JSONException) {
                return null
            }
        }

        /**
         * Converts a timestamp to a Date object.
         *
         * @param expiration The timestamp.
         * @return A Date object.
         */
        private fun expiration(expiration: Long): Date? {
            if (expiration == -1L) {
                return null
            }
            return Date(expiration)
        }
    }
}