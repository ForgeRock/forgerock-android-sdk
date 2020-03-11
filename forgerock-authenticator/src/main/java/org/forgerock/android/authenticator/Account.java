package org.forgerock.android.authenticator;

import android.net.Uri;

import org.forgerock.android.authenticator.util.SortedList;

import java.util.List;

/**
 * Account model represents an identity for the user with an issuer. It is possible for a user to
 * have multiple accounts provided by a single issuer, however it is not possible to have multiple
 * accounts from with same issuer for the same account name.
 */
public class Account extends ModelObject<Account> {

    /** Unique identifier of Account */
    private final String id;
    /** Issuer of the account */
    private final String issuer;
    /** AccountName, or Username of the account for the issuer */
    private final String accountName;
    /** URL of Account's logo image */
    private final String imageURL;
    /** HEX Color code in String for Account */
    private final String backgroundColor;
    /** An array of Mechanism associated with current Account */
    private final List<Mechanism> mechanisms;

    private static final String TAG = Account.class.getSimpleName();

    /**
     * Creates Account object with given information
     * @param issuer String value of issuer
     * @param accountName String value of accountName or username
     * @param imageURL URL of account's logo image (optional)
     * @param backgroundColor String HEX code of account's background color (optional)
     */
    public Account(String issuer, String accountName, String imageURL, String backgroundColor) {
        this.id = issuer + "-" + accountName;
        this.issuer = issuer;
        this.accountName = accountName;
        this.imageURL = imageURL;
        this.backgroundColor = backgroundColor;
        this.mechanisms = new SortedList<>();
    }

    /**
     * Creates Account object with given information
     * @param issuer String value of issuer
     * @param accountName String value of accountName or username
     */
    public Account(String issuer, String accountName) {
        this.id = issuer + "-" + accountName;
        this.issuer = issuer;
        this.accountName = accountName;
        this.imageURL = null;
        this.backgroundColor = null;
        this.mechanisms = new SortedList<>();
    }

    /**
     * Gets the unique identifier for the Account.
     * @return The unique identifier.
     */
    public String getId() {
        return issuer;
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
     * Gets the image URL for the IDP that issued this identity.
     * @return String representing the path to the image, or null if not assigned.
     */
    public String getImageURL() {
        return imageURL;
    }

    /**
     * Gets the background color for the IDP that issued this identity.
     * @return A hex string including a prepending # symbol, representing the color (e.g. #aabbcc)
     */
    public String getBackgroundColor() {
        return backgroundColor;
    }

    @Override
    public boolean matches(Account other) {
        if (other == null) {
            return false;
        }
        return other.issuer.equals(issuer) && other.accountName.equals(accountName);
    }

    @Override
    public int compareTo(Account another) {
        if (another == null) {
            return -1;
        }
        int compareIssuer = issuer.compareTo(another.issuer);
        if (compareIssuer == 0) {
            return accountName.compareTo(another.accountName);
        }
        return compareIssuer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Account account = (Account) o;

        if (!issuer.equals(account.issuer)) return false;
        if (!accountName.equals(account.accountName)) return false;
        if (imageURL != null ? !imageURL.equals(account.imageURL) : account.imageURL != null)
            return false;
        return backgroundColor != null ? backgroundColor.equals(account.backgroundColor) : account.backgroundColor == null;
    }

    @Override
    public int hashCode() {
        int result = issuer.hashCode();
        result = 31 * result + accountName.hashCode();
        result = 31 * result + (imageURL != null ? imageURL.hashCode() : 0);
        result = 31 * result + (backgroundColor != null ? backgroundColor.hashCode() : 0);
        return result;
    }

}
