package org.forgerock.android.authenticator.exception;

/**
 * Represents an error in attaching a Notification to a Mechanism.
 */
public class InvalidNotificationException extends Exception {

    /**
     * Create a new exception containing a message.
     * @param detailMessage The message cause of the exception.
     */
    public InvalidNotificationException(String detailMessage) {
        super(detailMessage);
    }
}
