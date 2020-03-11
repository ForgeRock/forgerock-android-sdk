package org.forgerock.android.authenticator;

import org.forgerock.android.authenticator.util.TimeKeeper;

/**
 * Represents a currently active token.
 */
class TokenCode {
    private final String code;
    private final long start;
    private final long until;
    private TimeKeeper timeKeeper;
    private final int MAX_VALUE = 1000;

    public TokenCode(TimeKeeper timeKeeper, String code, long start, long until) {
        this.timeKeeper = timeKeeper;
        this.code = code;
        this.start = start;
        this.until = until;
    }

    /**
     * Gets the code which is currently active.
     * @return The currently active token.
     */
    public String getCurrentCode() {
        return code;
    }

    /**
     * Returns true if the TokenCode has not yet expired.
     * @return True if the TokenCode is still valid, false otherwise.
     */
    public boolean isValid() {
        long cur = timeKeeper.getCurrentTimeMillis();

        return cur < until;
    }

    /**
     * Get the current progress of the TokenCode. This is a number between 0 and 1000, and represents
     * the amount of time that has passed between the start and end times of the code.
     * @return The total progress, a number between 0 and 1000.
     */
    public int getCurrentProgress() {
        long cur = timeKeeper.getCurrentTimeMillis();
        long total = until - start;
        long state = cur - start;
        int progress = (int) (state * MAX_VALUE / total);
        return progress < MAX_VALUE ? progress : MAX_VALUE;
    }

}
