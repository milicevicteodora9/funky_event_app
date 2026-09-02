package com.example.funkyeventapp.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;

import com.example.funkyeventapp.models.Receipt;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** On-device OCR and conservative parsing for receipt images and the first page of PDFs. */
public final class ReceiptOcrProcessor {
    public interface Callback {
        void onSuccess(@NonNull Receipt receipt);
        void onError(@NonNull Exception error);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private ReceiptOcrProcessor() { }

    public static void process(@NonNull Context context, @NonNull Uri uri, boolean pdf,
                               @NonNull Callback callback) {
        Context appContext = context.getApplicationContext();
        IO.execute(() -> {
            try {
                InputImage image = createInputImage(appContext, uri, pdf);
                mainHandler().post(() -> recognize(image, callback));
            } catch (Exception error) {
                mainHandler().post(() -> callback.onError(error));
            }
        });
    }

    static InputImage createInputImage(Context context, Uri uri, boolean pdf) throws IOException {
        return InputImage.fromBitmap(createBitmap(context, uri, pdf), 0);
    }

    static Bitmap createBitmap(Context context, Uri uri, boolean pdf) throws IOException {
        return pdf ? pdfFirstPageBitmap(context, uri) : imageBitmap(context, uri);
    }

    static void recognize(InputImage image, Callback callback) {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(result -> callback.onSuccess(ReceiptTextParser.parse(result.getText())))
                .addOnFailureListener(callback::onError)
                .addOnCompleteListener(task -> recognizer.close());
    }

    private static Bitmap imageBitmap(Context context, Uri uri) throws IOException {
        ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), uri);
        return ImageDecoder.decodeBitmap(source, (decoder, info, ignored) -> {
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            int width = info.getSize().getWidth();
            int height = info.getSize().getHeight();
            int largest = Math.max(width, height);
            if (largest > 4096) {
                float scale = 4096f / largest;
                decoder.setTargetSize(Math.max(1, Math.round(width * scale)),
                        Math.max(1, Math.round(height * scale)));
            }
        });
    }

    private static Bitmap pdfFirstPageBitmap(Context context, Uri uri) throws IOException {
        try (ParcelFileDescriptor descriptor = context.getContentResolver()
                .openFileDescriptor(uri, "r")) {
            if (descriptor == null) throw new IOException("Cannot open PDF");
            try (PdfRenderer renderer = new PdfRenderer(descriptor)) {
                if (renderer.getPageCount() == 0) throw new IOException("PDF has no pages");
                try (PdfRenderer.Page page = renderer.openPage(0)) {
                    float scale = Math.min(3f, 2200f / Math.max(page.getWidth(), page.getHeight()));
                    int width = Math.max(1, Math.round(page.getWidth() * scale));
                    int height = Math.max(1, Math.round(page.getHeight() * scale));
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    bitmap.eraseColor(android.graphics.Color.WHITE);
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    return bitmap;
                }
            }
        }
    }

    private static Handler mainHandler() {
        return new Handler(Looper.getMainLooper());
    }
}
