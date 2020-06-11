/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.camera;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The QrCodeAnalyzer class is used to detect the QR codes from camera input using ImageAnalysis use case.
 * It implements ImageAnalysis.Analyzer interface, which provides the methods analyze(ImageProxy image,
 * int rotationDegrees) used for QR code detection.
 */
public class QrCodeAnalyzer implements ImageAnalysis.Analyzer {

    private AnalyzerCallback onQrCodesDetected;

    /**
     * QrCodeAnalyzer constructor which receives a callback to handle to receive the scan result
     * @param onQrCodesDetected callback to get notifications when QR code is detected
     */
    public QrCodeAnalyzer(@NotNull AnalyzerCallback onQrCodesDetected) {
        this.onQrCodesDetected = onQrCodesDetected;
    }

    @Override
    public void analyze(@NonNull ImageProxy image, int rotationDegrees) {
        // Get instance of FirebaseVisionBarcodeDetector
        FirebaseVisionBarcodeDetectorOptions options = new FirebaseVisionBarcodeDetectorOptions.Builder()
                .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
                .build();
        FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);

        // Coverts rotation
        int rotation = 0;
        try {
            rotation = this.rotationDegreesToFirebaseRotation(rotationDegrees);
        } catch (Exception e) {
            if(onQrCodesDetected != null){
                onQrCodesDetected.onException(e);
                return;
            }
        }

        // Create FirebaseVisionImage from frame
        FirebaseVisionImage visionImage = FirebaseVisionImage.fromMediaImage(image.getImage(), rotation);


        // Pass visionImage to detector and notify onQrCodesDetected with list of detected QR code
        detector.detectInImage(visionImage).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
            @Override
            public void onSuccess(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
                if(onQrCodesDetected != null) {
                    onQrCodesDetected.onSuccess(firebaseVisionBarcodes);
                    return;
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if(onQrCodesDetected != null) {
                    onQrCodesDetected.onException(e);
                    return;
                }
            }
        });
    }


    /**
     * Converts ImageAnalysis.Analyzer’s rotation degrees to firebase’s rotation
     * @param rotationDegrees
     * @return firebase’s rotation value
     * @throws IllegalArgumentException if rotation degrees is not supported
     */
    private final int rotationDegreesToFirebaseRotation(int rotationDegrees) throws IllegalArgumentException {
        int value;
        switch(rotationDegrees) {
            case 0:
                value = FirebaseVisionImageMetadata.ROTATION_0;
                break;
            case 90:
                value = FirebaseVisionImageMetadata.ROTATION_90;
                break;
            case 180:
                value = FirebaseVisionImageMetadata.ROTATION_180;
                break;
            case 270:
                value = FirebaseVisionImageMetadata.ROTATION_270;
                break;
            default:
                throw new IllegalArgumentException("Not supported");
        }
        return value;
    }

    /**
     * Callback interface to get notifications when QR code is detected
     */
    public interface AnalyzerCallback {

        void onSuccess(List<FirebaseVisionBarcode> result);

        void onException(Exception e);

    }

}
