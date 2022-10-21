package org.forgerock.android.auth.callback;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.json.JSONObject;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.util.Calendar;
import java.util.Date;

public class CustomDeviceSigningVerifierCallback extends DeviceSigningVerifierCallback {
    public CustomDeviceSigningVerifierCallback() {
        super();
    }

    public CustomDeviceSigningVerifierCallback(JSONObject json, int index) {
        super(json, index);
    }

    private int expSeconds = 0;
    public void setExpSeconds(int expSeconds) {
        this.expSeconds = expSeconds;
    }

    @Override
    protected Date getExpiration() {
        Calendar date = Calendar.getInstance();
        date.add(Calendar.SECOND, this.expSeconds);
        return date.getTime();
    }

    public String getSignedJwt(String kid, String sub, String challenge)
    {
        //Generate RSA key
        KeyPairGenerator kpg = null;
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        kpg.initialize(2048);
        KeyPair rsaKey = kpg.generateKeyPair();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS512)
                .type(JOSEObjectType.JWT)
                .keyID(kid)
                .build();

        JWTClaimsSet payload = new JWTClaimsSet.Builder()
                .subject(sub)
                .claim("challenge", challenge)
                .expirationTime(getExpiration())
                .build();

        SignedJWT signedJWT = new SignedJWT(header, payload);
        try {
            signedJWT.sign(new RSASSASigner((RSAPrivateKey) rsaKey.getPrivate()));

        } catch (JOSEException e) {
            e.printStackTrace();
        }

        return signedJWT.serialize();
    }
}
