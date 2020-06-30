/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.view.activity;


import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.forgerock.android.auth.FRAListener;
import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.PushNotification;
import org.forgerock.authenticator.sample.R;

public class PushNotificationActivity extends BaseNotificationActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_notification);

        final PushNotification notification = getNotification();
        Mechanism mechanism = getMechanism();

        if (notification == null || !notification.isPending()) {
            finish();
            return;
        }

        final Activity thisActivity = this;

        TextView questionView = findViewById(R.id.question);
        ((TextView) findViewById(R.id.issuer)).setText(mechanism.getIssuer());
        ((TextView) findViewById(R.id.accountName)).setText(mechanism.getAccountName());
        questionView.setText(String.format(getString(R.string.pushauth_login_title), mechanism.getIssuer()));

        Button approveButton = findViewById(R.id.pushApprove);
        approveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notification.accept(new FRAListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        PushNotificationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(thisActivity, String.format(getString(R.string.push_notification_approval), "approved"), Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onException(Exception e) {
                        PushNotificationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(thisActivity, R.string.notification_error_network_failure_message, Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });
                    }
                });
            }
        });

        Button denyButton = findViewById(R.id.pushDeny);
        denyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notification.deny(new FRAListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        PushNotificationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(thisActivity, String.format(getString(R.string.push_notification_approval), "denied"), Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onException(Exception e) {
                        PushNotificationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(thisActivity, R.string.notification_error_network_failure_message, Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });
                    }
                });
            }
        });

    }

}
