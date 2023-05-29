/*
 * Copyright (c) 2020 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import androidx.annotation.VisibleForTesting;

import org.forgerock.android.auth.exception.AccountLockException;
import org.forgerock.android.auth.exception.OathMechanismException;
import org.forgerock.android.auth.util.Base32String;
import org.forgerock.android.auth.util.TimeKeeper;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.forgerock.android.auth.OathMechanism.TokenType.HOTP;
import static org.forgerock.android.auth.OathMechanism.TokenType.TOTP;

/**
 * This singleton is an utility used to generate TOTP and HOTP tokens for registered accounts.
 */
class OathCodeGenerator {

    private static OathCodeGenerator INSTANCE = null;

    private static final String TAG = OathCodeGenerator.class.getSimpleName();

    /** StorageClient to persist OathMechanism updates **/
    private StorageClient storageClient;

    /**
     * Return the OathCodeGenerator instance
     *
     * @return OathCodeGenerator instance
     */
    static OathCodeGenerator getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("OathCodeGenerator is not initialized. " +
                    "Please make sure to call OathCodeGenerator#init first.");
        }
        return INSTANCE;
    }

    /**
     * Initialize/Return the OathCodeGenerator instance
     *
     * @return OathCodeGenerator instance
     */
    static OathCodeGenerator getInstance(StorageClient storageClient) {
        synchronized (OathCodeGenerator.class) {
            if (INSTANCE == null) {
                INSTANCE = new OathCodeGenerator(storageClient);
            }
            return INSTANCE;
        }
    }

    /**
     * Private constructor restricted to this class itself
     */
    private OathCodeGenerator(StorageClient storageClient) {
        this.storageClient = storageClient;
    }

    /**
     * Generates a new set of codes for this token.
     * @return The new active token.
     * @throws OathMechanismException If an error occur on generating OTP codes
     */
    OathTokenCode generateNextCode(OathMechanism oath, TimeKeeper timeKeeper)
            throws OathMechanismException, AccountLockException {
        Logger.debug(TAG, "Generating next OTP code.");

        if(oath.getAccount() != null && oath.getAccount().isLocked()) {
            throw new AccountLockException("Error generating next OTP code: Account is locked.");
        }

        long currentTime = timeKeeper.getCurrentTimeMillis();
        String otp;
        try {
            switch (oath.getOathType()) {
                case HOTP:
                    ((HOTPMechanism) oath).incrementCounter();
                    storageClient.setMechanism(oath);
                    otp = getOTP(((HOTPMechanism) oath).getCounter(), oath.getDigits(), oath.getSecret(), oath.getAlgorithm());
                    return new OathTokenCode(timeKeeper, otp, currentTime, 0, HOTP);
                case TOTP:
                    long counter = currentTime / 1000 / ((TOTPMechanism) oath).getPeriod();
                    otp = getOTP(counter + 0, oath.getDigits(), oath.getSecret(), oath.getAlgorithm());
                    return new OathTokenCode(timeKeeper, otp,
                            (counter + 0) * ((TOTPMechanism) oath).getPeriod() * 1000,
                            (counter + 1) * ((TOTPMechanism) oath).getPeriod() * 1000, TOTP);
            }
        } catch (InvalidKeyException e) {
            Logger.warn(TAG, e,"Invalid secret used");
            throw new OathMechanismException("Error generating next OTP code: Invalid secret used.", e);
        } catch (NoSuchAlgorithmException e) {
            Logger.warn(TAG, e, "Invalid algorithm used: %s", oath.getAlgorithm());
            throw new OathMechanismException("Error generating next OTP code: Invalid algorithm used.", e);
        } catch (Base32String.DecodingException e) {
            Logger.warn(TAG, e,"Could not decode secret.");
            throw new OathMechanismException("Error generating next OTP code: Could not decode secret.", e);
        }

        return null;
    }

    /**
     * Compute new OTP code
     */
    private static String getOTP(long counter, int digits, String secretStr, String algo)
            throws InvalidKeyException, NoSuchAlgorithmException, Base32String.DecodingException {
        // Encode counter in network byte order
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putLong(counter);

        // Create digits divisor
        int div = 1;
        for (int i = digits; i > 0; i--) {
            div *= 10;
        }

        // Create the HMAC
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
        StringBuilder hotp = new StringBuilder(Integer.toString(binary));
        while (hotp.length() != digits)
            hotp.insert(0, "0");

        Logger.debug(TAG, "New OTP code generated successfully.");

        return hotp.toString();
    }

    @VisibleForTesting
    static void reset() {
        INSTANCE = null;
    }

}
