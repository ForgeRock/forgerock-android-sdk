/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.forgerock.android.auth.webauthn.PublicKeyCredentialSource;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;

/**
 * A Repository that store {@link PublicKeyCredentialSource}
 */
@TargetApi(23)
public class WebAuthnDataRepository {

    private static String TAG = WebAuthnDataRepository.class.getSimpleName();
    private static final String ALLOW_CREDENTIALS = "ALLOW_CREDENTIALS";
    private static final String ORG_FORGEROCK_V_1_WEBAUTHN_KEYS = "org.forgerock.v1.WEBAUTHN_KEYS";
    private static final String ORG_FORGEROCK_V_1_WEBAUTHN = "org.forgerock.v1.WEBAUTHN";
    private DataRepository dataRepository;
    //Only keep track on the last 10 registered credentials
    private int maxCredentials = 10;

    @Builder
    WebAuthnDataRepository(@NonNull Context context,
                           @Nullable Encryptor encryptor,
                           @Nullable SharedPreferences sharedPreferences,
                           @Nullable Integer maxCredentials) {
        try {
            dataRepository = new AccountDataRepository(context,
                    context.getString(R.string.forgerock_webauthn_account_name),
                    encryptor, ORG_FORGEROCK_V_1_WEBAUTHN_KEYS);
        } catch (Exception e) {
            SharedPreferences sp = null;
            if (sharedPreferences == null) {
                sp = new SecuredSharedPreferences(context,
                        ORG_FORGEROCK_V_1_WEBAUTHN,
                        ORG_FORGEROCK_V_1_WEBAUTHN_KEYS, encryptor);
            } else {
                sp = sharedPreferences;
            }
            dataRepository = new SharedPreferenceDataRepository(context, sp);
        } catch (Error error) {
            //Something wrong cannot support Usernameless
            Logger.error(TAG, error, "UsernameLess cannot be supported." );
        }
        if (maxCredentials == null) {
            this.maxCredentials = context.getResources()
                    .getInteger(R.integer.forgerock_webauthn_max_credential);
        } else {
            this.maxCredentials = maxCredentials;
        }
    }

    /**
     * Persist the {@link PublicKeyCredentialSource}, it overrides the existing
     * {@link PublicKeyCredentialSource} with the same {@link PublicKeyCredentialSource#getUserHandle()}
     *
     * @param source The {@link PublicKeyCredentialSource} to persist.
     */
    public void persist(@NonNull PublicKeyCredentialSource source) {
        if (dataRepository == null) {
            Logger.warn(TAG, "UsernameLess cannot be supported. No credential will be stored" );
            return;
        }
        String storedCredentials = dataRepository.getString(ALLOW_CREDENTIALS);
        JSONArray result = new JSONArray();
        if (storedCredentials != null) {
            //filter the one with same user id.
            try {
                JSONArray stored = new JSONArray(storedCredentials);
                for (int i = 0; i < stored.length() && result.length() < maxCredentials - 1; i++) {
                    PublicKeyCredentialSource s = PublicKeyCredentialSource.fromJson(stored.getJSONObject(i));
                    if (!source.equalsUserHandle(s)) {
                        result.put(s.toJson());
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        result.put(source.toJson());
        dataRepository.save(ALLOW_CREDENTIALS, result.toString());
    }

    /**
     * Retrieve all {@link PublicKeyCredentialSource} with the same rpId {@link PublicKeyCredentialSource#getRpid()}
     *
     * @param rpId The Relying Party Id
     * @return All {@link PublicKeyCredentialSource} with the same rpId {@link PublicKeyCredentialSource#getRpid()}
     */
    public List<PublicKeyCredentialSource> getPublicKeyCredentialSource(String rpId) {
        List<PublicKeyCredentialSource> result = new ArrayList<>();
        if (dataRepository == null) {
            Logger.warn(TAG, "UsernameLess cannot be supported. No credential is stored" );
            return result;
        }
        String credentials = dataRepository.getString(ALLOW_CREDENTIALS);
        try {
            if (credentials != null) {
                JSONArray array = new JSONArray(credentials);
                for (int i = 0; i < array.length(); i++) {
                    PublicKeyCredentialSource source = PublicKeyCredentialSource.fromJson(array.getJSONObject(i));
                    if (rpId == null || rpId.equals(source.getRpid())) {
                        result.add(PublicKeyCredentialSource.fromJson(array.getJSONObject(i)));
                    }
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return result;

    }

    /**
     * Retrieve all stored {@link PublicKeyCredentialSource}
     *
     * @return All {@link PublicKeyCredentialSource}
     */
    public List<PublicKeyCredentialSource> getPublicKeyCredentialSource() {
        return getPublicKeyCredentialSource(null);
    }
}
