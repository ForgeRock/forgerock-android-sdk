package org.forgerock.android.authenticator.exception;

/**
 * Represents an error in parsing a Configuration URI, or in creating a Configuration
 * URI from a set of attributes.
 */
public class URIMappingException extends Exception {
    public URIMappingException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public URIMappingException(String detailMessage) {
        super(detailMessage);
    }
}
