/*
 * Copyright (c) 2019 - 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.interceptor;

import android.net.Uri;

import androidx.annotation.NonNull;

import org.forgerock.android.auth.InitProvider;
import org.forgerock.android.auth.PolicyAdvice;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.StatusLine;

/**
 * Reference Implementation of using {@link Interceptor} to handle advice from ForgeRock Identity Gateway
 */
public abstract class IdentityGatewayAdviceInterceptor<T> implements Interceptor {

    @NonNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {

        Response response = chain.proceed(chain.request());

        if (!response.isSuccessful() && response.code() == StatusLine.HTTP_TEMP_REDIRECT) {
            String location = response.header("location");
            PolicyAdvice advice = getAdvice(location);
            if (advice != null) {
                try {
                    getAdviceHandler(advice)
                            .onAdviceReceived(InitProvider.getCurrentActivity(), advice).get();
                } catch (ExecutionException e) {
                    return response;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return response;
                }
                //Discard the existing response
                try {
                    response.close();
                } catch (Exception e) {
                    //ignore
                }
                //Retry the request
                return chain.proceed(decorateRequest(chain.request(), advice));
            }
        }
        return response;
    }

    /**
     * Get the {@link AdviceHandler} to handle the advice request.
     *
     * @param advice The Advice
     * @return An {@link AdviceHandler} to handle the advice.
     */
    public abstract AdviceHandler<T> getAdviceHandler(PolicyAdvice advice);

    /**
     * Extract the Advice from the location redirect url
     *
     * @param location The redirect location
     * @return Policy Advice or null if advice not found.
     */
    private PolicyAdvice getAdvice(String location) {
        try {
            Uri redirect = Uri.parse(location);
            String advice = redirect.getQueryParameter("authIndexValue");
            if (advice != null) {
                return PolicyAdvice.parse(advice);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Decorate the request with additional parameter which required for Policy Advice
     *
     * @param original The original Request
     * @param advice   The Advice
     * @return The decorated Request.
     */
    private Request decorateRequest(Request original, PolicyAdvice advice) {
        if (advice.getType() == PolicyAdvice.TRANSACTION_CONDITION_ADVICE) {
            //Add _txid to the original query parameter
            HttpUrl originalHttpUrl = original.url();

            HttpUrl url = originalHttpUrl.newBuilder()
                    .addQueryParameter("_txid", advice.getValue())
                    .build();

            return original.newBuilder()
                    .url(url).build();
        } else {
            return original;
        }
    }
}
