/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui;

import android.app.Activity;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import org.forgerock.android.auth.FRListener;

/**
 * Simple Register Activity
 */
public class SimpleRegisterActivity extends AppCompatActivity implements FRListener<Void> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_register);
    }

    @Override
    public void onSuccess(Void result) {
        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onException(Exception e) {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }
}
