/*
 * Copyright (c) 2024 Ping Identity. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.selfservice

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Interface defining immutable device operations.
 */
interface ImmutableDevice<T> {
    /**
     * Retrieves a list of devices.
     * @return A list of devices of type [T].
     */
    suspend fun get(): List<T>

    /**
     * Deletes the specified device.
     * @param device The device to delete.
     */
    suspend fun delete(device: T)
}

/**
 * Interface defining mutable device operations.
 */
interface MutableDevice<T> : ImmutableDevice<T> {
    /**
     * Updates the specified device.
     * @param device The device to update.
     */
    suspend fun update(device: T)
}

/**
 * Abstract class representing a device.
 */
@Serializable
sealed class Device {
    abstract val id: String
    abstract val deviceName: String
    abstract val urlSuffix: String
}

/**
 * Data class representing a Binding device.
 * @property id The ID of the device.
 * @property deviceName The name of the device.
 * @property deviceId The device ID.
 * @property uuid The UUID of the device.
 * @property createdDate The creation date of the device.
 * @property lastAccessDate The last access date of the device.
 */
@Serializable
data class BoundDevice(
    @SerialName("_id")
    override val id: String,
    override var deviceName: String,
    val deviceId: String,
    val uuid: String,
    val createdDate: Long,
    val lastAccessDate: Long
) : Device() {
    override var urlSuffix: String = "devices/2fa/binding"
}

/**
 * Data class representing an Oath device.
 * @property id The ID of the device.
 * @property deviceName The name of the device.
 * @property uuid The UUID of the device.
 * @property createdDate The creation date of the device.
 * @property lastAccessDate The last access date of the device.
 */
@Serializable
data class OathDevice(
    @SerialName("_id")
    override val id: String,
    override val deviceName: String,
    val uuid: String,
    val createdDate: Long,
    val lastAccessDate: Long
) : Device() {
    override var urlSuffix: String = "devices/2fa/oath"
}

/**
 * Data class representing a Push device.
 * @property id The ID of the device.
 * @property deviceName The name of the device.
 * @property uuid The UUID of the device.
 * @property createdDate The creation date of the device.
 * @property lastAccessDate The last access date of the device.
 */
@Serializable
data class PushDevice(
    @SerialName("_id")
    override val id: String,
    override val deviceName: String,
    val uuid: String,
    val createdDate: Long,
    val lastAccessDate: Long
) : Device() {
    override var urlSuffix: String = "devices/2fa/push"
}

/**
 * Data class representing a WebAuthn device.
 * @property id The ID of the device.
 * @property deviceName The name of the device.
 * @property uuid The UUID of the device.
 * @property credentialId The credential ID of the device.
 * @property createdDate The creation date of the device.
 * @property lastAccessDate The last access date of the device.
 */
@Serializable
data class WebAuthnDevice(
    @SerialName("_id")
    override val id: String,
    override var deviceName: String,
    val uuid: String,
    val credentialId: String,
    val createdDate: Long,
    val lastAccessDate: Long
) : Device() {
    override var urlSuffix: String = "devices/2fa/webauthn"
}

/**
 * Data class representing a Profile device.
 * @property id The ID of the device.
 * @property deviceName The name of the device (alias).
 * @property identifier The identifier of the device.
 * @property metadata The metadata of the device.
 * @property location The location of the device.
 * @property lastSelectedDate The last selected date of the device.
 */
@Serializable
data class ProfileDevice(
    @SerialName("_id")
    override val id: String,
    @SerialName("alias")
    override var deviceName: String, // alias
    val identifier: String,
    val metadata: JsonObject,
    val location: Location? = null,
    val lastSelectedDate: Long
) : Device() {
    override var urlSuffix: String = "devices/profile"
}

/**
 * Data class representing a location.
 * @property latitude The latitude of the location.
 * @property longitude The longitude of the location.
 */
@Serializable
data class Location(val latitude: Double, val longitude: Double)