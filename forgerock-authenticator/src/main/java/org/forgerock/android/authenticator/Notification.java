package org.forgerock.android.authenticator;

import org.forgerock.android.authenticator.exception.InvalidNotificationException;

import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

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
    /** Unique identifier of the Mechanism */
    private String amlbCookie;
    /** Date that the notification was received */
    private final Calendar timeAdded;
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
     * @param ttl time-to-live value from message payload
     * @param approved boolean indicator of whether notification is still pending or not
     * @param pending boolean indicator of whether notification is approved or not
     */
    public Notification(String mechanismUID, String messageId, String challenge, String amlbCookie,
                        Calendar timeAdded, long ttl, boolean approved, boolean pending) {
        this.id = mechanismUID + "-" + timeAdded;
        this.mechanismUID = mechanismUID;
        this.messageId = messageId;
        this.challenge = challenge;
        this.amlbCookie = amlbCookie;
        this.timeAdded = timeAdded;
        this.ttl = ttl;
        this.approved = approved;
        this.pending = pending;
    }

    /**
     * Creates the Notification object with given data
     * @param mechanismUID Mechanism UUID associated with the Notification
     * @param messageId message identifier from the message payload
     * @param challenge challenge from message payload
     * @param amlbCookie load balance cookie from OpenAM
     * @param timeAdded Date when the notification is delivered
     * @param ttl time-to-live value from message payload
     */
    public Notification(String mechanismUID, String messageId, String challenge, String amlbCookie,
                        Calendar timeAdded, long ttl) {
        this.id = mechanismUID + "-" + timeAdded;
        this.mechanismUID = mechanismUID;
        this.messageId = messageId;
        this.challenge = challenge;
        this.amlbCookie = amlbCookie;
        this.timeAdded = timeAdded;
        this.ttl = ttl;
        this.approved = false;
        this.pending = true;
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
}
