/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.callback;

import android.text.TextUtils;

import com.google.android.material.textfield.TextInputLayout;

import org.forgerock.android.auth.callback.AbstractValidatedCallback;

public abstract class AbstractValidatedCallbackFragment<T extends AbstractValidatedCallback> extends CallbackFragment<T> {

    protected String getErrorMessage(String prompt, AbstractValidatedCallback.FailedPolicy failedPolicy) {
        int identifier = getResources().getIdentifier(failedPolicy.getPolicyRequirement(), "string", getContext().getPackageName());
        if (identifier == 0) {
            return failedPolicy.getPolicyRequirement();
        } else {
            return failedPolicy.format(prompt, getResources().getString(identifier));
        }
    }

    protected String getErrorMessage() {
        final StringBuilder sb = new StringBuilder();
        if (!callback.getFailedPolicies().isEmpty()) {
            for (AbstractValidatedCallback.FailedPolicy fp : callback.getFailedPolicies()) {
                sb.append(getErrorMessage(callback.getPrompt(), fp));
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public void setError(TextInputLayout textInputLayout) {
        String errorMessage = getErrorMessage();
        if (!TextUtils.isEmpty(errorMessage)) {
            textInputLayout.setError(errorMessage);
        }
    }


}
