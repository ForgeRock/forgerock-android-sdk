/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.forgerock.android.auth.exception.AccountLockException;
import org.forgerock.android.auth.exception.MechanismCreationException;
import org.forgerock.android.auth.exception.OathMechanismException;
import org.forgerock.android.auth.util.Base32String;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Random;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class DefaultStorageClientStressTest {

    private final Context context = ApplicationProvider.getApplicationContext();
    private final Random rand = new Random();

    private static final String[] ALGORITHMS = {"SHA1", "SHA256", "SHA512"};
    private static final String ISSUER = "ISSUER_";
    private static final String ACCOUNT_NAME = "ACCOUNT_NAME_";
    private static final String TAG = DefaultStorageClientStressTest.class.getSimpleName();

    private DefaultStorageClient defaultStorage;

    @Before
    public void setUp() {
        defaultStorage = new DefaultStorageClient(context);
        OathCodeGenerator.getInstance(defaultStorage);
    }

    @After
    public void cleanUp() {
        defaultStorage.removeAll();
    }

    @Test
    public void testStoreOneHundredAccounts()
            throws OathMechanismException, AccountLockException, MechanismCreationException {
        long startTime = System.currentTimeMillis();
        int numberOfAccounts = 100;

        // Create accounts
        for (int i = 0; i < numberOfAccounts; i++) {
            String issuer = ISSUER + i;
            String accountName = ACCOUNT_NAME + i;
            OathMechanism.TokenType tokenType = getRandomTokenType();
            int period = getRandomPeriod();
            int digits = getRandomDigits();
            int counter = getRandomCounter();
            String algorithm = getRandomAlgorithm();
            String mechanismUid = getRandomMechanismUid();
            String secret = getRandomSharedSecret();

            Account account = MockModelBuilder.createAccount(issuer, accountName);
            Mechanism mechanism = MockModelBuilder.createOath(mechanismUid, issuer, accountName,
                    tokenType, algorithm, secret, digits, counter, period);
            defaultStorage.setAccount(account);
            defaultStorage.setMechanism(mechanism);
        }

        // Retrieve and verify accounts
        for (int i = 0; i < numberOfAccounts; i++) {
            String issuer = ISSUER + i;
            String accountName = ACCOUNT_NAME + i;
            // Verify account
            Account account = defaultStorage.getAccount(issuer + "-" + accountName);
            assertNotNull(account);
            // Verify mechanism
            List<Mechanism> mechanismList = defaultStorage.getMechanismsForAccount(account);
            OathMechanism mechanism = (OathMechanism) mechanismList.get(0);
            assertNotNull(mechanism);
            // Verify token code
            OathTokenCode tokenCode = mechanism.getOathTokenCode();
            assertNotNull(tokenCode);
        }

        Log.d(TAG, "Stored and retrieved " + numberOfAccounts +
                " accounts in " + (System.currentTimeMillis() - startTime)/1000 + " seconds");
    }

    private String getRandomSharedSecret() {
        int sharedSecretByteLength = Math.max(8, (int) Math.ceil(32 / 2d));
        byte[] sharedSecret = new byte[sharedSecretByteLength];
        rand.nextBytes(sharedSecret);
        return Base32String.encode(sharedSecret);
    }

    private int getRandomDigits() {
        return rand.nextBoolean() ? 6 : 8;
    }

    private int getRandomCounter() {
        return rand.nextInt(200);
    }

    private int getRandomPeriod() {
        return rand.nextBoolean() ? 30 : 60;
    }

    private String getRandomAlgorithm() {
        return ALGORITHMS[rand.nextInt(ALGORITHMS.length)];
    }

    private String getRandomMechanismUid() {
        return UUID.randomUUID().toString();
    }

    private OathMechanism.TokenType getRandomTokenType() {
        return rand.nextBoolean() ? OathMechanism.TokenType.HOTP : OathMechanism.TokenType.TOTP;
    }

}