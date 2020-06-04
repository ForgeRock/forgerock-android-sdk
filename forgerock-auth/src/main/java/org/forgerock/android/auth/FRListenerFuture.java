/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import androidx.annotation.NonNull;

import java.util.concurrent.*;

/**
 * A {@code Future} represents the result of an {@link FRListener}
 *
 * @param <T> The type of the result
 */
public class FRListenerFuture<T> implements FRListener<T>, Future<T> {

    private CountDownLatch countDownLatch = new CountDownLatch(1);
    private boolean done = false;
    private T result;
    private Exception exception;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public T get() throws ExecutionException, InterruptedException {
        if (!done) {
            countDownLatch.await();
        }
        if (exception != null) {
            throw new ExecutionException(exception);
        }
        return result;
    }

    @Override
    public T get(long timeout, @NonNull TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        if (!done && !countDownLatch.await(timeout, unit)) {
            throw new TimeoutException("Timeout waiting for result");
        }
        if (exception != null) {
            throw new ExecutionException(exception);
        }
        return result;
    }

    @Override
    public void onSuccess(T token) {
        this.result = token;
        this.done = true;
        countDownLatch.countDown();
    }

    @Override
    public void onException(Exception e) {
        this.exception = e;
        this.done = true;
        countDownLatch.countDown();
    }

}
