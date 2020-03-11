package org.forgerock.android.authenticator.exception;

/**
 * Represents an error in setting up a mechanism.
 */
public class MechanismCreationException extends Exception {
    /**
     * Create a new exception containing a message.
     * @param detailMessage The message cause of the exception.
     * @param throwable The throwable cause of the exception.
     */
    public MechanismCreationException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    /**
     * Create a new exception containing a message.
     * @param detailMessage The message cause of the exception.
     */
    public MechanismCreationException(String detailMessage) {
        super(detailMessage);
    }
}
