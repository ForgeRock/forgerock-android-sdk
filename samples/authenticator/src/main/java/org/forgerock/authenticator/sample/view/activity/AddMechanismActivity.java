/*
 * Copyright (c) 2020 - 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.view.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.forgerock.android.auth.FRAListener;
import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.exception.DuplicateMechanismException;
import org.forgerock.authenticator.sample.controller.AuthenticatorModel;
import org.forgerock.authenticator.sample.R;
import org.forgerock.authenticator.sample.camera.CameraScanActivity;
import org.forgerock.authenticator.sample.controller.GooglePlayServicesUtil;

/**
 * Activity used for Add new mechanism by scanning QR codes. Provides feedback to the user when a QR code is scanned,
 * and if successful, creates the Mechanism that the QR code represents and create/update the Account associated.
 */
public class AddMechanismActivity extends AppCompatActivity {

    private static final int QRCODE_READER_ACTIVITY_REQUEST = 1208;
    private static final String TAG = AddMechanismActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_mechanism);

        // Load QRCode scanner
        launchCameraScanActivity();
    }

    /**
     * Starts the back camera and scan the QRCode.
     * Check if Google Play Services is enabled in order to choose the scan method.
     */
    private void launchCameraScanActivity() {
        try {
            if (GooglePlayServicesUtil.isAvailable(getApplicationContext())) {
                this.startDefaultScan();
                Log.i(TAG, "Using Google MLKit API to scan the QRCode.");
            } else {
                this.startAlternativeScan();
                Log.i(TAG, "Using zxing library to scan the QRCode.");
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Missing configuration for Google Play Services.", e);
        }
    }

    /**
     * This default scan method uses the Google MLKit API to scan the QRCode
     */
    private void startDefaultScan() {
        Intent launchIntent = new Intent(this, CameraScanActivity.class);
        startActivityForResult(launchIntent, QRCODE_READER_ACTIVITY_REQUEST);
    }

    /**
     * This alternative scan method uses zxing library to scan the QRCode
     */
    private void startAlternativeScan() {
        IntentIntegrator intentIntegrator = new IntentIntegrator(this);
        intentIntegrator.setPrompt("Scan a barcode or QR Code");
        intentIntegrator.setOrientationLocked(true);
        intentIntegrator.initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        } else if (resultCode != Activity.RESULT_OK) {
            Toast.makeText(this, R.string.error_scanning, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (requestCode == QRCODE_READER_ACTIVITY_REQUEST && data != null) {
            String scanResult = data.getStringExtra(CameraScanActivity.INTENT_EXTRA_QRCODE_VALUE);
            if(scanResult != null) {
                createMechanismFromScan(scanResult);
            } else {
                Toast.makeText(this, R.string.error_scanning, Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (intentResult != null) {
                if (intentResult.getContents() != null) {
                    createMechanismFromScan(intentResult.getContents());
                }
            }
        }
    }

    /**
     * Creates a second factor authenticator mechanism from the QRCode scanned
     */
    private void createMechanismFromScan(final String scanResult) {

        final Activity thisActivity = this;

        AuthenticatorModel.getInstance().createMechanismFromUri(scanResult, new FRAListener<Mechanism>() {
            @Override
            public void onSuccess(final Mechanism mechanism) {
                AddMechanismActivity.this.runOnUiThread(new Runnable() {
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
                AddMechanismActivity.this.runOnUiThread(new Runnable() {
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
                            String message = getString(R.string.add_error_qrcode);
                            message += getString(R.string.add_error_qrcode_detail, exception.getLocalizedMessage());
                            builder.setMessage(message)
                                    .setPositiveButton(R.string.try_again, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                            launchCameraScanActivity();
                                        }
                                    })
                                    .setNegativeButton(R.string.cancel,  new DialogInterface.OnClickListener() {
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
