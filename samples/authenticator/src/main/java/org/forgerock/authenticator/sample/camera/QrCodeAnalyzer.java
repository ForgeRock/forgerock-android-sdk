/*
 * Copyright (c) 2020 - 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.camera;

import android.annotation.SuppressLint;
import android.media.Image;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The QrCodeAnalyzer class is used to detect the QR codes from camera input using ImageAnalysis use case.
 * It implements ImageAnalysis.Analyzer interface, which provides the methods analyze(ImageProxy image)
 * used for QR code detection.
 */
public class QrCodeAnalyzer implements ImageAnalysis.Analyzer {

    private final AnalyzerCallback onQrCodesDetected;
    private final BarcodeScanner scanner = BarcodeScanning.getClient();

    /**
     * QrCodeAnalyzer constructor which receives a callback to handle to receive the scan result
     * @param onQrCodesDetected callback to get notifications when QR code is detected
     */
    public QrCodeAnalyzer(@NotNull AnalyzerCallback onQrCodesDetected) {
        this.onQrCodesDetected = onQrCodesDetected;
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
            InputImage image = InputImage.fromMediaImage(mediaImage, rotationDegrees);
            scanner.process(image)
                    .addOnSuccessListener(barcodes -> onQrCodesDetected.onSuccess(barcodes))
                    .addOnFailureListener(e -> onQrCodesDetected.onException(e))
                    .addOnCompleteListener(task -> imageProxy.close());
        }
    }

    /**
     * Callback interface to get notifications when QR code is detected
     */
    public interface AnalyzerCallback {
        void onSuccess(List<Barcode> result);

        void onException(Exception e);
    }

}
