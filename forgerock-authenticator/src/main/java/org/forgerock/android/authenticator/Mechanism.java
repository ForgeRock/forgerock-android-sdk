package org.forgerock.android.authenticator;

import androidx.annotation.VisibleForTesting;

import org.forgerock.android.auth.Logger;
import org.forgerock.android.authenticator.exception.InvalidNotificationException;
import org.forgerock.android.authenticator.exception.MechanismCreationException;
import org.forgerock.android.authenticator.util.SortedList;
import org.forgerock.android.authenticator.util.TimeKeeper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Mechanism model represents the two-factor way used for authentication.
 * Encapsulates the related settings, as well as an owning Account.
 */
public abstract class Mechanism extends ModelObject<Mechanism> {

    /** Unique identifier of the Mechanism */
    private String id;
    /** Uniquely identifiable UUID for current mechanism */
    private final String mechanismUID;
    /** Issuer of the account */
    private final String issuer;
    /** AccountName, or Username of the account for the issuer */
    private final String accountName;
    /** The type of the Mechanism */
    private final String type;
    /** The shared secret of the Mechanism */
    private final String secret;

    private static final String TAG = Mechanism.class.getSimpleName();

    public Mechanism(String mechanismUID, String issuer, String accountName, String type) {
        this.id = issuer + "-" + accountName + "-" + type;
        this.mechanismUID = mechanismUID;
        this.issuer = issuer;
        this.accountName = accountName;
        this.type = type;
        this.secret = null;
    }

    public Mechanism(String mechanismUID, String issuer, String accountName, String type,
                     String secret) {
        this.id = issuer + "-" + accountName + "-" + type;
        this.mechanismUID = mechanismUID;
        this.issuer = issuer;
        this.accountName = accountName;
        this.type = type;
        this.secret = secret;
    }

    /**
     * Get the mechanism unique Id that this notification was intended for.
     * @return The receiving Mechanism.
     */
    public String getMechanismUID() {
        return mechanismUID;
    }

    /**
     * Gets the name of the IDP that issued this identity.
     * @return The name of the IDP.
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Returns the name of this Identity.
     * @return The account name.
     */
    public String getAccountName() {
        return accountName;
    }

    /**
     * Get the string used to represent this type of mechanism.
     * @return The mechanism type as string.
     */
    public String getType() {
        return type;
    }

    /**
     * Get the string used to represent the shared secret of the mechanism.
     * @return The mechanism type as string.
     */
    public String getSecret() {
        return secret;
    }

    @Override
    public final boolean matches(Mechanism other) {
        if (other == null) {
            return false;
        }
        return other.issuer.equals(issuer) && other.accountName.equals(accountName) && other.type.equals(type);
    }

    @Override
    public int compareTo(Mechanism other) {
        return type.compareTo(other.type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Mechanism mechanism = (Mechanism) o;

        if (!mechanismUID.equals(mechanism.mechanismUID)) return false;
        if (!issuer.equals(mechanism.issuer)) return false;
        if (!accountName.equals(mechanism.accountName)) return false;
        return type.equals(mechanism.type);
    }

    @Override
    public int hashCode() {
        int result = mechanismUID.hashCode();
        result = 31 * result + issuer.hashCode();
        result = 31 * result + accountName.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }
}
