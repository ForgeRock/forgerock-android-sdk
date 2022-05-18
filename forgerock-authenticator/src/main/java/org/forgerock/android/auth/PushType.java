/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import androidx.annotation.NonNull;

/**
 * The supported Push types.
 */
public enum PushType {

    /**
     * Default. Push to accept notification.
     */
    DEFAULT {
        @NonNull
        @Override
        public String toString() {
            return "default";
        }
    },
    /**
     * Push to Challenge notification.
     */
    CHALLENGE {
        @NonNull
        @Override
        public String toString() {
            return "challenge";
        }
    },
    /**
     * Push to Biometric notification.
     */
    BIOMETRIC {
        @NonNull
        @Override
        public String toString() {
            return "biometric";
        }
    };

    /**
     * Return the push type for a given string.
     * @param type a String representing the PushType.
     * @return the push type. Returns DEFAULT if the type parameter is {null} or invalid.
     */
    public static PushType fromString(String type) {
        if (type == null) {
            return PushType.DEFAULT;
        }

        try {
            return PushType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PushType.DEFAULT;
        }
    }

}