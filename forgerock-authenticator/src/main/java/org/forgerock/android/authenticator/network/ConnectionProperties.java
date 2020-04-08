/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator.network;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents the network connection properties within a Request-URI.
 */
public class ConnectionProperties {

    /** The CONTENT_TYPE key */
    public static final String CONTENT_TYPE = "Content-Type";

    /** The DEFAULT_CONTENT_TYPE value */
    public static final String DEFAULT_CONTENT_TYPE = "application/json";

    /** The COOKIE key */
    public static final String COOKIE = "Cookie";

    /** The default connection timeout value in seconds */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 30;

    private RequestMethod requestMethod;
    private Map<String, String> headerParameters;
    private Map<String, String> bodyParameters;
    private int connectionTimeout;

    /**
     * Instantiates a new NetworkConnectionProperties.
     *
     * @param builder the connection property builder
     */
    ConnectionProperties(PropertyBuilder builder) {
        requestMethod = builder.requestMethod;
        headerParameters = builder.headerParameters;
        bodyParameters = builder.bodyParameters;
        connectionTimeout = builder.connectionTimeout;
    }

    /**
     * Request HTTP method.
     *
     * @return the request method
     */
    public RequestMethod requestMethod() {
        return requestMethod;
    }

    /**
     * Header parameters map.
     *
     * @return the map
     */
    public Map<String, String> headerParameters() {
        return headerParameters;
    }

    /**
     * Body parameters map.
     *
     * @return the map
     */
    public Map<String, String> bodyParameters() {
        return bodyParameters;
    }

    /**
     * Connection timeout in seconds.
     *
     * @return the int
     */
    public int connectionTimeout() {
        return connectionTimeout > 0 ? connectionTimeout : DEFAULT_CONNECTION_TIMEOUT;
    }

    /**
     * The Parameter builder.
     */
    public static final class PropertyBuilder {
        private RequestMethod requestMethod;
        private Map<String, String> headerParameters;
        private Map<String, String> bodyParameters;
        private int connectionTimeout = -1;

        /**
         * Instantiates a new PropertyBuilder.
         */
        public PropertyBuilder() {
        }

        /**
         * Build network connection properties.
         *
         * @return the connection properties
         */
        public ConnectionProperties build() {
            if (headerParameters == null) {
                headerParameters = new HashMap<>();
            }
            return new ConnectionProperties(this);
        }

        /**
         * Sets request method.
         *
         * @param method the method
         * @return the request method
         */
        public PropertyBuilder setRequestMethod(@NonNull RequestMethod method) {
            requestMethod = method;
            return this;
        }

        /**
         * Sets a single header parameter.
         *
         * @param key   the key
         * @param value the value
         * @return the request property
         */
        public PropertyBuilder setHeaderParameter(@NonNull String key,
                                                  @NonNull String value) {
            if (headerParameters == null) {
                headerParameters = new HashMap<>();
            }
            headerParameters.put(key, value);
            return this;
        }

        /**
         * Sets multiple header parameters.
         *
         * @param map the map
         * @return the builder
         */
        public PropertyBuilder setHeaderParameters(@NonNull Map<String, String> map) {
            if (headerParameters == null) {
                headerParameters = new HashMap<>();
            }
            headerParameters.putAll(map);
            return this;
        }

        /**
         * Sets connection timeout in seconds.
         *
         * @param timeOut the time out
         * @return the builder
         */
        public PropertyBuilder setConnectionTimeout(int timeOut) {
            connectionTimeout = timeOut;
            return this;
        }

        /**
         * Sets a single body parameter.
         *
         * @param key   the key
         * @param value the value
         * @return the builder
         */
        public PropertyBuilder setBodyParameter(@NonNull String key, @NonNull String value) {
            if (bodyParameters == null) {
                bodyParameters = new HashMap<>();
            }
            bodyParameters.put(key, value);
            return this;
        }

        /**
         * Sets multiple body parameters.
         *
         * @param map the map containing the parameters
         * @return the builder
         */
        public PropertyBuilder setBodyParameters(@NonNull Map<String, String> map) {
            if (bodyParameters == null) {
                bodyParameters = new HashMap<>();
            }
            bodyParameters.putAll(map);
            return this;
        }

    }
}
