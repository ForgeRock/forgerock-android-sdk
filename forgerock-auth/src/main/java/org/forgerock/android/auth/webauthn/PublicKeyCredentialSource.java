/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.webauthn;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;

import lombok.Builder;
import lombok.Getter;

import static com.google.android.gms.fido.common.Transport.INTERNAL;

/**
 * Representation of Public Key Credential Source that received after WebAuthn registration.
 */
@Getter
public class PublicKeyCredentialSource implements Parcelable {

    //Credential ID
    private final byte[] id;
    private String type;
    private final String rpid;
    private final byte[] userHandle;
    private final String otherUI;

    @Builder
    public PublicKeyCredentialSource(byte[] id, String type, String rpid, byte[] userHandle, String otherUI) {
        this.id = id;
        this.type = type;
        if (type == null) {
            this.type = PublicKeyCredentialType.PUBLIC_KEY.toString();
        }
        this.rpid = rpid;
        this.userHandle = userHandle;
        this.otherUI = otherUI;
    }

    protected PublicKeyCredentialSource(Parcel in) {
        id = in.createByteArray();
        type = in.readString();
        rpid = in.readString();
        userHandle = in.createByteArray();
        otherUI = in.readString();
    }

    public boolean equalsUserHandle(PublicKeyCredentialSource source) {
        return Arrays.equals(userHandle, source.getUserHandle());
    }

    public static final Creator<PublicKeyCredentialSource> CREATOR = new Creator<PublicKeyCredentialSource>() {
        @Override
        public PublicKeyCredentialSource createFromParcel(Parcel in) {
            return new PublicKeyCredentialSource(in);
        }

        @Override
        public PublicKeyCredentialSource[] newArray(int size) {
            return new PublicKeyCredentialSource[size];
        }
    };

    /**
     * Convert {@link PublicKeyCredentialSource} to {@link JSONObject}
     *
     * @return The result JSONObject
     */
    public JSONObject toJson() {
        JSONObject result = new JSONObject();
        try {
            result.put("id", Base64.encodeToString(id, Base64.DEFAULT));
            result.put("type", type);
            result.put("rpid", rpid);
            if (userHandle != null) {
                result.put("userHandle", Base64.encodeToString(userHandle, Base64.DEFAULT));
            }
            result.put("otherUI", otherUI);
            return result;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convert {@link PublicKeyCredentialSource} to {@link PublicKeyCredentialDescriptor}
     *
     * @return The result {@link PublicKeyCredentialDescriptor}
     */
    public PublicKeyCredentialDescriptor toDescriptor() {
        try {
            return new PublicKeyCredentialDescriptor(
                    PublicKeyCredentialType.fromString(type).toString(),
                    id,
                    Collections.singletonList(INTERNAL));
        } catch (PublicKeyCredentialType.UnsupportedPublicKeyCredTypeException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    /**
     * Convert {@link JSONObject} to {@link PublicKeyCredentialSource}
     *
     * @param jsonObject The json object to be converted
     * @return The result PublicKeyCredentialSource
     */
    public static PublicKeyCredentialSource fromJson(JSONObject jsonObject) {
        try {
            return PublicKeyCredentialSource.builder()
                    .id(Base64.decode(jsonObject.getString("id"), Base64.DEFAULT))
                    .type(jsonObject.optString("type", null))
                    .rpid(jsonObject.optString("rpid", null))
                    .userHandle(jsonObject.has("userHandle") ? Base64.decode(jsonObject.getString("userHandle"), Base64.DEFAULT) : null)
                    .otherUI(jsonObject.optString("otherUI", null))
                    .build();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(id);
        dest.writeString(type);
        dest.writeString(rpid);
        dest.writeByteArray(userHandle);
        dest.writeString(otherUI);
    }
}
