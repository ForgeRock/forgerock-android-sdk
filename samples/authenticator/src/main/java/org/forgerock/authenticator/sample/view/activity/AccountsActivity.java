/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.view.activity;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import androidx.appcompat.app.AppCompatActivity;

import org.forgerock.android.auth.Account;
import org.forgerock.authenticator.sample.R;
import org.forgerock.authenticator.sample.controller.AuthenticatorModel;
import org.forgerock.authenticator.sample.controller.AuthenticatorModelListener;
import org.forgerock.authenticator.sample.view.adapter.AccountAdapter;

/**
 * Page for viewing a list of all Accounts. The start page for the app.
 */
public class AccountsActivity extends AppCompatActivity {

    private AuthenticatorModel authenticatorModel;
    private AccountAdapter accountAdapter;
    private AuthenticatorModelListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authenticatorModel = AuthenticatorModel.getInstance(getApplicationContext());

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.account_title);
            actionBar.setDisplayUseLogoEnabled(false);
        }

        onNewIntent(getIntent());
        setContentView(R.layout.activity_account);

        accountAdapter = new AccountAdapter(this);
        final GridView accountsView = findViewById(R.id.account_list);
        accountsView.setAdapter(accountAdapter);

        // Enable delete account context menu
        accountsView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Account account = accountAdapter.getItem(position);
                ContextualActionBar actionBar = new ContextualActionBar(getApplicationContext(), account);
                startActionMode(actionBar);
                return true;
            }
        });

        setListVisibility();
        listener = new AuthenticatorModelListener() {
            @Override
            public void dataChanged() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setListVisibility();
                        accountAdapter.notifyDataSetChanged();
                    }
                });
            }
        };
        authenticatorModel.addListener(listener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        accountAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        accountAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        authenticatorModel.removeListener(listener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scan:
                startActivity(new Intent(this, AddMechanismActivity.class));
                return true;
            default:
                super.onOptionsItemSelected(item);
        }
        return false;
    }

    private void setListVisibility() {
        if (accountAdapter.getCount() == 0) {
            findViewById(R.id.empty_account_list).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.empty_account_list).setVisibility(View.GONE);
        }
    }

    /**
     * Action Bar which is displayed when an Account is long pressed.
     */
    private class ContextualActionBar implements ActionMode.Callback {

        private final Context context;
        private final Account account;

        public ContextualActionBar(Context context, Account account) {
            this.context = context;
            this.account = account;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.account, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.action_delete) {
                Intent intent = new Intent(context, DeleteAccountActivity.class);
                intent.putExtra(DeleteAccountActivity.ACCOUNT_REFERENCE, account.getId());
                startActivity(intent);
            }
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

    }

}
