/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import androidx.annotation.NonNull;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

import kotlin.Pair;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Represent an HTTP request. Instances of this class are immutable,
 * use {@link #newBuilder()} to clone the the existing request for customization.
 */
public class Request {

    @Getter(AccessLevel.PACKAGE)
    private okhttp3.Request internalReq;

    Request(@NonNull okhttp3.Request request) {
        this.internalReq = request;
    }

    public Builder newBuilder() {
        return new Builder(internalReq.newBuilder());
    }

    public URL url() {
        return internalReq.url().url();
    }

    public Iterator<Pair<String, String>> headers() {
        return internalReq.headers().iterator();
    }

    public String header(String name) {
        return internalReq.header(name);
    }

    public List<String> headers(String name) {
        return internalReq.headers(name);
    }

    public String method() {
        return internalReq.method();
    }

    public Object tag() {
        return internalReq.tag();
    }

    public Body body() {
        if (internalReq.body() != null) {
            return new Body(internalReq.body());
        }
        return null;
    }


    public static class Builder {
        private okhttp3.Request.Builder builder;

        Builder(@NonNull okhttp3.Request.Builder builder) {
            this.builder = builder;
        }

        public Builder url(URL url) {
            this.builder.url(url);
            return this;
        }

        public Builder url(String url) {
            this.builder.url(url);
            return this;
        }

        /**
         * Sets or replaces a header.
         *
         * @param name Header name
         * @param value Header Value
         */
        public Builder header(String name, String value) {
            this.builder.header(name, value);
            return this;
        }


        /**
         * Adds a header. This is for multiply-valued headers.
         *
         * @param name Header name
         * @param value Header Value
         */
        public Builder addHeader(String name, String value) {
            this.builder.addHeader(name, value);
            return this;
        }

        /**
         * Remove all headers with provided header name.
         *
         * @param name Header name
         */
        public Builder removeHeader(String name) {
            this.builder.removeHeader(name);
            return this;
        }

        public Builder get() {
            this.builder.get();
            return this;
        }

        public Builder put(Body body) {
            this.builder.put(body.getRequestBody());
            return this;
        }

        public Builder post(Body body) {
            this.builder.post(body.getRequestBody());
            return this;
        }

        public Builder delete(Body body) {
            this.builder.delete(body.getRequestBody());
            return this;
        }

        public Builder delete() {
            this.builder.delete();
            return this;
        }

        public Builder patch(Body body) {
            this.builder.patch(body.getRequestBody());
            return this;
        }

        public Request build() {
            return new Request(builder.build());
        }
    }
}
