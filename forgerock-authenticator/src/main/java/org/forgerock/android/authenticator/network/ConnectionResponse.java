/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator.network;

import androidx.annotation.NonNull;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import okhttp3.Response;

public class ConnectionResponse {

    /** The response header */
    private Map<String, List<String>> header;

    /** The response code */
    private int code;

    /** The response message */
    private String message;

    /** The response Content length */
    private int contentLength;

    /** The response body */
    private InputStream body;

    /**
     * Representation of the response from the network connection
     * @param header response-header fields
     * @param code response code
     * @param message response message
     * @param contentLength content length in bytes
     * @param body response body
     */
    public ConnectionResponse(Map<String, List<String>> header, int code, String message,
                              int contentLength, InputStream body) {
        this.header = header;
        this.code = code;
        this.message = message;
        this.contentLength = contentLength;
        this.body = body;
    }

    /**
     * Representation of the response from the network connection
     * @param response response object
     */
    public ConnectionResponse(@NonNull Response response) {

        this.code = response.code();
        this.message =  response.message();

        if (response != null && response.headers() != null) {
            this.header = response.headers().toMultimap();
        }

        if (response != null && response.body() != null) {
            this.body = response.body().byteStream();
        }

        if (response != null && response.body() != null) {
            this.contentLength = (int) response.body().contentLength();
        }
    }

    /**
     * Returns the content length in bytes specified by the response header field.
     *
     * @return the value of the response header field content-length.
     */
    public int getContentLength() {
        return contentLength;
    }

    /**
     * Returns a map of the response-header fields and values.
     *
     * @return The response headers
     */
    public Map<String, List<String>> getAllHeaders() {
        return this.header;
    }

    /**
     * Returns the response code returned by the remote server.
     *
     * @return The response code
     */
    public int getResponseCode() {
        return this.code;
    }

    /**
     * Returns the response message returned by the remote server.
     *
     * @return The response message
     */
    public String getResponseMessage() {
        return this.message;
    }

    /**
     * Returns the response body returned by the remote server
     *
     * @return The response body
     */
    public InputStream getBody() {
        return this.body;
    }

}
