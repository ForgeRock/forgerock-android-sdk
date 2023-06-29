/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.view.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * An example full-screen launcher activity
 */
public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use this activity to display animations, initialize some stuff and then proceed to
        // the Accounts activity

        // Uncomment the next lines to enable a sample/test HTTP request interceptor
        // Logger.set(Logger.Level.DEBUG);
        // RequestInterceptorRegistry.getInstance().register(new TestPushRequestInterceptor());

        proceed();
    }

    private void proceed() {
        Intent intent = new Intent(this, AccountsActivity.class);
        startActivity(intent);
        finish();
    }
}