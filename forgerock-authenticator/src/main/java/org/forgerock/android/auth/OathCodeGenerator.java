/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.util.Base32String;
import org.forgerock.android.auth.util.TimeKeeper;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

class OathCodeGenerator {

    private static final String TAG = OathCodeGenerator.class.getSimpleName();

    /**
     * Generates a new set of codes for this token.
     * @return The new active token.
     */
    static OathTokenCode generateNextCode(Oath oath, TimeKeeper timeKeeper) {
        Logger.debug(TAG, "Generating next OTP code.");

        long currentTime = timeKeeper.getCurrentTimeMillis();
        String otp;

        switch (oath.getOathType()) {
            case HOTP:
                oath.incrementCounter();
                otp = getOTP(oath.getCounter(), oath.getDigits(), oath.getSecret(), oath.getAlgorithm());
                return new OathTokenCode(timeKeeper, otp, currentTime, currentTime + (oath.getPeriod() * 1000));

            case TOTP:
                long counter = currentTime / 1000 / oath.getPeriod();
                otp = getOTP(counter + 0, oath.getDigits(), oath.getSecret(), oath.getAlgorithm());
                return new OathTokenCode(timeKeeper, otp,
                        (counter + 0) * oath.getPeriod() * 1000,
                        (counter + 1) * oath.getPeriod() * 1000);
        }

        return null;
    }

    /**
     * Compute new OTP code
     */
    private static String getOTP(long counter, int digits, String secretStr, String algo) {
        // Encode counter in network byte order
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putLong(counter);

        // Create digits divisor
        int div = 1;
        for (int i = digits; i > 0; i--) {
            div *= 10;
        }

        // Create the HMAC
        try {
            byte[] secret = Base32String.decode(secretStr);
            Mac mac = Mac.getInstance("Hmac" + algo);
            mac.init(new SecretKeySpec(secret, "Hmac" + algo));

            // Do the hashing
            byte[] digest = mac.doFinal(bb.array());

            // Truncate
            int binary;
            int off = digest[digest.length - 1] & 0xf;
            binary = (digest[off] & 0x7f) << 0x18;
            binary |= (digest[off + 1] & 0xff) << 0x10;
            binary |= (digest[off + 2] & 0xff) << 0x08;
            binary |= (digest[off + 3] & 0xff);
            binary = binary % div;

            // Zero pad
            String hotp = Integer.toString(binary);
            while (hotp.length() != digits)
                hotp = "0" + hotp;

            Logger.debug(TAG, "New OTP code generated successfully.");

            return hotp;
        } catch (InvalidKeyException e) {
            Logger.warn(TAG, e,"Invalid key used");
        } catch (NoSuchAlgorithmException e) {
            Logger.warn(TAG, e, "Invalid algorithm used");
        } catch (Base32String.DecodingException e) {
            Logger.warn(TAG, e,"Could not decode secret: %s", secretStr);
        }

        return "";
    }

}
