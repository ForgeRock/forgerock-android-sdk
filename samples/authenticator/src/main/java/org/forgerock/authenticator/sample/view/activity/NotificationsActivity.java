/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.view.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.PushNotification;
import org.forgerock.authenticator.sample.R;
import org.forgerock.authenticator.sample.controller.AuthenticatorModel;
import org.forgerock.authenticator.sample.controller.AuthenticatorModelListener;
import org.forgerock.authenticator.sample.view.adapter.NotificationAdapter;

/**
 * Page for viewing a list of Notifications relating to a mechanism.
 */
public class NotificationsActivity extends BaseNotificationActivity {

    private AuthenticatorModel authenticatorModel;
    private NotificationAdapter notificationAdapter;
    private AuthenticatorModelListener listener;
    private Mechanism mechanism;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authenticatorModel = AuthenticatorModel.getInstance(getApplicationContext());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.title_activity_notification);
            actionBar.setDisplayUseLogoEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setContentView(R.layout.activity_notifications);

        mechanism = getMechanism();
        if (mechanism == null) {
            finish();
            return;
        }

        notificationAdapter = new NotificationAdapter(this, mechanism);
        final GridView notificationsView = (GridView) findViewById(R.id.notification_list);
        notificationsView.setAdapter(notificationAdapter);

        // Enable delete account context menu
        notificationsView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                PushNotification notification = notificationAdapter.getItem(position);
                ContextualActionBar actionBar = new ContextualActionBar(getApplicationContext(), notification);
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
                        notificationAdapter.notifyDataSetChanged();
                    }
                });
            }
        };
        authenticatorModel.addListener(listener);

    }

    @Override
    public void onResume() {
        super.onResume();
        notificationAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        notificationAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        authenticatorModel.removeListener(listener);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    private void setListVisibility() {
        if (notificationAdapter.getCount() == 0) {
            findViewById(R.id.empty_notification_list).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.empty_notification_list).setVisibility(View.GONE);
        }
    }

    /**
     * Action Bar which is displayed when an Account is long pressed.
     */
    private class ContextualActionBar implements ActionMode.Callback {

        private final Context context;
        private final PushNotification notification;

        public ContextualActionBar(Context context, PushNotification notification) {
            this.context = context;
            this.notification = notification;
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
                boolean success = authenticatorModel.removeNotification(notification);
                if(success) {
                    Toast.makeText(context, getString(R.string.notification_remove_success), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, getString(R.string.notification_remove_error), Toast.LENGTH_SHORT).show();
                }
            }
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

    }

}
