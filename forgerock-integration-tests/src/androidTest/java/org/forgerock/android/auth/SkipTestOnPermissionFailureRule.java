package org.forgerock.android.auth;

import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import junit.framework.AssertionFailedError;

public class SkipTestOnPermissionFailureRule implements TestRule {

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    base.evaluate();
                } catch (AssertionFailedError e) {
                    if (e.getMessage().contains("Failed to grant permissions")) {
                        throw new AssumptionViolatedException("Skipping test due to failure to grant permissions");
                    } else {
                        throw e; // Re-throw other AssertionFailedError
                    }
                }
            }
        };
    }
}