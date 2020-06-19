/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.view.layout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.forgerock.android.auth.Account;
import org.forgerock.android.auth.Mechanism;
import org.forgerock.authenticator.sample.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Individual entry which displays information about a given Account.
 */
public class AccountLayout extends RelativeLayout {

    //Constructors used automatically by Android. Will never be called directly.
    public AccountLayout(Context context) {
        super(context);
    }
    public AccountLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public AccountLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Set the Account that this Layout displays.
     * @param account The account to display.
     */
    public void bind(final Account account) {
        TextView issuerView = findViewById(R.id.issuer);
        TextView accountNameView = findViewById(R.id.accountName);
        ImageView logoView = findViewById(R.id.image);
        LinearLayout childElement;

        // Set Issuer and Account Name
        issuerView.setText(account.getIssuer());
        accountNameView.setText(account.getAccountName());

        // Load the logo
        Picasso.with(getContext())
                .load(account.getImageURL())
                .placeholder(R.drawable.forgerock_placeholder)
                .into(logoView);

        // There should currently only be at most two mechanisms associated with the Account
        List<MechanismIconLayout> icons = new ArrayList<>();
        icons.add((MechanismIconLayout) findViewById(R.id.iconA));
        icons.add((MechanismIconLayout) findViewById(R.id.iconB));

        for (int i = 0; i < icons.size(); i++) {
            icons.get(i).setVisibility(GONE);
        }

        for (int i = 0; i < account.getMechanisms().size() && i < icons.size(); i++) {
            icons.get(i).setVisibility(VISIBLE);
            icons.get(i).setMechanism(account.getMechanisms().get(i));
        }

        // For accounts with OTP mechanisms, display the codes under account
        childElement = findViewById(R.id.account_sub_item);
        for (final Mechanism mechanism : account.getMechanisms()) {
            if(mechanism.getType().equals(Mechanism.OATH)){
                childElement.setVisibility(View.VISIBLE);
                return;
            } else {
                childElement.setVisibility(View.GONE);
            }
        }
    }

}
