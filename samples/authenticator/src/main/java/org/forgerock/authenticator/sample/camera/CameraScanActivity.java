/*
 * Copyright (c) 2020 - 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.camera;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.Barcode;

import org.forgerock.authenticator.sample.R;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Camera Scanner Activity uses Camera X API to initialize the back camera and bind
 * the {@link QrCodeAnalyzer} to detect the QRCode.
 *
 * This is the default QRCode scanner method, which depends on Google Play Services APIs.
 */
public class CameraScanActivity extends AppCompatActivity {

    // Key value of the QRCode value.
    public static final String INTENT_EXTRA_QRCODE_VALUE = "qrcode_value";

    // Key value of the Camera permission
    private static final int REQUEST_CAMERA_PERMISSION = 10;

    private static final String TAG = CameraScanActivity.class.getSimpleName();

    private PreviewView defaultView;
    private ExecutorService cameraExecutor;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_camera_scan);

        defaultView = this.findViewById(R.id.default_view);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (isCameraPermissionGranted()) {
            defaultView.post(new Runnable() {
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
    private boolean isCameraPermissionGranted() {
        int selfPermission = ContextCompat.checkSelfPermission(this.getBaseContext(), Manifest.permission.CAMERA);
        return selfPermission == 0;
    }

    /**
     * Returns the result of requesting of the camera permission.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (this.isCameraPermissionGranted()) {
                defaultView.post(new Runnable() {
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
     * Starts the back camera and scan the QRCode.
     */
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        QrCodeAnalyzer qrCodeAnalyzer = new QrCodeAnalyzer(new QrCodeAnalyzer.AnalyzerCallback() {
            @Override
            public void onSuccess(List<Barcode> result) {
                if(!result.isEmpty()) {
                    onQRCodeDetected(result.get(0).getRawValue());
                }
            }

            @Override
            public void onException(Exception e) {
                Log.e(TAG, "something went wrong", e);
            }
        });

        cameraProviderFuture.addListener(
                () -> {
                    try {
                        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();
                        imageAnalysis.setAnalyzer(cameraExecutor, qrCodeAnalyzer);

                        bindPreview(cameraProvider, imageAnalysis);
                    } catch (ExecutionException | InterruptedException e) {
                        Log.e(TAG, "something went wrong", e);
                    }
                },
                ContextCompat.getMainExecutor(this)
        );
    }

    /**
     * Bind the BACK camera to the preview and sets ImageAnalysis.
     */
    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider, ImageAnalysis imageAnalysis) {
        Preview preview = new Preview.Builder()
                .build();
        preview.setSurfaceProvider(defaultView.getSurfaceProvider());

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
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
