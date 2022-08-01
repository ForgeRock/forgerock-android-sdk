/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;
import static androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.OperationCanceledException;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.InitProvider;
import org.forgerock.android.auth.Listener;
import org.forgerock.android.auth.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class DeviceSigningVerifierCallback extends AbstractCallback implements KeyAware {

    private String userId;
    private String challenge;
    private String title;
    private Integer timeout;
    private String subtitle;
    private String description;

    @Keep
    public DeviceSigningVerifierCallback(JSONObject jsonObject, int index) {
        super(jsonObject, index);
    }

    @Override
    protected void setAttribute(String name, Object value) {
        switch (name) {
            case "userId":
                this.userId = (String) value;
                break;
            case "challenge":
                this.challenge = (String) value;
                break;
            case "title":
                this.title = (String) value;
                break;
            case "subtitle":
                this.subtitle = (String) value;
                break;
            case "description":
                this.description = (String) value;
                break;
            case "timeout":
                this.timeout = (Integer) value;
                break;

            default:
                //ignore
        }
    }

    public String getUserId() {
       if (StringUtils.isEmpty(userId)) {
            //Get the first one for now.
           SharedPreferences sharedPreferences = InitProvider.getCurrentActivity()
                   .getSharedPreferences(DEVICE_BINDING, Context.MODE_PRIVATE);
           if (sharedPreferences.getAll().keySet().isEmpty()) {
                throw new IllegalStateException("User Not found");
            } else {
                try {
                    return new JSONObject((String)sharedPreferences.getAll().entrySet().iterator().next().getValue()).getString("userId");
                } catch (JSONException e) {
                    throw new IllegalStateException(e);
                }
            }
        } else {
            return userId;
        }
    }

    public void setJws(String value) {
        super.setValue(value, 0);
    }

    public void setClientError(String value) {
        super.setValue(value, 1);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void sign(Context context, FRListener<Void> listener) {

        //Check if the device support it.
        if (!isSupported()) {
            setClientError("unsupported");
            Listener.onException(listener, new UnsupportedOperationException());
        }

        FragmentActivity fragmentActivity = InitProvider.getCurrentActivityAsFragmentActivity();

        String keyAlias = null;
        try {
            keyAlias = getKeyAlias();
        } catch (IllegalStateException e) {
            setClientError("keyNotFound");
            Listener.onException(listener, new IllegalStateException("Key not found") );
            return;
        }
        if (getKey(keyAlias) == null)  {
            setClientError("keyNotFound");
            Listener.onException(listener, new IllegalStateException("Key not found") );
            return;
        }

        String finalKeyAlias = keyAlias;
        BiometricPrompt biometricPrompt = new BiometricPrompt(fragmentActivity, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                setClientError("unsupported");
                Listener.onException(listener, new OperationCanceledException(errString.toString()));
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                try {
                    SignedJWT signedJWT = new SignedJWT(
                            new JWSHeader.Builder(JWSAlgorithm.RS512)
                                    .keyID(getBindingInfo(context).getString("kid"))
                                    .build()
                            , new JWTClaimsSet.Builder()
                            .subject(getUserId())
                            .claim("challenge", challenge)
                            .build());

                    signedJWT.sign(new RSASSASigner(getPrivateKey(finalKeyAlias)));
                    setJws(signedJWT.serialize());
                    Listener.onSuccess(listener, null);
                } catch (Exception e) {
                    Listener.onException(listener, e);
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                setClientError("unsupported");
                Listener.onException(listener, new OperationCanceledException());
            }

        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getTitle())
                .setSubtitle(getSubtitle())
                .setConfirmationRequired(true)
                .setDescription(getDescription())
                //.setNegativeButtonText("Cancel")
                .setConfirmationRequired(true)
                .setAllowedAuthenticators(BIOMETRIC_STRONG | DEVICE_CREDENTIAL).build();
                //.setAllowedAuthenticators(BIOMETRIC_STRONG).build();

        biometricPrompt.authenticate(promptInfo);

    }

    protected PrivateKey getPrivateKey(String keyAlias) throws GeneralSecurityException, IOException {
        KeyStore keyStore = getKeyStore();
        return (PrivateKey) keyStore.getKey(keyAlias, null);
    }

    protected boolean isSupported() {
        return true;
    }

    @Override
    public String getType() {
        return DeviceSigningVerifierCallback.class.getSimpleName();
    }

}
