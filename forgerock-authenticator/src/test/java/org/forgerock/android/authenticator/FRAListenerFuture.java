/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A Future represents the result of the FRAListener asynchronous result.
 * @param <T> The result type returned by this Future's get method
 */
public class FRAListenerFuture<T> extends FRAListener<T> implements Future<T> {

    private CountDownLatch countDownLatch = new CountDownLatch(1);
    private boolean done = false;
    private T result;
    private Exception exception;

    @Override
    public void onSuccess(T result) {
        this.result = result;
        this.done = true;
        countDownLatch.countDown();
    }

    @Override
    public void onException(Exception e) {
        this.exception = e;
        this.done = true;
        countDownLatch.countDown();
    }

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
    public T get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        if (!done) {
            countDownLatch.await(timeout, unit);
        }
        if (exception != null) {
            throw new ExecutionException(exception);
        }
        return result;
    }
}
