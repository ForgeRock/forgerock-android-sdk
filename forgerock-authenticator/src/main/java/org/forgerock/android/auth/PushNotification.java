/*
 * Copyright (c) 2020 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.biometric.BiometricPrompt.AuthenticationCallback;
import androidx.fragment.app.FragmentActivity;

import org.forgerock.android.auth.biometric.BiometricAuth;
import org.forgerock.android.auth.exception.AccountLockException;
import org.forgerock.android.auth.exception.InvalidNotificationException;
import org.forgerock.android.auth.exception.PushMechanismException;
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
    private final String id;
    /** Unique identifier of the Mechanism associated with this PushNotification */
    private final String mechanismUID;
    /** Message Identifier that was received with this notification */
    private final String messageId;
    /** Base64 challenge that was sent with this notification */
    private final String challenge;
    /** The AM load balance cookie */
    private final String amlbCookie;
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
    /** The JSON String containing the custom attributes added to this notification */
    private final String customPayload;
    /** Message that was received with this notification */
    private final String message;
    /** The mechanism associated with this notification **/
    private PushMechanism pushMechanism;
    /** The type of push notification **/
    private final PushType pushType;
    /** The numbers used in the push challenge **/
    private final String numbersChallenge;
    /** The context information to this notification. */
    private final String contextInfo;

    private static final String TAG = PushNotification.class.getSimpleName();

    /**
     * Creates the PushNotification object with given data
     * @param mechanismUID Mechanism UUID associated with the PushNotification
     * @param messageId message identifier from the notification payload
     * @param message message from the notification payload
     * @param challenge challenge from message payload
     * @param amlbCookie load balance cookie from OpenAM
     * @param timeAdded Date when the notification is delivered
     * @param timeExpired Date when the notification was expired
     * @param ttl time-to-live value from message payload
     * @param approved boolean indicator of whether notification is still pending or not
     * @param pending boolean indicator of whether notification is approved or not
     * @param customPayload JSON String containing the custom attributes
     * @param numbersChallenge numbers used in the push challenge
     * @param contextInfo contextual information, such as location
     * @param pushType the type of push notification
     */
    private PushNotification(String mechanismUID, String messageId, String message, String challenge,
                             String amlbCookie, Calendar timeAdded, Calendar timeExpired, long ttl,
                             boolean approved, boolean pending, String customPayload,
                             String numbersChallenge, String contextInfo, PushType pushType) {
        this.id = mechanismUID + "-" + timeAdded.getTimeInMillis();
        this.mechanismUID = mechanismUID;
        this.messageId = messageId;
        this.message = message;
        this.challenge = challenge;
        this.amlbCookie = amlbCookie;
        this.timeAdded = timeAdded;
        this.timeExpired = timeExpired;
        this.ttl = ttl;
        this.approved = approved;
        this.pending = pending;
        this.customPayload = customPayload;
        this.numbersChallenge = numbersChallenge;
        this.contextInfo = contextInfo;
        this.pushType = pushType;
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
     * Get the message of this notification
     * @return The message value
     */
    public String getMessage() {
        return message;
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
     * @return The date the notification was received.
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
     * Get the contextual information to this notification.
     * @return JSON String containing context information to this notification.
     */
    public String getContextInfo() {
        return contextInfo;
    }

    /**
     * Get the custom attributes added to this notification.
     * @return JSON String containing custom attributes added to the payload of this notification.
     */
    public String getCustomPayload() {
        return customPayload;
    }

    /**
     * Get the type of Push notification.
     * @return the push type.
     */
    public PushType getPushType() {
        return pushType;
    }

    /**
     * Get numbers used for push challenge
     * @return the numbers as int array. Returns {null} if "numbersChallenge" is not available
     */
    public int[] getNumbersChallenge () {
        int[] numbers = null;

        if(this.numbersChallenge != null) {
            String[] strArray = this.numbersChallenge.split(",");
            numbers = new int[strArray.length];
            for(int i = 0; i < strArray.length; i++) {
                numbers[i] = Integer.parseInt(strArray[i]);
            }
        }

        return numbers;
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
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", getId());
            jsonObject.put("mechanismUID", getMechanismUID());
            jsonObject.put("messageId", getMessageId());
            jsonObject.put("message", getMessage());
            jsonObject.put("challenge", getChallenge());
            jsonObject.put("amlbCookie", getAmlbCookie());
            jsonObject.put("timeAdded", getTimeAdded() != null ? getTimeAdded().getTimeInMillis() : null);
            jsonObject.put("timeExpired", getTimeExpired() != null ? getTimeExpired().getTimeInMillis() : null);
            jsonObject.put("ttl", getTtl());
            jsonObject.put("approved", isApproved());
            jsonObject.put("pending", isPending());
            jsonObject.put("customPayload", getCustomPayload());
            jsonObject.put("numbersChallenge", this.numbersChallenge);
            jsonObject.put("contextInfo", getContextInfo());
            if(getPushType() != null) {
                jsonObject.put("pushType", getPushType().toString());
            }
        } catch (JSONException e) {
            Logger.warn(TAG, e, "Error parsing PushNotification object to JSON for messageId: %s",
                    messageId);
            throw new RuntimeException("Error parsing PushNotification object to JSON string representation.", e);
        }
        return jsonObject.toString();
    }

    @Override
    String serialize() {
        return this.toJson();
    }

    /**
     * Accepts the push authentication request. Use this method to approve notification of
     * type {@code PushType.DEFAULT}.
     * @param listener Listener for receiving the authentication result.
     */
    public final void accept(@NonNull FRAListener<Void> listener) {
        if(this.pushMechanism.getAccount() != null && this.pushMechanism.getAccount().isLocked()) {
            listener.onException(new AccountLockException("Unable to process the Push " +
                    "Authentication request: Account is locked."));
        } else if (this.pushType == PushType.DEFAULT) {
            Logger.debug(TAG, "Accept Push Authentication request for message: %s", getMessageId());
            performAcceptDenyAsync(true, listener);
        } else {
            listener.onException(new PushMechanismException("Error processing the Push " +
                    "Authentication request. This method cannot be used to process notification " +
                    "of type: " + this.pushType));
        }
    }

    /**
     * Accepts the push notification request with the challenge response. Use this method to handle
     * notification of type {@code PushType.CHALLENGE}.
     * @param challengeResponse the response for the Push Challenge
     * @param listener Listener for receiving the authentication result.
     */
    public final void accept(@NonNull String challengeResponse, @NonNull FRAListener<Void> listener) {
        if(this.pushMechanism.getAccount() != null && this.pushMechanism.getAccount().isLocked()) {
            listener.onException(new AccountLockException("Unable to process the Push " +
                    "Authentication request: Account is locked."));
        } else if (this.pushType == PushType.CHALLENGE) {
            Logger.debug(TAG, "Respond the challenge for message: %s", getMessageId());
            PushResponder.getInstance().authenticationWithChallenge(this, challengeResponse, listener);
        } else {
            listener.onException(new PushMechanismException("Error processing the Push " +
                    "Authentication request. This method cannot be used to process notification " +
                    "of type: " + this.pushType));
        }
    }

    /**
     * Accepts the push notification request with Biometric Authentication. Use this method to handle
     * notification of type {@code PushType.BIOMETRIC}.
     * @param title the title to be displayed on the prompt.
     * @param subtitle the subtitle to be displayed on the prompt.
     * @param allowDeviceCredentials if {@code true}, accepts device PIN, pattern, or password to process notification.
     * @param activity the activity of the client application that will host the prompt.
     * @param listener listener for receiving the push authentication result.
     */
    public final void accept(String title,
                             String subtitle,
                             boolean allowDeviceCredentials,
                             @NonNull FragmentActivity activity,
                             @NonNull FRAListener<Void> listener) {
        if(this.pushMechanism.getAccount() != null && this.pushMechanism.getAccount().isLocked()) {
            listener.onException(new AccountLockException("Unable to process the Push " +
                    "Authentication request: Account is locked."));
        } else if (this.pushType == PushType.BIOMETRIC) {
            final PushNotification pushNotification = this;
            BiometricAuth biometricAuth = new BiometricAuth(title,
                    subtitle, allowDeviceCredentials, activity, new AuthenticationCallback() {

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    Logger.debug(TAG, "Respond the challenge for message: %s", getMessageId());
                    PushResponder.getInstance().authentication(pushNotification, true, listener);
                }

                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    listener.onException(new PushMechanismException("Error processing the Push " +
                            "Authentication request. Biometric Authentication failed: " + errString));
                }

                @Override
                public void onAuthenticationFailed() {
                    //Ignore to allow fingerprint retry
                }
            });
            biometricAuth.authenticate();
        } else {
            listener.onException(new PushMechanismException("Error processing the Push " +
                    "Authentication request. This method cannot be used to process notification " +
                    "of type: " + this.pushType));
        }
    }

    /**
     * Deny any type of push authentication request.
     * @param listener Listener for receiving the HTTP call response code.
     */
    public final void deny(@NonNull FRAListener<Void> listener) {
        if(this.pushMechanism.getAccount() != null && this.pushMechanism.getAccount().isLocked()) {
            listener.onException(new AccountLockException("Unable to process the Push " +
                    "Authentication request: Account is locked."));
        } else {
            Logger.debug(TAG, "Deny Push Authentication request for message: %s", getMessageId());
            performAcceptDenyAsync(false, listener);
        }
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
    public static PushNotification deserialize(String jsonString) {
        if (jsonString == null || jsonString.length() == 0) {
            return null;
        }

        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            return PushNotification.builder()
                    .setMechanismUID(jsonObject.getString("mechanismUID"))
                    .setMessageId(jsonObject.getString("messageId"))
                    .setMessage(jsonObject.has("message") ? jsonObject.getString("message") : null)
                    .setChallenge(jsonObject.getString("challenge"))
                    .setAmlbCookie(jsonObject.has("amlbCookie") ? jsonObject.getString("amlbCookie") : null)
                    .setTimeAdded(jsonObject.has("timeAdded") ? getDate(jsonObject.optLong("timeAdded")) : null)
                    .setTimeExpired(jsonObject.has("timeExpired") ? getDate(jsonObject.optLong("timeExpired")) : null)
                    .setTtl(jsonObject.optLong("ttl", -1))
                    .setApproved(jsonObject.getBoolean("approved"))
                    .setPending(jsonObject.getBoolean("pending"))
                    .setCustomPayload(jsonObject.has("customPayload") ? jsonObject.getString("customPayload") : null)
                    .setNumbersChallenge(jsonObject.has("numbersChallenge") ? jsonObject.getString("numbersChallenge") : null)
                    .setContextInfo(jsonObject.has("contextInfo") ? jsonObject.getString("contextInfo") : null)
                    .setPushType(jsonObject.has("pushType") ? jsonObject.getString("pushType") : PushType.DEFAULT.toString())
                    .build();
        } catch (JSONException | InvalidNotificationException e) {
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
        private String message;
        private String challenge;
        private String amlbCookie;
        private Calendar timeAdded;
        private Calendar timeExpired;
        private long ttl;
        private boolean approved = false;
        private boolean pending = true;
        private String customPayload;
        private String numbersChallenge;
        private String contextInfo;
        private PushType pushType;
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
         * Sets the message that was received with this notification.
         * @param message The message that was received.
         * @return The current builder.
         */
        public PushNotificationBuilder setMessage(String message) {
            this.message = message;
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
         * Sets custom attributes associated with this notification.
         * @param customPayload JSON String containing the custom attributes
         * @return The current builder.
         */
        public PushNotificationBuilder setCustomPayload(String customPayload) {
            this.customPayload = customPayload;
            return this;
        }

        /**
         * Sets the numbers used in the Push challenge.
         * @param numbersChallenge String containing the numbers for challenge
         * @return The current builder.
         */
        public PushNotificationBuilder setNumbersChallenge(String numbersChallenge) {
            this.numbersChallenge = numbersChallenge;
            return this;
        }

        /**
         * Sets context information for this notification.
         * @param contextInfo JSON String containing the context information.
         * @return The current builder.
         */
        public PushNotificationBuilder setContextInfo(String contextInfo) {
            this.contextInfo = contextInfo;
            return this;
        }

        /**
         * Sets the type of Push notification.
         * @param pushType the push type
         * @return The current builder.
         */
        public PushNotificationBuilder setPushType(String pushType) {
            this.pushType = PushType.fromString(pushType);
            return this;
        }

        /**
         * Build the notification.
         * @return The final notification.
         * @throws InvalidNotificationException if timeAdded or mechanismUID are not provided
         */
        protected PushNotification build() throws InvalidNotificationException {
            if(timeAdded == null) {
                throw new InvalidNotificationException("timeAdded cannot be null.");
            }
            if(mechanismUID == null) {
                throw new InvalidNotificationException("mechanismUID cannot be null.");
            }

            return new PushNotification(mechanismUID, messageId, message, challenge, amlbCookie,
                    timeAdded, timeExpired, ttl, approved, pending, customPayload, numbersChallenge, contextInfo, pushType);
        }
    }

}
