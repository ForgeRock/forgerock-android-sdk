/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.camera;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;

import org.forgerock.authenticator.sample.R;

import java.util.List;

public class CameraScanActivity extends AppCompatActivity {

    private TextureView textureView;

    // Key value of the QRCode value.
    public static final String INTENT_EXTRA_QRCODE_VALUE = "qrcode_value";

    // Key value of the Camera permission
    private static final int REQUEST_CAMERA_PERMISSION = 10;

    private static final String TAG = CameraScanActivity.class.getSimpleName();

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_camera_scan);

        textureView = this.findViewById(R.id.texture_view);

        if (isCameraPermissionGranted()) {
            textureView.post(new Runnable() {
                public final void run() {
                    CameraScanActivity.this.startCamera();
                }
            });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }

    }

    /**
     * Handles the requesting of the camera permission.  This includes showing a dialog message of
     * why the permission is needed then sending the request.
     */
    private final boolean isCameraPermissionGranted() {
        int selfPermission = ContextCompat.checkSelfPermission(this.getBaseContext(), Manifest.permission.CAMERA);
        return selfPermission == 0;
    }

    /**
     * Returns the result of requesting of the camera permission.
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (this.isCameraPermissionGranted()) {
                textureView.post(new Runnable() {
                    public final void run() {
                        CameraScanActivity.this.startCamera();
                    }
                });
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }

    /**
     * Starts the back camera source and sets QrCodeAnalyzer to handle the QRCode capture
     */
    private final void startCamera() {
        PreviewConfig previewConfig = (new PreviewConfig.Builder()).setLensFacing(CameraX.LensFacing.BACK).build();
        Preview preview = new Preview(previewConfig);

        preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
            public final void onUpdated(Preview.PreviewOutput previewOutput) {
                ViewGroup parent = (ViewGroup) textureView.getParent();
                parent.removeView(textureView);
                textureView.setSurfaceTexture(previewOutput.getSurfaceTexture());
                parent.addView(textureView, 0);
            }
        });

        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder().build();
        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);

        QrCodeAnalyzer qrCodeAnalyzer = new QrCodeAnalyzer(new QrCodeAnalyzer.AnalyzerCallback() {
            @Override
            public void onSuccess(List<FirebaseVisionBarcode> result) {
                if(!result.isEmpty()) {
                    onQRCodeDetected(result.get(0).getRawValue());
                }
            }

            @Override
            public void onException(Exception e) {
                Log.e(TAG, "something went wrong", e);
            }
        });
        imageAnalysis.setAnalyzer(qrCodeAnalyzer);
        CameraX.bindToLifecycle(this, preview, imageAnalysis);
    }

    /**
     * Returns the detected qrcode value to the caller activity.
     */
    private void onQRCodeDetected(String qrcode) {
        Log.d(TAG, "Detected QRCode with value: " + qrcode);

        Intent returnIntent = new Intent();
        returnIntent.putExtra(INTENT_EXTRA_QRCODE_VALUE, qrcode);
        setResult(RESULT_OK, returnIntent);
        finish();
    }

}
