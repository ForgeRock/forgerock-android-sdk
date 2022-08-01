/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;

import android.content.Context;
import android.os.Build;
import android.os.OperationCanceledException;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.forgerock.android.auth.DeviceIdentifier;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.InitProvider;
import org.forgerock.android.auth.Listener;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class DeviceBindingCallback extends AbstractCallback implements KeyAware {

    private String userId;
    private String challenge;
    private String authenticationType;
    private String title;
    private String subtitle;
    private String description;

    @Keep
    public DeviceBindingCallback(JSONObject jsonObject, int index) {
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
            case "authenticationType":
                this.authenticationType = (String) value;
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
            default:
                //ignore
        }
    }

    public void setJws(String value) {
        super.setValue(value, 0);
    }

    public void setDeviceName(String value) {
        super.setValue(value, 1);
    }

    public void setDeviceId(String value) {
        super.setValue(value, 2);
    }

    public void setClientError(String value) {
        super.setValue(value, 3);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void bind(Context context, FRListener<Void> listener) {

        //Check if the device support it.
        if (!isSupported()) {
            setClientError("unsupported");
            Listener.onException(listener, new UnsupportedOperationException());
        }

        FragmentActivity fragmentActivity = InitProvider.getCurrentActivityAsFragmentActivity();

        String keyAlias;

        try {
            keyAlias = generateKeys(context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


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
                    String kid = getBindingInfo(context).getString("kid");
                    String jws = sign(keyAlias, challenge, kid);
                    setJws(jws);
                    setDeviceName("Andy Device");
                    setDeviceId(DeviceIdentifier.builder().context(context).build().getIdentifier());
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
                .setNegativeButtonText("Cancel")
                .setConfirmationRequired(true)
                //.setAllowedAuthenticators(BIOMETRIC_STRONG | DEVICE_CREDENTIAL).build();
                .setAllowedAuthenticators(BIOMETRIC_STRONG).build();

        biometricPrompt.authenticate(promptInfo);

    }

    protected boolean isSupported() {
        return true;
    }

    @Override
    public String getType() {
        return DeviceBindingCallback.class.getSimpleName();
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private String sign(String keyAlias, String challenge, String kid) throws GeneralSecurityException, IOException, JSONException, JOSEException {

        KeyStore keyStore = getKeyStore();
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, null);

        JWK jwk = new RSAKey.Builder((RSAPublicKey) getPublicKey(keyAlias))
                .keyUse(KeyUse.SIGNATURE)
                .keyID(kid)
                .algorithm(JWSAlgorithm.RS512)
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS512)
                        .keyID(kid)
                        .jwk(jwk)
                        .build()
                , new JWTClaimsSet.Builder()
                .subject(getUserId())
                .claim("challenge", challenge)
                .build());

        signedJWT.sign(new RSASSASigner(privateKey));
        return signedJWT.serialize();

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private PublicKey getPublicKey(String keyAlias) throws GeneralSecurityException, IOException {
        KeyStore keyStore = getKeyStore();
        return keyStore.getCertificate(keyAlias).getPublicKey();
    }

}
