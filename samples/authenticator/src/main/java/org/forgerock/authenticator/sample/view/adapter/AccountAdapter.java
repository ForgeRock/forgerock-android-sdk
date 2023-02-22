/*
 * Copyright (c) 2020 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.forgerock.android.auth.Account;
import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.OathMechanism;
import org.forgerock.authenticator.sample.controller.AuthenticatorModel;
import org.forgerock.authenticator.sample.R;
import org.forgerock.authenticator.sample.view.layout.AccountDetailLayout;
import org.forgerock.authenticator.sample.view.layout.AccountLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for linking the complete list of Accounts with a series of layouts which display each one.
 */
public class AccountAdapter extends BaseAdapter {
    private final AuthenticatorModel authenticatorModel;
    private final LayoutInflater layoutInflater;
    private List<Account> accountList;

    /**
     * Creates the adapter, and finds the data model.
     */
    public AccountAdapter(Context context) {
        authenticatorModel =  AuthenticatorModel.getInstance(context.getApplicationContext());
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        accountList = authenticatorModel.getAllAccounts();
    }

    @Override
    public int getCount() {
        return accountList.size();
    }

    @Override
    public Account getItem(int position) {
        return accountList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.layout_account, parent, false);
        }

        // Bind account object to the view
        Account account = getItem(position);
        ((AccountLayout) convertView).bind(account);

        // If account contains an OTP mechanism, bind Oath mechanism to the view to display the
        // codes under the account
        View view = convertView.findViewById(R.id.account_detail);
        AccountDetailLayout accountDetailLayout = (AccountDetailLayout) view;
        for (Mechanism mechanism : account.getMechanisms()) {
            if (mechanism.getType().equals(Mechanism.OATH)) {
                accountDetailLayout.bind((OathMechanism)mechanism, account);
            }
        }

        return convertView;
    }

    @Override
    public void notifyDataSetChanged() {
        accountList = authenticatorModel.getAllAccounts();
        super.notifyDataSetChanged();
    }

}
