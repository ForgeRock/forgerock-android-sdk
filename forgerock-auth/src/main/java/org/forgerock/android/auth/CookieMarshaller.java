/*
 * Copyright (c) 2020 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.util.Base64;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;

import okhttp3.Cookie;

/**
 * Class to support marshal and unmarshal of {@link Cookie} Object
 */
class CookieMarshaller {

    private static String TAG = CookieMarshaller.class.getSimpleName();

    /**
     * Write out the {@link Cookie} Object to String
     *
     * @param cookie The Cookie
     * @return The marshalled String
     */
    String marshal(@NonNull Cookie cookie) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream outputStream = new ObjectOutputStream(baos)) {
            outputStream.writeObject(new SerializableCookie(cookie));
            return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        } catch (Exception e) {
            Logger.warn(TAG, "Failed to marshal the cookie to String.", e);
            return null;
        }
    }

    /**
     * Read a {@link String} Object and unmarshal to {@link Cookie} Object.
     *
     * @param cookie The String representation of the Cookie.
     * @return The Cookie Object.
     */
    Cookie unmarshal(@NonNull String cookie) {
        try {
            Base64.decode(cookie, Base64.DEFAULT);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(Base64.decode(cookie, Base64.DEFAULT));
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream) {
                @Override
                protected Class<?> resolveClass(ObjectStreamClass desc) throws ClassNotFoundException, IOException {
                    if (desc.getName().equals(SerializableCookie.class.getName())) {
                        return super.resolveClass(desc);
                    }
                    throw new InvalidClassException("Unsupported class:", desc.getName());
                }
            };
            return ((SerializableCookie) objectInputStream.readObject()).getCookie();
        } catch (Exception e) {
            Logger.warn(TAG, "Failed to unmarshal the cookie from String.", e);
            return null;
        }
    }

}
