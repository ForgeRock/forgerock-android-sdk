/*
 * Copyright (c) 2019 - 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Logger;

/**
 * Simple Login Activity
 */
public class SimpleLoginActivity extends AppCompatActivity implements FRListener<Void> {

    private static final String TAG = SimpleLoginActivity.class.getSimpleName();
    public static final String EXCEPTION = "EXCEPTION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_login);
    }

    @Override
    public void onSuccess(Void result) {
        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onException(Exception e) {
        Logger.error(TAG, e, e.getMessage());
        Intent intent = new Intent();
        intent.putExtra(EXCEPTION, e);
        setResult(Activity.RESULT_CANCELED, intent);
        finish();
    }
}
