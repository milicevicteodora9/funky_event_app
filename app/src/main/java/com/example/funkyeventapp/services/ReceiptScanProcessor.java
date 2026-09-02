package com.example.funkyeventapp.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.funkyeventapp.models.Receipt;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tries a trusted Serbian fiscal QR first, then falls back to existing on-device OCR. */
public final class ReceiptScanProcessor {
    public enum Source { SUF_QR, OCR_FALLBACK }
    public enum FallbackReason {
        QR_NOT_FOUND, QR_DETECTED_NOT_READABLE, NON_SUF_QR, QR_SCAN_FAILED, SUF_LOOKUP_FAILED
    }

    public interface Callback {
        void onSuccess(@NonNull Result result);
        void onError(@NonNull Exception error);
    }

    public static final class Result {
        private final Receipt receipt;
        private final Source source;
        private final FallbackReason fallbackReason;

        private Result(Receipt receipt, Source source, FallbackReason fallbackReason) {
            this.receipt = receipt;
            this.source = source;
            this.fallbackReason = fallbackReason;
        }

        public Receipt getReceipt() { return receipt; }
        public Source getSource() { return source; }
        @Nullable public FallbackReason getFallbackReason() { return fallbackReason; }
    }

    private static final String TAG = "ReceiptScanProcessor";
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private ReceiptScanProcessor() { }

    public static void process(@NonNull Context context, @NonNull Uri uri, boolean pdf,
                               @NonNull Callback callback) {
        Context appContext = context.getApplicationContext();
        IO.execute(() -> {
            try {
                Bitmap bitmap = ReceiptOcrProcessor.createBitmap(appContext, uri, pdf);
                InputImage image = InputImage.fromBitmap(bitmap, 0);
                mainHandler().post(() -> scanQr(bitmap, image, image, false, callback));
            } catch (Exception error) {
                mainHandler().post(() -> callback.onError(error));
            }
        });
    }

    private static void scanQr(Bitmap bitmap, InputImage image, InputImage originalImage,
                               boolean croppedRetry, Callback callback) {
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .enableAllPotentialBarcodes()
                .build();
        BarcodeScanner scanner = BarcodeScanning.getClient(options);
        scanner.process(image)
                .addOnSuccessListener(codes -> handleQrResult(
                        bitmap, originalImage, findSufQr(codes), croppedRetry, callback))
                .addOnFailureListener(error -> {
                    Log.w(TAG, "QR detection failed; using OCR fallback", error);
                    fallbackToOcr(originalImage, FallbackReason.QR_SCAN_FAILED, callback);
                })
                .addOnCompleteListener(task -> scanner.close());
    }

    private static void handleQrResult(Bitmap bitmap, InputImage originalImage, QrMatch match,
                                       boolean croppedRetry, Callback callback) {
        if (match.url == null) {
            if (!croppedRetry && match.potentialBounds != null) {
                Bitmap cropped = cropQr(bitmap, match.potentialBounds);
                scanQr(cropped, InputImage.fromBitmap(cropped, 0), originalImage,
                        true, callback);
            } else {
                FallbackReason reason = croppedRetry && match.fallbackReason == FallbackReason.QR_NOT_FOUND
                        ? FallbackReason.QR_DETECTED_NOT_READABLE : match.fallbackReason;
                fallbackToOcr(originalImage, reason, callback);
            }
            return;
        }
        IO.execute(() -> {
            try {
                Receipt receipt = SufReceiptClient.fetch(match.url);
                callbackOnMain(() -> callback.onSuccess(
                        new Result(receipt, Source.SUF_QR, null)));
            } catch (Exception error) {
                Log.w(TAG, "SUF lookup failed; using OCR fallback", error);
                callbackOnMain(() -> fallbackToOcr(
                        originalImage, FallbackReason.SUF_LOOKUP_FAILED, callback));
            }
        });
    }

    private static QrMatch findSufQr(List<Barcode> codes) {
        boolean qrDetected = false;
        Rect potentialBounds = null;
        for (Barcode code : codes) {
            String rawValue = code.getRawValue();
            if (rawValue == null) {
                if (potentialBounds == null && code.getBoundingBox() != null) {
                    potentialBounds = new Rect(code.getBoundingBox());
                }
                continue;
            }
            qrDetected = true;
            try {
                java.net.URI uri = new java.net.URI(rawValue.trim());
                if (SufReceiptClient.isTrustedSufUrl(uri)) {
                    return new QrMatch(rawValue.trim(), null, null);
                }
            } catch (Exception ignored) {
                // Not a valid trusted fiscal verification URL; continue with OCR.
            }
        }
        FallbackReason reason = qrDetected ? FallbackReason.NON_SUF_QR
                : potentialBounds == null ? FallbackReason.QR_NOT_FOUND
                : FallbackReason.QR_DETECTED_NOT_READABLE;
        return new QrMatch(null, reason, potentialBounds);
    }

    private static Bitmap cropQr(Bitmap bitmap, Rect bounds) {
        int paddingX = Math.max(24, bounds.width() / 3);
        int paddingY = Math.max(24, bounds.height() / 3);
        int left = Math.max(0, bounds.left - paddingX);
        int top = Math.max(0, bounds.top - paddingY);
        int right = Math.min(bitmap.getWidth(), bounds.right + paddingX);
        int bottom = Math.min(bitmap.getHeight(), bounds.bottom + paddingY);
        Bitmap cropped = Bitmap.createBitmap(bitmap, left, top,
                Math.max(1, right - left), Math.max(1, bottom - top));
        int largest = Math.max(cropped.getWidth(), cropped.getHeight());
        if (largest >= 1400) return cropped;
        float scale = Math.min(3f, 1400f / largest);
        return Bitmap.createScaledBitmap(cropped,
                Math.max(1, Math.round(cropped.getWidth() * scale)),
                Math.max(1, Math.round(cropped.getHeight() * scale)), true);
    }

    private static void fallbackToOcr(InputImage image, FallbackReason reason, Callback callback) {
        ReceiptOcrProcessor.recognize(image, new ReceiptOcrProcessor.Callback() {
            @Override public void onSuccess(@NonNull Receipt receipt) {
                callback.onSuccess(new Result(receipt, Source.OCR_FALLBACK, reason));
            }

            @Override public void onError(@NonNull Exception error) {
                callback.onError(error);
            }
        });
    }

    private static void callbackOnMain(Runnable callback) {
        mainHandler().post(callback);
    }

    private static Handler mainHandler() {
        return new Handler(Looper.getMainLooper());
    }

    private static final class QrMatch {
        private final String url;
        private final FallbackReason fallbackReason;
        private final Rect potentialBounds;

        private QrMatch(String url, FallbackReason fallbackReason, Rect potentialBounds) {
            this.url = url;
            this.fallbackReason = fallbackReason;
            this.potentialBounds = potentialBounds;
        }
    }
}
