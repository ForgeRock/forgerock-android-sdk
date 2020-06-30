/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * PushNotification is a model class which represents a message that was received from an external
 * source. A notification could be raised against any mechanism. Currently used by Push mechanism.
 */
public class PushNotification extends ModelObject<PushNotification> {

    /** Unique identifier for PushNotification object associated with the mechanism */
    private String id;
    /** Unique identifier of the Mechanism associated with this PushNotification */
    private final String mechanismUID;
    /** Message Identifier that was received with this notification */
    private String messageId;
    /** Base64 challenge that was sent with this notification */
    private String challenge;
    /** The AM load balance cookie */
    private String amlbCookie;
    /** Date that the notification was received */
    private final Calendar timeAdded;
    /** Date that the notification has expired */
    private final Calendar timeExpired;
    /** Time to live for the notification */
    private final long ttl;
    /** Determines if the PushNotification has been approved by the user. */
    private boolean approved;
    /** Determines if the PushNotification has been interacted with the user. */
    private boolean pending;
    /** The mechanism associated with this notification **/
    private PushMechanism pushMechanism;

    private static final String TAG = PushNotification.class.getSimpleName();

    /**
     * Creates the PushNotification object with given data
     * @param mechanismUID Mechanism UUID associated with the PushNotification
     * @param messageId message identifier from the message payload
     * @param challenge challenge from message payload
     * @param amlbCookie load balance cookie from OpenAM
     * @param timeAdded Date when the notification is delivered
     * @param timeExpired Date when the notification was expired
     * @param ttl time-to-live value from message payload
     * @param approved boolean indicator of whether notification is still pending or not
     * @param pending boolean indicator of whether notification is approved or not
     */
    private PushNotification(String mechanismUID, String messageId, String challenge, String amlbCookie,
                               Calendar timeAdded, Calendar timeExpired, long ttl, boolean approved,
                               boolean pending) {
        this.id = mechanismUID + "-" + timeAdded;
        this.mechanismUID = mechanismUID;
        this.messageId = messageId;
        this.challenge = challenge;
        this.amlbCookie = amlbCookie;
        this.timeAdded = timeAdded;
        this.timeExpired = timeExpired;
        this.ttl = ttl;
        this.approved = approved;
        this.pending = pending;
    }

    /**
     * Gets the unique identifier for the PushNotification.
     * @return The unique identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * Get the mechanism Id that this notification was intended for.
     * @return The receiving Mechanism.
     */
    public String getMechanismUID() {
        return mechanismUID;
    }

    /**
     * Get the message Id of this notification
     * @return The messageId value
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Get challenge from message payload
     * @return The challenge value
     */
    public String getChallenge() {
        return challenge;
    }

    /**
     * Get load balance cookie from OpenAM
     * @return The load balance cookie value
     */
    public String getAmlbCookie() {
        return amlbCookie;
    }

    /**
     * Get time-to-live value from message payload
     * @return The time-to-live value
     */
    public long getTtl() {
        return ttl;
    }

    /**
     * Get the time that this notification was received.
     * @return The date the notification was receuved.
     */
    public Calendar getTimeAdded() {
        return timeAdded;
    }

    /**
     * Get the time that the notification will or did expire.
     * @return The expiry date.
     */
    public Calendar getTimeExpired() {
        return timeExpired;
    }

    /**
     * Determine whether the authentication the notification is related to succeeded.
     * @return True if the authentication succeeded, false otherwise.
     */
    public boolean isApproved() {
        return approved;
    }

    /**
     * Set if the authentication succeeded.
     */
    void setApproved(boolean approved) {
        this.approved = approved;
    }

    /**
     * Determines if the PushNotification has been interacted with by the user.
     * @return True if the PushNotification has not been interacted with, false otherwise.
     */
    public boolean isPending() {
        return pending;
    }

    /**
     * Determine if the notification has expired.
     * @return True if the notification has expired, false otherwise.
     */
    public final boolean isExpired() {
        return timeExpired.getTimeInMillis() < Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                .getTimeInMillis();
    }

