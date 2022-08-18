/*
 * Copyright (c) 2020 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.view.activity;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.forgerock.android.auth.Account;
import org.forgerock.android.auth.FRAListener;
import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.exception.DuplicateMechanismException;
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
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
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
                        accountAdapter.notifyDataSetChanged();
                        setListVisibility();
                    }
                });
            }
        };
        authenticatorModel.addListener(listener);

        // Request notification permission for Android 13 and above
        if (Build.VERSION.SDK_INT >= 33) {
            final Activity activity = this;
            requestPermissionLauncher = registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                        @Override
                        public void onActivityResult(Boolean isGranted) {
                            if (isGranted) {
                                Toast.makeText(activity,
                                        "Notification permission granted. You now can " +
                                                "use Push accounts.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(activity,
                                        "Permission not granted. You can manually set " +
                                                "via Android Settings",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });

            requestNotificationPermission();
        }
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

    @RequiresApi(api = 33)
    private void requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED) {
            // Push notifications are enabled
        } else if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
            // User denied permission before, explain to the user why the app requires this permission
            new AlertDialog.Builder(this)
                    .setTitle("Notification Permission required")
                    .setMessage("Notification Permission is needed in order to receive Push Authentication requests on your device. " +
                            "If you deny, you will be unable to use Push accounts. To enable Push Notifications, click on 'Enable' and " +
                            "select 'Allow' in the next screen.")
                    .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create().show();
        } else {
            // User didn't take any action yet, ask for the permission
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        final Activity thisActivity = this;

        final Uri uri = intent.getData();
        if (uri != null) {
            AuthenticatorModel.getInstance().createMechanismFromUri(uri.toString(), new FRAListener<Mechanism>() {
                @Override
                public void onSuccess(final Mechanism mechanism) {
                    thisActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Mechanism successfully stored
                            Toast.makeText(thisActivity, String.format(getString(R.string.add_success),
                                    mechanism.getAccountName()), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                    AuthenticatorModel.getInstance().notifyDataChanged();
                }

                @Override
                public void onException(final Exception exception) {
                    thisActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Check if it's a duplication exception issue
                            if(exception instanceof DuplicateMechanismException)  {
                                final Mechanism duplicate = ((DuplicateMechanismException) exception).getCausingMechanism();
                                AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity);
                                builder.setTitle(R.string.duplicate_title_noreplace)
                                        .setMessage(R.string.duplicate_message_noreplace)
                                        .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                finish();
                                                return;
                                            }
                                        })
                                        .setCancelable(false)
                                        .show();
                            }
                            // Check for any other issue
                            else {
                                AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity);
                                String message = getString(R.string.add_error_qrcode_noretry);
                                message += getString(R.string.add_error_qrcode_detail, exception.getLocalizedMessage());
                                builder.setMessage(message)
                                        .setNeutralButton(R.string.ok,  new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int id) {
                                                finish();
                                            }
                                        })
                                        .setCancelable(false)
                                        .show();
                            }
                        }
                    });
                }
            });
        }
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
