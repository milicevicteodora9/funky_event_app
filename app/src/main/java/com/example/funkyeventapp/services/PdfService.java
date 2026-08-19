package com.example.funkyeventapp.services;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;

import androidx.core.content.FileProvider;

import com.example.funkyeventapp.R;
import com.example.funkyeventapp.models.Client;
import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.repositories.MockDataRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PdfService {
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int PAGE_MARGIN = 56;

    private final MockDataRepository repository;
    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    public PdfService() {
        repository = MockDataRepository.getInstance();
    }

    public File generateQuote(Context context, Event event) throws IOException {
        File documentsDirectory = resolveDocumentsDirectory(context);
        if (!documentsDirectory.exists() && !documentsDirectory.mkdirs()) {
            throw new IOException("Unable to create app Documents directory");
        }

        File outputFile = new File(documentsDirectory,
                "Quote_" + sanitizeFileName(event.getName()) + ".pdf");
        PdfDocument document = new PdfDocument();
        try {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH, PAGE_HEIGHT, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            drawPlaceholderQuote(page.getCanvas(), event);
            document.finishPage(page);

            try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                document.writeTo(outputStream);
            }
        } finally {
            document.close();
        }
        return outputFile;
    }

    public void shareQuote(Context context, File pdfFile) {
        Uri contentUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                pdfFile);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.setClipData(ClipData.newRawUri("PDF Quote", contentUri));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(
                shareIntent,
                context.getString(R.string.share_pdf_quote)));
    }

    private void drawPlaceholderQuote(Canvas canvas, Event event) {
        Paint brandPaint = paint(22, true, Color.rgb(32, 33, 42));
        Paint titlePaint = paint(30, true, Color.rgb(71, 203, 166));
        Paint labelPaint = paint(12, true, Color.rgb(115, 121, 135));
        Paint valuePaint = paint(16, false, Color.rgb(32, 33, 42));
        Paint footerPaint = paint(11, false, Color.rgb(115, 121, 135));

        Client client = repository.getClientById(event.getClientId());
        String clientName = client == null ? "—" : client.getName();
        String date = event.getStartDate() == null
                ? "—"
                : event.getStartDate().format(dateFormatter);

        int y = 80;
        canvas.drawText("Funky Business", PAGE_MARGIN, y, brandPaint);
        y += 70;
        canvas.drawText("QUOTE", PAGE_MARGIN, y, titlePaint);
        y += 68;
        canvas.drawText("Event:", PAGE_MARGIN, y, labelPaint);
        y += 25;
        canvas.drawText(event.getName(), PAGE_MARGIN, y, valuePaint);
        y += 58;
        canvas.drawText("Client:", PAGE_MARGIN, y, labelPaint);
        y += 25;
        canvas.drawText(clientName, PAGE_MARGIN, y, valuePaint);
        y += 58;
        canvas.drawText("Date:", PAGE_MARGIN, y, labelPaint);
        y += 25;
        canvas.drawText(date, PAGE_MARGIN, y, valuePaint);

        canvas.drawText("Generated from Funky Event App",
                PAGE_MARGIN,
                PAGE_HEIGHT - PAGE_MARGIN,
                footerPaint);
    }

    private Paint paint(float textSize, boolean bold, int color) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
        paint.setColor(color);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF,
                bold ? Typeface.BOLD : Typeface.NORMAL));
        return paint;
    }

    private File resolveDocumentsDirectory(Context context) {
        File externalDocuments = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (externalDocuments != null) {
            return externalDocuments;
        }
        return new File(context.getFilesDir(), Environment.DIRECTORY_DOCUMENTS);
    }

    private String sanitizeFileName(String eventName) {
        String safeName = eventName == null ? "Event" : eventName.trim();
        safeName = safeName.replaceAll("[^A-Za-z0-9_-]+", "_");
        safeName = safeName.replaceAll("_+", "_");
        safeName = safeName.replaceAll("^_+|_+$", "");
        return safeName.isEmpty() ? "Event" : safeName;
    }
}
