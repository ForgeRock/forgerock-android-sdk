/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

import android.content.Context;

import org.forgerock.android.auth.Logger;
import org.forgerock.android.authenticator.exception.DuplicateMechanismException;
import org.forgerock.android.authenticator.exception.MechanismCreationException;
import org.forgerock.android.authenticator.exception.MechanismParsingException;
import org.forgerock.android.authenticator.util.MapUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Determines the type of mechanism which is being created, and routes the creation request to the
 * appropriate builder.
 */
abstract class MechanismFactory {

    private Context context;
    private StorageClient storageClient;

    private static final String TAG = MechanismFactory.class.getSimpleName();

    /**
     * Creates the MechanismFactory and loads the available mechanism information.
     */
    public MechanismFactory(Context context, StorageClient storageClient) {
        this.context = context;
        this.storageClient = storageClient;
    }

    /**
     * Method used to create the Mechanism represented by a given URI.
     * @param version The version extracted from the URI.
     * @param mechanismUID A generated mechanismUID
     * @param map The map of values generated from the original URI.
     * @return The Mechanism object
     * @throws MechanismCreationException If anything goes wrong.
     */
    protected abstract  Mechanism createFromUriParameters(int version, String mechanismUID, Map<String, String> map)
            throws MechanismCreationException;

    /**
     * Return the MechanismParser subclass used by the factory for a particular Mechanism type.
     * @return The MechanismParser.
     */
    protected abstract MechanismParser getParser();

    /**
     * Convert a URL to the Mechanism it represents, including extracting the account.
     * Also adds it to the model.
     * @param uri The URI to process.
     * @return The created Mechanism.
     * @throws MechanismParsingException If the URL was not parsed correctly.
     * @throws MechanismCreationException If the data was not valid to create a Mechanism.
     */
    public final Mechanism createFromUri(String uri) throws MechanismParsingException, MechanismCreationException {
        MechanismParser parser = getParser();
        Map<String, String> values = parser.map(uri);
        String mechanismType = MapUtil.get(values, MechanismParser.SCHEME, "");
        String issuer = MapUtil.get(values, MechanismParser.ISSUER, "");
        String accountName = MapUtil.get(values, MechanismParser.ACCOUNT_NAME, "");
        String imageURL = MapUtil.get(values, MechanismParser.IMAGE, null);
        String bgColor = MapUtil.get(values, MechanismParser.BG_COLOR, null);

        int version;
        try {
            version = Integer.parseInt(MapUtil.get(values, MechanismParser.VERSION, "1"));
        } catch (NumberFormatException e) {
            Logger.warn(TAG, e,"Expected valid integer, found: %s", values.get(MechanismParser.VERSION));
            throw new MechanismCreationException("Expected valid integer, found " +
                    values.get(MechanismParser.VERSION), e);
        }

        Account account = storageClient.getAccount(issuer + "-" + accountName);
        try {

            if (account == null) {
                account = Account.builder()
                        .setIssuer(issuer)
                        .setAccountName(accountName)
                        .setImageURL(imageURL)
                        .setBackgroundColor(bgColor)
                        .build();
                storageClient.setAccount(account);
            } else {
                final List<Mechanism> mechanisms = storageClient.getMechanismsForAccount(account);
                for (Mechanism mechanism : mechanisms) {
                    if (mechanism.getType().equals(mechanismType)) {
                        throw new DuplicateMechanismException("Matching mechanism already exists", mechanism);
                    }
                }
            }

            String mechanismUID = getNewMechanismUID();
            Mechanism newMechanism = createFromUriParameters(version, mechanismUID, values);
            if(storageClient.setMechanism(newMechanism)) {
                Logger.debug(TAG, "New mechanism with UID %s stored successfully.", mechanismUID);
                return newMechanism;
            } else {
                Logger.debug(TAG,"Error setting the mechanism (%s) to the Account.", mechanismUID);
                throw new MechanismCreationException("Error setting the mechanism to the Account.");
            }
        } catch (MechanismCreationException e) {
            if (storageClient.getMechanismsForAccount(account).isEmpty()) {
                Logger.warn(TAG, e,"Removing temporally created account.");
                storageClient.removeAccount(account);
            }
            throw e;
        }
    }

    /**
     * Generate a new, unique ID for a Mechanism.
     * @return The new mechanism UID.
     */
    private String getNewMechanismUID() {
        Logger.debug(TAG,"Creating new UID for the mechanism.");
        UUID uid = UUID.randomUUID();
        while (isExistingMechanismUID(uid.toString())) {
            uid = UUID.randomUUID();
        }
        return uid.toString();
    }

    /**
     * Check if unique ID for a Mechanism already exist
     * @return The new mechanism UID.
     */
    private boolean isExistingMechanismUID(String uid) {
        for (Mechanism mechanism : getAllMechanisms()) {
            if (mechanism.getMechanismUID().equals(uid)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get all mechanisms stored in the model.
     * @return The complete list of mechanisms.
     */
    private List<Mechanism> getAllMechanisms() {
        List<Mechanism> result = new ArrayList<>();
        for (Account account : storageClient.getAllAccounts()) {
            result.addAll(storageClient.getMechanismsForAccount(account));
        }
        return result;
    }

}
