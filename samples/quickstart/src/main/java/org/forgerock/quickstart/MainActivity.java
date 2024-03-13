/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.quickstart;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.forgerock.android.auth.FRAuth;
import org.forgerock.android.auth.FRUser;
import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;

public class MainActivity extends AppCompatActivity implements NodeListener<FRUser> {

    private static String TAG = MainActivity.class.getName();

    private TextView status;
    private Button loginButton;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Logger.set(Logger.Level.DEBUG);
        FRAuth.start(this);
        status = findViewById(R.id.status);
        loginButton = findViewById(R.id.login);
        logoutButton = findViewById(R.id.logout);
        updateStatus();

        loginButton.setOnClickListener(view -> FRUser.login(getApplicationContext(), this));
        logoutButton.setOnClickListener(view -> {
            FRUser.getCurrentUser().logout();
            updateStatus();
        });
    }

    private void updateStatus() {
        runOnUiThread(() -> {
            if (FRUser.getCurrentUser() == null) {
                status.setText("User is not authenticated");
                loginButton.setEnabled(true);
                logoutButton.setEnabled(false);
            } else {
                status.setText("User is authenticated");
                loginButton.setEnabled(false);
                logoutButton.setEnabled(true);
            }
        });
   }

    @Override
    public void onSuccess(FRUser result) {
        updateStatus();
    }

    @Override
    public void onException(Exception e) {
        Logger.error(TAG, e.getMessage(), e);
    }

    @Override
    public void onCallbackReceived(Node node) {
        NodeDialogFragment fragment = NodeDialogFragment.newInstance(node);
        fragment.show(getSupportFragmentManager(), NodeDialogFragment.class.getName());
    }
}