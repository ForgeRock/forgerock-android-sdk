/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import java.io.IOException;

import lombok.AccessLevel;
import lombok.Getter;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;

/**
 * The content of the {@link Request} body
 */
public class Body {

    @Getter(AccessLevel.PACKAGE)
    private RequestBody requestBody;
    @Getter
    private byte[] content;
    @Getter
    private String contentType;

    Body(RequestBody requestBody) {
        this.requestBody = requestBody;
        try (Buffer buffer = new Buffer()) {
            requestBody.writeTo(buffer);
            content = buffer.readByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        if (requestBody.contentType() != null) {
            contentType = requestBody.contentType().toString();
        }
    }

    /**
     * Construct body with byte[]
     *
     * @param content     The body content
     * @param contentType body Content Type
     */
    public Body(byte[] content, String contentType) {
        this(RequestBody.create(content, MediaType.parse(contentType)));
    }

    /**
     * Construct body with String
     *
     * @param content     The body content
     * @param contentType body Content Type
     */
    public Body(String content, String contentType) {
        this(RequestBody.create(content, MediaType.parse(contentType)));
    }

}
