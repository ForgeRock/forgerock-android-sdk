package org.forgerock.android.authenticator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class OathTest {

    private final String MECHANISM_UID = "b162b325-ebb1-48e0-8ab7-b38cf341da95";
    private final String OTHER_MECHANISM_UID = "013be51a-8c14-356d-b0fc-b3660cc8a101";
    private final String ISSUER = "test.issuer";
    private final String OTHER_ISSUER = "test.issuer2";
    private final String ACCOUNT_NAME = "test.user";
    private final String OTHER_ACCOUNT_NAME = "test.user2";
    private final String MECHANISM_TYPE = "OPT";
    private final String OTHER_MECHANISM_TYPE = "PUSH";
    private final String SECRET = "JMEZ2W7D462P3JYBDG2HV7PFBM";
    private final String ALGORITHM = "SHA 256";
    private final int DIGITS = 6;
    private final int PERIOD = 30;
    private final int COUNTER = 0;

    @Test
    public void createOathMechanismSuccessfuly() {
        Oath mechanism = new Oath(MECHANISM_UID, ISSUER, ACCOUNT_NAME, MECHANISM_TYPE,
                Oath.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);

        assertEquals(mechanism.getMechanismUID(), MECHANISM_UID);
        assertEquals(mechanism.getIssuer(), ISSUER);
        assertEquals(mechanism.getAccountName(), ACCOUNT_NAME);
        assertEquals(mechanism.getType(), MECHANISM_TYPE);
        assertEquals(mechanism.getOathType(), Oath.TokenType.HOTP);
        assertEquals(mechanism.getAlgorithm(), ALGORITHM);
        assertEquals(mechanism.getSecret(), SECRET);
        assertEquals(mechanism.getDigits(), DIGITS);
        assertEquals(mechanism.getCounter(), COUNTER);
        assertEquals(mechanism.getPeriod(), PERIOD);
    }

    @Test
    public void shouldEqualEquivalentOathMechanism() {
        Mechanism mechanism1 = new Oath(MECHANISM_UID, ISSUER, ACCOUNT_NAME, MECHANISM_TYPE,
                Oath.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);
        Mechanism mechanism2
                = new Oath(MECHANISM_UID, ISSUER, ACCOUNT_NAME, MECHANISM_TYPE,
                Oath.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);

        assertEquals(mechanism1, mechanism2);
        assertEquals(mechanism1.compareTo(mechanism2), 0);
        assertEquals(mechanism2.compareTo(mechanism1), 0);
        assertEquals(mechanism1.hashCode(), mechanism2.hashCode());
    }

    @Test
    public void shouldNotEqualDifferentOathMechanismWithType() {
        Mechanism mechanism1 = new Oath(MECHANISM_UID, ISSUER, ACCOUNT_NAME, MECHANISM_TYPE,
                Oath.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);
        Mechanism mechanism2
                = new Oath(MECHANISM_UID, ISSUER, ACCOUNT_NAME, OTHER_MECHANISM_TYPE,
                Oath.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);

        assertFalse(mechanism1.equals(mechanism2));
        assertEquals(mechanism1.compareTo(mechanism2), -1);
        assertEquals(mechanism2.compareTo(mechanism1), 1);
    }

    @Test
    public void shouldNotEqualDifferentOathMechanismWithAccountName() {
        Mechanism mechanism1 = new Oath(MECHANISM_UID, ISSUER, ACCOUNT_NAME, MECHANISM_TYPE,
                Oath.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);
        Mechanism mechanism2
                = new Oath(MECHANISM_UID, ISSUER, OTHER_ACCOUNT_NAME, MECHANISM_TYPE,
                Oath.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);

        assertFalse(mechanism1.equals(mechanism2));
        assertEquals(mechanism1.compareTo(mechanism2), 0);
        assertEquals(mechanism2.compareTo(mechanism1), 0);
    }

    @Test
    public void shouldNotEqualDifferentOathMechanismWithAccountIssuer() {
        Mechanism mechanism1 = new Oath(MECHANISM_UID, ISSUER, ACCOUNT_NAME, MECHANISM_TYPE,
                Oath.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);
        Mechanism mechanism2
                = new Oath(MECHANISM_UID, OTHER_ISSUER, ACCOUNT_NAME, MECHANISM_TYPE,
                Oath.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);

        assertFalse(mechanism1.equals(mechanism2));
        assertEquals(mechanism1.compareTo(mechanism2), 0);
        assertEquals(mechanism2.compareTo(mechanism1), 0);
    }

}