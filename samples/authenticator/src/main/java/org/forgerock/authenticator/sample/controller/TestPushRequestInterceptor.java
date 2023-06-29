package org.forgerock.authenticator.sample.controller;


import android.net.Uri;

import androidx.annotation.NonNull;

import org.forgerock.android.auth.Action;
import org.forgerock.android.auth.FRRequestInterceptor;
import org.forgerock.android.auth.Request;

/**
 * This is an example http interceptor for testing purposes (SDKS-2544)
 */
public class TestPushRequestInterceptor implements FRRequestInterceptor<Action> {

    @NonNull
    @Override
    public Request intercept(@NonNull Request request, Action tag) {
        if (tag.getType().equals(Action.PUSH_REGISTER)) {
            return request.newBuilder()
                    // Add query parameter:
                    .url(Uri.parse(request.url().toString())
                            .buildUpon()
                            .appendQueryParameter("testParameter", "PUSH_REGISTER").toString())

                    // Add additional header:
                    .addHeader("testHeader", "PUSH_REGISTER")

                    // Construct the updated request:
                    .build();
        }
        else if (tag.getType().equals(Action.PUSH_AUTHENTICATE)) {
            return request.newBuilder()
                    // Add query parameter:
                    .url(Uri.parse(request.url().toString())
                            .buildUpon()
                            .appendQueryParameter("testParameter", "PUSH_AUTHENTICATE").toString())

                    // Add additional header:
                    .addHeader("testHeader", "PUSH_AUTHENTICATE")

                    // Construct the updated request:
                    .build();
        }
        return request;
    }
}