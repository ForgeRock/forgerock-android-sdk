/*
 * Copyright (c) 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Objects;

/**
 * Represents a FCM device token with its associated ID and last update timestamp.
 */
public class PushDeviceToken extends ModelObject<PushDeviceToken> {

    private final String tokenId;
    private final Calendar timeAdded;

    /**
     * Constructs a new PushDeviceToken.
     *
     * @param tokenId       The ID of the device token (cannot be null or empty).
     * @param timeAdded     The date the device token was received.
     * @throws IllegalArgumentException If tokenId is null or empty, or if lastUpdate is negative.
     */
    public PushDeviceToken(@NonNull String tokenId, @NonNull Calendar timeAdded) {
        if (tokenId.isEmpty()) {
            throw new IllegalArgumentException("Token ID cannot be null or empty.");
        }
        this.tokenId = tokenId;
        this.timeAdded = timeAdded;
    }

    /**
     * Gets the ID of the device token.
     *
     * @return The device token ID.
     */
    public String getTokenId() {
        return tokenId;
    }

    /**
     * Get the time that this device token was received.
     *
     * @return The date the device token was received.
     */
    public Calendar getTimeAdded() {
        return timeAdded;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj
     * argument; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PushDeviceToken that = (PushDeviceToken) o;
        return timeAdded == that.timeAdded &&
                tokenId.equals(that.tokenId);
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(tokenId, timeAdded);
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return "PushDeviceToken{" +
                "tokenId='" + tokenId + '\'' +
                ", lastUpdate=" + timeAdded +
                '}';
    }

    @Override
    public boolean matches(PushDeviceToken other) {
       if (other == null) {
           return false;
       } else {
           return other.tokenId.equals(tokenId) && other.timeAdded.equals(timeAdded);
       }
    }

    @Override
    public String toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("tokenId", getTokenId());
            jsonObject.put("timeAdded", getTimeAdded());
        } catch (JSONException e) {
            throw new RuntimeException("Error parsing PushDeviceToken object to JSON string representation.", e);
        }
        return jsonObject.toString();
    }

    @Override
    String serialize() {
        return this.toJson();
    }

    /**
     * Deserializes a JSON string into a {@link PushDeviceToken} object.
     * @param jsonString the json string representing the object to be deserialized
     * @return a {@link PushDeviceToken} object from the string. Returns {@code null} if {@code jsonString} is {@code null}
     * or if {@code jsonString} is empty.
     */
    public static PushDeviceToken deserialize(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }

        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            return new PushDeviceToken(
                    jsonObject.getString("tokenId"),
                    getDate(jsonObject.optLong("timeAdded")));
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public int compareTo(PushDeviceToken another) {
        if (another == null) {
            return -1;
        }
        int compareTokenId = tokenId.compareTo(another.tokenId);
        if (compareTokenId == 0) {
            return timeAdded.compareTo(another.timeAdded);
        }
        return compareTokenId;
    }
}
