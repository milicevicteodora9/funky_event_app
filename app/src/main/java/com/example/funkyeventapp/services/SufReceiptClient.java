package com.example.funkyeventapp.services;

import androidx.annotation.NonNull;

import com.example.funkyeventapp.models.Currency;
import com.example.funkyeventapp.models.Receipt;
import com.example.funkyeventapp.models.ReceiptProcessingStatus;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Reads the documented JSON representation of a verified Serbian fiscal receipt. */
final class SufReceiptClient {
    private static final String SUF_HOST = "suf.purs.gov.rs";
    private static final int TIMEOUT_MILLIS = 10_000;
    private static final int MAX_RESPONSE_BYTES = 1_048_576;
    private static final ZoneId SERBIA_ZONE = ZoneId.of("Europe/Belgrade");

    private SufReceiptClient() { }

    @NonNull
    static Receipt fetch(@NonNull String verificationUrl) throws IOException {
        URI uri;
        try {
            uri = new URI(verificationUrl.trim());
        } catch (URISyntaxException error) {
            throw new IOException("Invalid SUF verification URL", error);
        }
        if (!isTrustedSufUrl(uri)) throw new IOException("Untrusted fiscal receipt URL");

        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        connection.setInstanceFollowRedirects(false);
        try {
            int status = connection.getResponseCode();
            String contentType = connection.getContentType();
            if (status != HttpURLConnection.HTTP_OK
                    || contentType == null
                    || !contentType.toLowerCase(Locale.ROOT).contains("application/json")) {
                throw new IOException("SUF did not return a JSON receipt");
            }
            String response;
            try (BufferedInputStream input = new BufferedInputStream(connection.getInputStream())) {
                response = readLimited(input);
            }
            return parse(response);
        } finally {
            connection.disconnect();
        }
    }

    static boolean isTrustedSufUrl(@NonNull URI uri) {
        return "https".equalsIgnoreCase(uri.getScheme())
                && SUF_HOST.equalsIgnoreCase(uri.getHost())
                && uri.getUserInfo() == null
                && (uri.getPort() == -1 || uri.getPort() == 443);
    }

    @NonNull
    private static Receipt parse(String json) throws IOException {
        try {
            JSONObject root = new JSONObject(json);
            if (!root.optBoolean("isValid", false)) {
                throw new IOException("SUF receipt is not valid");
            }
            JSONObject request = root.optJSONObject("invoiceRequest");
            JSONObject result = root.optJSONObject("invoiceResult");
            if (request == null || result == null) throw new IOException("Incomplete SUF receipt");

            BigDecimal total = decimal(result.opt("totalAmount"));
            if (total == null || total.signum() <= 0) throw new IOException("Missing SUF total");

            Receipt receipt = new Receipt();
            receipt.setSeller(firstNonBlank(request.optString("businessName", null),
                    request.optString("locationName", null)));
            receipt.setSellerTaxId(blankToNull(request.optString("taxId", null)));
            receipt.setReceiptNumber(blankToNull(result.optString("invoiceNumber", null)));
            receipt.setIssueDate(parseDate(result.optString("sdcTime", null)));
            receipt.setTotalAmount(total);
            receipt.setCurrency(Currency.RSD);
            receipt.setRecognizedText(root.optString("journal", ""));
            receipt.setProcessingStatus(ReceiptProcessingStatus.PROCESSED);
            return receipt;
        } catch (JSONException error) {
            throw new IOException("Invalid SUF JSON", error);
        }
    }

    private static String readLimited(BufferedInputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > MAX_RESPONSE_BYTES) throw new IOException("SUF response is too large");
            output.write(buffer, 0, read);
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }

    private static BigDecimal decimal(Object value) {
        if (value == null || value == JSONObject.NULL) return null;
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return Instant.parse(value).atZone(SERBIA_ZONE).toLocalDate();
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(value).atZoneSameInstant(SERBIA_ZONE).toLocalDate();
            } catch (DateTimeParseException ignoredAgain) {
                try {
                    return LocalDateTime.parse(value).toLocalDate();
                } catch (DateTimeParseException ignoredFinally) {
                    return null;
                }
            }
        }
    }

    private static String firstNonBlank(String first, String second) {
        String value = blankToNull(first);
        return value == null ? blankToNull(second) : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
