package org.forgerock.android.authenticator;

import android.net.Uri;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class PushTest {

    private final String MECHANISM_UID = "b162b325-ebb1-48e0-8ab7-b38cf341da95";
    private final String OTHER_MECHANISM_UID = "013be51a-8c14-356d-b0fc-b3660cc8a101";
    private final String ISSUER = "test.issuer";
    private final String OTHER_ISSUER = "test.issuer2";
    private final String ACCOUNT_NAME = "test.user";
    private final String OTHER_ACCOUNT_NAME = "test.user2";
    private final String MECHANISM_TYPE = "PUSH";
    private final String OTHER_MECHANISM_TYPE = "OTP";
    private final String REGISTRATION_ENDPOINT = "http://openam.forgerock.com:8080/openam/json/push/sns/message?_action=register";
    private final String AUTHENTICATION_ENDPOINT = "http://openam.forgerock.com:8080/openam/json/push/sns/message?_action=authenticate";

    @Test
    public void createPushMechanismSuccessfuly() {
        Push mechanism = new Push(MECHANISM_UID, ISSUER, ACCOUNT_NAME, MECHANISM_TYPE,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);

        assertEquals(mechanism.getMechanismUID(), MECHANISM_UID);
        assertEquals(mechanism.getIssuer(), ISSUER);
        assertEquals(mechanism.getAccountName(), ACCOUNT_NAME);
        assertEquals(mechanism.getType(), MECHANISM_TYPE);
        assertEquals(mechanism.getRegistrationEndpoint(), REGISTRATION_ENDPOINT);
        assertEquals(mechanism.getAuthenticationEndpoint(), AUTHENTICATION_ENDPOINT);
    }

    @Test
    public void shouldEqualEquivalentPushMechanism() {
        Mechanism mechanism1 = new Push(MECHANISM_UID, ISSUER, ACCOUNT_NAME, MECHANISM_TYPE,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        Mechanism mechanism2
                = new Push(MECHANISM_UID, ISSUER, ACCOUNT_NAME, MECHANISM_TYPE,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);

        assertEquals(mechanism1, mechanism2);
        assertEquals(mechanism1.compareTo(mechanism2), 0);
        assertEquals(mechanism2.compareTo(mechanism1), 0);
        assertEquals(mechanism1.hashCode(), mechanism2.hashCode());
    }

    @Test
    public void shouldNotEqualDifferentPushMechanismWithType() {
        Mechanism mechanism1 = new Push(MECHANISM_UID, ISSUER, ACCOUNT_NAME, MECHANISM_TYPE,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        Mechanism mechanism2
                = new Push(MECHANISM_UID, ISSUER, ACCOUNT_NAME, OTHER_MECHANISM_TYPE,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);

        assertFalse(mechanism1.equals(mechanism2));
        assertEquals(mechanism1.compareTo(mechanism2), 1);
        assertEquals(mechanism2.compareTo(mechanism1), -1);
    }

    @Test
    public void shouldNotEqualDifferentPushMechanismWithAccountName() {
        Mechanism mechanism1 = new Push(MECHANISM_UID, ISSUER, ACCOUNT_NAME, MECHANISM_TYPE,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        Mechanism mechanism2
                = new Push(MECHANISM_UID, ISSUER, OTHER_ACCOUNT_NAME, MECHANISM_TYPE,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);

        assertFalse(mechanism1.equals(mechanism2));
        assertEquals(mechanism1.compareTo(mechanism2), 0);
        assertEquals(mechanism2.compareTo(mechanism1), 0);
    }

    @Test
    public void shouldNotEqualDifferentPushMechanismWithAccountIssuer() {
        Mechanism mechanism1 = new Push(MECHANISM_UID, ISSUER, ACCOUNT_NAME, MECHANISM_TYPE,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        Mechanism mechanism2
                = new Push(MECHANISM_UID, OTHER_ISSUER, ACCOUNT_NAME, MECHANISM_TYPE,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);

        assertFalse(mechanism1.equals(mechanism2));
        assertEquals(mechanism1.compareTo(mechanism2), 0);
        assertEquals(mechanism2.compareTo(mechanism1), 0);
    }

}