    /**
     * Set if the authentication is pending.
     */
    void setPending(boolean pending) {
        this.pending = pending;
    }

    /**
     * Gets the mechanism object associated with the notification.
     * @return the push mechanism object.
     */
    PushMechanism getPushMechanism() {
        return this.pushMechanism ;
    }

    /**
     * Sets the mechanism object associated with the notification.
     * @param mechanism the mechanism object.
     */
    void setPushMechanism(Mechanism mechanism) {
        this.pushMechanism = (PushMechanism) mechanism;
    }

    @Override
    public String toJson() {
        return convertToJson(true);
    }

    @Override
    String serialize() {
        return convertToJson(false);
    }

    private String convertToJson(boolean excludeSensitiveData) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", getId());
            jsonObject.put("mechanismUID", getMechanismUID());
            jsonObject.put("messageId", getMessageId());
            jsonObject.put("challenge", excludeSensitiveData ? "REMOVED" : getChallenge());
            jsonObject.put("amlbCookie", excludeSensitiveData ? "REMOVED" : getAmlbCookie());
            jsonObject.put("timeAdded", getTimeAdded() != null ? getTimeAdded().getTimeInMillis() : null);
            jsonObject.put("timeExpired", getTimeExpired() != null ? getTimeExpired().getTimeInMillis() : null);
            jsonObject.put("ttl", getTtl());
            jsonObject.put("approved", isApproved());
            jsonObject.put("pending", isPending());
        } catch (JSONException e) {
            Logger.warn(TAG, e, "Error parsing PushNotification object to JSON for messageId: %s",
                    messageId);
            throw new RuntimeException("Error parsing PushNotification object to JSON string representation.", e);
        }
        return jsonObject.toString();
    }

    /**
     * Accepts the push authentication request.
     * @param listener Listener for receiving the HTTP call response code.
     */
    public final void accept(@NonNull FRAListener<Void> listener) {
        Logger.debug(TAG, "Accept Push Authentication request for message: %s", getMessageId());
        performAcceptDenyAsync(true, listener);
    }

    /**
     * Deny the push authentication request.
     * @param listener Listener for receiving the HTTP call response code.
     */
    public final void deny(@NonNull FRAListener<Void> listener) {
        Logger.debug(TAG, "Deny Push Authentication request for message: %s", getMessageId());
        performAcceptDenyAsync(false, listener);
    }

    void performAcceptDenyAsync(boolean approved, FRAListener<Void> listener) {
        PushResponder.getInstance().authentication(this, approved, listener);
    }

    /**
     * Deserializes the specified Json into an object of the {@link PushNotification} object.
     * @param jsonString the json string representing the object to be deserialized
     * @return a {@link PushNotification} object from the string. Returns {@code null} if {@code jsonString} is {@code null}
     * or if {@code jsonString} is empty.
     */
    static PushNotification deserialize(String jsonString) {
        if (jsonString == null || jsonString.length() == 0) {
            return null;
        }

        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            return PushNotification.builder()
                    .setMechanismUID(jsonObject.getString("mechanismUID"))
                    .setMessageId(jsonObject.getString("messageId"))
                    .setChallenge(jsonObject.getString("challenge"))
                    .setAmlbCookie(jsonObject.has("amlbCookie") ? jsonObject.getString("amlbCookie"): null)
                    .setTimeAdded(jsonObject.has("timeAdded") ? getDate(jsonObject.optLong("timeAdded")) : null)
                    .setTimeExpired(jsonObject.has("timeExpired") ? getDate(jsonObject.optLong("timeExpired")) : null)
                    .setTtl(jsonObject.optLong("ttl", -1))
                    .setApproved(jsonObject.getBoolean("approved"))
                    .setPending(jsonObject.getBoolean("pending"))
                    .build();
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public boolean matches(PushNotification other) {
        if (other == null) {
            return false;
        }
        return mechanismUID.equals(other.getMechanismUID()) && timeAdded.getTimeInMillis() == other.timeAdded.getTimeInMillis();
    }

    @Override
    public int compareTo(PushNotification another) {
        long thisTime = timeAdded.getTimeInMillis();
        long otherTime = another.timeAdded.getTimeInMillis();
        if (otherTime < thisTime) {
            return -1;
        }
        if (otherTime == thisTime) {
            return 0;
        }
        return 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PushNotification that = (PushNotification) o;

        if (!mechanismUID.equals(that.mechanismUID)) return false;
        if (!messageId.equals(that.messageId)) return false;
        return timeAdded.equals(that.timeAdded);
    }

    @Override
    public int hashCode() {
        int result = mechanismUID.hashCode();
        result = 31 * result + messageId.hashCode();
        result = 31 * result + timeAdded.hashCode();
        return result;
    }

    /**
     * Returns a builder for creating a PushNotification.
     * @return The PushNotification builder.
     */
    public static PushNotificationBuilder builder() {
        return new PushNotificationBuilder();
    }

    /**
     * Builder class responsible for producing Notifications.
     */
    public static class PushNotificationBuilder {
        private String mechanismUID;
        private String messageId;
        private String challenge;
        private String amlbCookie;
        private Calendar timeAdded;
        private Calendar timeExpired;
        private long ttl;
        private boolean approved = false;
        private boolean pending = true;
        private Mechanism mechanism;

        /**
         * Sets the mechanism unique Id.
         * @param mechanismUID the mechanism unique Id.
         * @return The current builder.
         */
        public PushNotificationBuilder setMechanismUID(String mechanismUID) {
            this.mechanismUID = mechanismUID;
            return this;
        }

        /**
         * Sets the message id that was received with this notification.
         * @param messageId The messageId that was received.
         * @return The current builder.
         */
        public PushNotificationBuilder setMessageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * Set the challenge that was sent with this notification.
         * @param challenge The base64 encoded challenge.
         * @return The current builder.
         */
        public PushNotificationBuilder setChallenge(String challenge) {
            this.challenge = challenge;
            return this;
        }

        /**
         * Set the he AM load balance cookie with this notification.
         * @param amlbCookie the load balance cookie.
         * @return The current builder.
         */
        public PushNotificationBuilder setAmlbCookie(String amlbCookie) {
            this.amlbCookie = amlbCookie;
            return this;
        }

        /**
         * Sets the date that the notification was received.
         * @param timeAdded The date received in UTC.
         * @return The current builder.
         */
        public PushNotificationBuilder setTimeAdded(Calendar timeAdded) {
            this.timeAdded = timeAdded;
            return this;
        }

        /**
         * Sets the date that the notification will automatically fail.
         * @param timeExpired The expiry date.
         * @return The current builder.
         */
        public PushNotificationBuilder setTimeExpired(Calendar timeExpired) {
            this.timeExpired = timeExpired;
            return this;
        }

        /**
         * Sets the time to live for the notification.
         * @param ttl The expiry date.
         * @return The current builder.
         */
        public PushNotificationBuilder setTtl(long ttl) {
            this.ttl = ttl;
            return this;
        }

        /**
         * Sets whether the authentication the notification is related to was approved.
         * @param approved True if the authentication was approved, false otherwise.
         * @return The current builder.
         */
        public PushNotificationBuilder setApproved(boolean approved) {
            this.approved = approved;
            return this;
        }

        /**
         * Sets whether the authentication the notification is related to has been handled.
         * @param pending True if the authentication has not been handled, false otherwise.
         * @return The current builder.
         */
        public PushNotificationBuilder setPending(boolean pending) {
            this.pending = pending;
            return this;
        }

        /**
         * Build the notification.
         * @return The final notification.
         */
        protected PushNotification build() {
            return new PushNotification(mechanismUID, messageId, challenge, amlbCookie, timeAdded,
                    timeExpired, ttl, approved, pending);
        }
    }

}
