/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.view.activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.forgerock.android.auth.Account;
import org.forgerock.authenticator.sample.controller.AuthenticatorModel;
import org.forgerock.authenticator.sample.R;

/**
 * Activity responsible for verifying that a user wishes to delete an account, then deleting it.
 */
public class DeleteAccountActivity extends Activity {

    /** The key to use to put the Account reference into the Intent. */
    public static final String ACCOUNT_REFERENCE = "identityReference";

    private static final String TAG = DeleteAccountActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_delete);

        String accountReference = getIntent().getStringExtra(ACCOUNT_REFERENCE);
        final Account account = AuthenticatorModel.getInstance().getAccount(accountReference);

        if (account == null) {
            Log.d(TAG, "Failed to find Account to delete");
            finish();
            return;
        }
        ((TextView) findViewById(R.id.issuer)).setText(account.getIssuer());
        ((TextView) findViewById(R.id.accountName)).setText(account.getAccountName());
        ((TextView) findViewById(R.id.confirmation_message)).setText(String.format(getString(R.string.delete_confirmation_message), getString(R.string.delete_type_account)));
        Picasso.with(this)
                .load(account.getImageURL())
                .placeholder(R.drawable.forgerock_placeholder)
                .into((ImageView) findViewById(R.id.image));

        findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AuthenticatorModel.getInstance().removeAccount(account);
                finish();
            }
        });
    }
}
