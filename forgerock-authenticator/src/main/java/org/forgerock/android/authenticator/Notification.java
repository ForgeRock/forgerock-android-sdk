/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

import java.util.Calendar;

/**
 * Notification is a model class which represents a message that was received from an external
 * source. A notification could be raised against any mechanism. Currently used by Push mechanism.
 */
public class Notification extends ModelObject<Notification> {

    /** Unique identifier for Notification object associated with the mechanism */
    private String id;
    /** Unique identifier of the Mechanism associated with this Notification */
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
    /** Determines if the Notification has been approved by the user. */
    private boolean approved;
    /** Determines if the Notification has been interacted with the user. */
    private boolean pending;

    /**
     * Creates the Notification object with given data
     * @param mechanismUID Mechanism UUID associated with the Notification
     * @param messageId message identifier from the message payload
     * @param challenge challenge from message payload
     * @param amlbCookie load balance cookie from OpenAM
     * @param timeAdded Date when the notification is delivered
     * @param timeExpired Date when the notification was expired
     * @param ttl time-to-live value from message payload
     * @param approved boolean indicator of whether notification is still pending or not
     * @param pending boolean indicator of whether notification is approved or not
     */
    private Notification(String mechanismUID, String messageId, String challenge, String amlbCookie,
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
     * Gets the unique identifier for the Notification.
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
     * Determines if the Notification has been interacted with by the user.
     * @return True if the Notification has not been interacted with, false otherwise.
     */
    public final boolean isPending() {
        return pending;
    }

    @Override
    public boolean matches(Notification other) {
        if (other == null) {
            return false;
        }
        return mechanismUID.equals(other.getMechanismUID()) && timeAdded.getTimeInMillis() == other.timeAdded.getTimeInMillis();
    }

    @Override
    public int compareTo(Notification another) {
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

        Notification that = (Notification) o;

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
     * Returns a builder for creating a Notification.
     * @return The Notification builder.
     */
    public static NotificationBuilder builder() {
        return new NotificationBuilder();
    }

    /**
     * Builder class responsible for producing Notifications.
     */
    public static class NotificationBuilder {
        private String mechanismUID;
        private String messageId;
        private String challenge;
        private String amlbCookie;
        private Calendar timeAdded;
        private Calendar timeExpired;
        private long ttl;
        private boolean approved;
        private boolean pending;

        /**
         * Sets the mechanism unique Id.
         * @param mechanismUID the mechanism unique Id.
         * @return The current builder.
         */
        public NotificationBuilder setMechanismUID(String mechanismUID) {
            this.mechanismUID = mechanismUID;
            return this;
        }

        /**
         * Sets the message id that was received with this notification.
         * @param messageId The messageId that was received.
         * @return The current builder.
         */
        public NotificationBuilder setMessageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * Set the challenge that was sent with this notification.
         * @param challenge The base64 encoded challenge.
         * @return The current builder.
         */
        public NotificationBuilder setChallenge(String challenge) {
            this.challenge = challenge;
            return this;
        }

        /**
         * Set the he AM load balance cookie with this notification.
         * @param amlbCookie the load balance cookie.
         * @return The current builder.
         */
        public NotificationBuilder setAmlbCookie(String amlbCookie) {
            this.amlbCookie = amlbCookie;
            return this;
        }

        /**
         * Sets the date that the notification was received.
         * @param timeAdded The date received in UTC.
         * @return The current builder.
         */
        public NotificationBuilder setTimeAdded(Calendar timeAdded) {
            this.timeAdded = timeAdded;
            return this;
        }

        /**
         * Sets the date that the notification will automatically fail.
         * @param timeExpired The expiry date.
         * @return The current builder.
         */
        public NotificationBuilder setTimeExpired(Calendar timeExpired) {
            this.timeExpired = timeExpired;
            return this;
        }

        /**
         * Sets the time to live for the notification.
         * @param ttl The expiry date.
         * @return The current builder.
         */
        public NotificationBuilder setTtl(long ttl) {
            this.ttl = ttl;
            return this;
        }

        /**
         * Sets whether the authentication the notification is related to was approved.
         * @param approved True if the authentication was approved, false otherwise.
         * @return The current builder.
         */
        public NotificationBuilder setApproved(boolean approved) {
            this.approved = approved;
            return this;
        }

        /**
         * Sets whether the authentication the notification is related to has been handled.
         * @param pending True if the authentication has not been handled, false otherwise.
         * @return The current builder.
         */
        public NotificationBuilder setPending(boolean pending) {
            this.pending = pending;
            return this;
        }

        /**
         * Build the notification.
         * @return The final notification.
         */
        protected Notification build() {
            return new Notification(mechanismUID, messageId, challenge, amlbCookie, timeAdded,
                    timeExpired, ttl, approved, pending);
        }
    }

}
